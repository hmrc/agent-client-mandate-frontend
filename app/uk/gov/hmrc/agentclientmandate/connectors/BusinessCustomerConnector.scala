/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.models.{AgentAuthRetrievals, UpdateRegistrationDetailsRequest}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BusinessCustomerConnector @Inject()(
                                         val http: DefaultHttpClient,
                                         val servicesConfig: ServicesConfig
                                         ) extends RawResponseReads {

  val serviceUrl: String = servicesConfig.baseUrl("business-customer")
  val baseUri: String = "business-customer"
  val updateRegistrationDetailsURI: String = "update"

  def updateRegistrationDetails(safeId: String, updateRegistrationDetails: UpdateRegistrationDetailsRequest, authRetrievals: AgentAuthRetrievals)
                               (implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authRetrievals.mandateConnectorUri
    val postUrl = s"""$serviceUrl$authLink/$baseUri/$updateRegistrationDetailsURI/$safeId"""
    val jsonData = Json.toJson(updateRegistrationDetails)
    http.POST(postUrl, jsonData) map { response =>
      response.status match {
        case OK => response
        case status =>
          Logger.warn(s"[BusinessCustomerConnector][updateRegistrationDetails] - STATUS - $status ")
          response
      }
    }
  }
}
