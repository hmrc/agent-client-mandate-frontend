/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.connectors

import play.api.Mode.Mode
import play.api.http.Status._
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.agentclientmandate.config.WSHttp
import uk.gov.hmrc.agentclientmandate.models.StartDelegationContext
import uk.gov.hmrc.http.{CorePut, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.DelegationServiceException
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object DelegationConnector extends DelegationConnector {
  // $COVERAGE-OFF$
  override val http: CorePut = WSHttp
  override protected def serviceUrl: String = baseUrl("delegation")
  // $COVERAGE-ON$
}

trait DelegationConnector extends ServicesConfig {
  val http: CorePut

  protected def serviceUrl: String

  // $COVERAGE-OFF$
  protected def mode: Mode = Play.current.mode
  protected def runModeConfiguration: Configuration = Play.current.configuration
  // $COVERAGE-ON$

  private def delegationUrl(oid: String): String = s"$serviceUrl/oid/$oid"

  def startDelegation(oid: String, delegationContext: StartDelegationContext)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.PUT[StartDelegationContext, HttpResponse](delegationUrl(oid), delegationContext) map { response =>
      response.status match {
        case CREATED =>
          Logger.info("[Delegation] Successfully created delegation")
          true
        case unexpectedStatus =>
          Logger.info(s"[Delegation] Unexpected response code $unexpectedStatus")
          throw DelegationServiceException(s"Unexpected response code '$unexpectedStatus'", "PUT", delegationUrl(oid))
      }
    } recover {
      case e: Exception =>
        Logger.info(s"[Delegation] Unexpected exception ${e.getClass}")
        throw DelegationServiceException(s"Unexpected exception ${e.getClass}", "PUT", delegationUrl(oid))
    }
  }
}