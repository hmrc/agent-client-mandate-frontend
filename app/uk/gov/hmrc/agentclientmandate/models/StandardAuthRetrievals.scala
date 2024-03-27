/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.models

import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation => Org}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment}


case class StandardAuthRetrievals(enrolments: Set[Enrolment],
                                  affinityGroup: Option[AffinityGroup],
                                  credentials: Option[Credentials],
                                  groupId: Option[String]){
  val saIdentifier = "IR-SA"
  val ctIdentifier = "IR-CT"

  def isAgent: Boolean = affinityGroup contains Agent
  def isOrg: Boolean = (affinityGroup contains Org) || ctUtr.isDefined
  def isSa: Boolean = enrolments.toSeq exists (_.key == saIdentifier)

  def saUtr: Option[String] = getUtr(saIdentifier)
  def ctUtr: Option[String] = getUtr(ctIdentifier)

  private def getUtr(identifier: String): Option[String] = {
    enrolments.find(_.key == identifier).flatMap(_.identifiers.collectFirst{ case i if i.key == "UTR" => i.value })
  }

  def credId: Option[String] = credentials.map(_.providerId)

  def authLink: String = {
    (affinityGroup, enrolments.find(_.key == "IR-SA")) match {
      case (Some(AffinityGroup.Organisation), _) => "org/UNUSED"
      case (Some(AffinityGroup.Agent), _) => "agent/UNUSED"
      case (_, Some(enrolment)) => s"sa/${enrolment.identifiers.find(_.key == "UTR").get.value}"
      case _ => throw new RuntimeException("User does not have the correct authorisation")
    }
  }
}
