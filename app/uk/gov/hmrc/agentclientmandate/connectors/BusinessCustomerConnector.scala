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


import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.config.WSHttp
import uk.gov.hmrc.agentclientmandate.models.UpdateRegistrationDetailsRequest
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BusinessCustomerConnector extends ServicesConfig with RawResponseReads {

  def serviceUrl: String

  def baseUri: String

  def updateRegistrationDetailsURI: String

  def http: CoreGet with CorePost

  def updateRegistrationDetails(safeId: String, updateRegistrationDetails: UpdateRegistrationDetailsRequest)
                               (implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
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

object BusinessCustomerConnector extends BusinessCustomerConnector {
  // $COVERAGE-OFF$
  val serviceUrl = baseUrl("business-customer")
  val baseUri = "business-customer"
  val updateRegistrationDetailsURI = "update"
  val http = WSHttp
  // $COVERAGE-ON$
}
