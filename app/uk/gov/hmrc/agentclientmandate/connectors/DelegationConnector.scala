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

package uk.gov.hmrc.agentclientmandate.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.agentclientmandate.models.StartDelegationContext
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.StringContextOps
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DelegationConnector @Inject()(val http: HttpClientV2,
                                    val servicesConfig: ServicesConfig) extends Logging {
  protected def serviceUrl: String = servicesConfig.baseUrl("delegation")

  private def delegationUrl(oid: String): String = s"$serviceUrl/oid/$oid"

  def startDelegation(oid: String, delegationContext: StartDelegationContext)
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    http.put(url"${delegationUrl(oid)}").withBody(Json.toJson(delegationContext)).execute[HttpResponse].map { response =>
      response.status match {
        case CREATED =>
          logger.info("[Delegation] Successfully created delegation")
          true
        case unexpectedStatus =>
          logger.info(s"[Delegation] Unexpected response code $unexpectedStatus")
          throw new RuntimeException(s"Unexpected response code '$unexpectedStatus' PUT ${delegationUrl(oid)}")
      }
    } recover {
      case e: Exception =>
        logger.info(s"[Delegation] Unexpected exception ${e.getClass}")
        throw new RuntimeException(s"Unexpected exception ${e.getClass} PUT ${delegationUrl(oid)}")
    }
  }
}
