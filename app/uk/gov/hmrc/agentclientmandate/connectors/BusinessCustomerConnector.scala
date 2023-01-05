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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.agentclientmandate.models.{AgentAuthRetrievals, UpdateRegistrationDetailsRequest}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessCustomerConnector @Inject()(
                                         val http: DefaultHttpClient,
                                         val servicesConfig: ServicesConfig
                                         ) extends Logging {

  val serviceUrl: String = servicesConfig.baseUrl("business-customer")
  val baseUri: String = "business-customer"
  val updateRegistrationDetailsURI: String = "update"

  def updateRegistrationDetails(safeId: String, updateRegistrationDetails: UpdateRegistrationDetailsRequest, authRetrievals: AgentAuthRetrievals)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = authRetrievals.mandateConnectorUri
    val postUrl = s"""$serviceUrl$authLink/$baseUri/$updateRegistrationDetailsURI/$safeId"""
    val jsonData = Json.toJson(updateRegistrationDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)(implicitly, implicitly, implicitly, implicitly) map { response =>
      response.status match {
        case OK => response
        case status =>
          logger.warn(s"[BusinessCustomerConnector][updateRegistrationDetails] - STATUS - $status ")
          response
      }
    }
  }
}
