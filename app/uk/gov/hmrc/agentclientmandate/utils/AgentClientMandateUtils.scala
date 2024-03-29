/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentclientmandate.utils

import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.models.Status.Status
import uk.gov.hmrc.agentclientmandate.models.{AgentDetails, Mandate, Status}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}
import scala.util.Random

object AgentClientMandateUtils {

  private val ZERO = 0
  private val ONE = 1
  private val TWO = 2
  private val THREE = 3
  private val FOUR = 4
  private val FIVE = 5
  private val SIX = 6
  private val SEVEN = 7
  private val EIGHT = 8
  private val NINE = 9
  private val TEN = 10

  val emailRegex =
    """^$|^(?!\.)("([^"\r\\]|\\["\r\\])+"|([-a-zA-Z0-9!#$%&'*+\/=?^_`{|}~]+|(?<!\.)\.)+)(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$"""
  val maximumEmailLength = 241
  val minimumEmailLength = 1

  def getUniqueAckNo: String = {
    val length = 32
    val nanoTime = System.nanoTime()
    val restChars = length - nanoTime.toString.length
    val randomChars = Random.alphanumeric.take(restChars).mkString
    randomChars + nanoTime
  }

  def validateUTR(utr: Option[String]): Boolean = {
    utr match {
      case Some(x) =>
        x.trim.length == TEN && x.trim.forall(_.isDigit) && {
          val actualUtr = x.trim.toList
          val checkDigit = actualUtr.head.asDigit
          val restOfUtr = actualUtr.tail
          val weights = List(SIX, SEVEN, EIGHT, NINE, TEN, FIVE, FOUR, THREE, TWO)
          val weightedUtr = for ((w1, u1) <- weights zip restOfUtr) yield {
            w1 * u1.asDigit
          }
          val total = weightedUtr.sum
          val remainder = total % 11
          isValidUtr(remainder, checkDigit)
        }
      case None => false
    }
  }

  private def isValidUtr(remainder: Int, checkDigit: Int): Boolean = {
    val mapOfRemainders = Map(ZERO -> TWO, ONE -> ONE, TWO -> NINE, THREE -> EIGHT, FOUR -> SEVEN, FIVE -> SIX,
      SIX -> FIVE, SEVEN -> FOUR, EIGHT -> THREE, NINE -> TWO, TEN -> ONE)
    mapOfRemainders.get(remainder).contains(checkDigit)
  }

  def isPendingStatus(status: Status): Boolean = {
    val pendingStates = Seq(Status.PendingCancellation, Status.New, Status.Approved, Status.PendingActivation)
    pendingStates.contains(status)
  }

  def checkStatus(status: Status): String = {
    status match {
      case Status.New => "Await"
      case Status.PendingActivation | Status.PendingCancellation => "Pending"
      case _ => ""
    }
  }

  def isUkAgent(agentDetails: AgentDetails): Boolean = agentDetails.addressDetails.countryCode == "GB"

  def isNonUkClient(mandate: Mandate): Boolean =
    !(mandate.statusHistory.exists(_.status == Status.Active) && mandate.statusHistory.exists(_.status == Status.New))

  def extractUrl(theUrl: RedirectUrl,  appConfig: AppConfig): String = url(theUrl.unsafeValue, appConfig)

  def url(theUrl: String,  appConfig: AppConfig): String = {
    RedirectUrl(theUrl).getEither(OnlyRelative | AbsoluteWithHostnameFromAllowlist(appConfig.allowedHosts: _*)) match {
      case Right(safeRedirectUrl) => safeRedirectUrl.url
      case Left(error) => throw new IllegalArgumentException(error)
    }
  }

  def getSafeLink(theUrl: RedirectUrl, appConfig: AppConfig): Option[String] = {
    try {
      Some(extractUrl(theUrl, appConfig))
    } catch {
      case _: Exception => None
    }
  }
}
