/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.agentclientmandate.config.WSHttp
import uk.gov.hmrc.agentclientmandate.models.{AgentDetails, CreateMandateDto, Mandate}
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._

import scala.concurrent.Future

trait AgentClientMandateConnector extends ServicesConfig with RawResponseReads {

  def serviceUrl: String

  val agentMandateUrl = "agent"
  val mandateUri = "mandate"
  val activateUri = "activate"
  val rejectClientUri = "rejectClient"

  def http: HttpGet with HttpPost with HttpDelete

  def createMandate(mandateDto: CreateMandateDto)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val agentLink = ac.principal.accounts.agent.map(_.link).getOrElse("")
    val postUrl = s"$serviceUrl$agentLink/$mandateUri"
    val jsonData = Json.toJson(mandateDto)
    Logger.info(s"[AgentClientMandateConnector][createMandate] - POST - $postUrl and JSON Data - $jsonData")
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def fetchMandate(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"$serviceUrl$authLink/$mandateUri/$mandateId"
    Logger.info(s"[AgentClientMandateConnector][fetchMandate] - GET - $getUrl")
    http.GET[HttpResponse](getUrl)
  }

  def approveMandate(mandate: Mandate)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val jsonData = Json.toJson(mandate)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/approve"
    Logger.info(s"[AgentClientMandateConnector][approveMandate] - POST - $postUrl and JSON Data - $jsonData")
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def fetchAllMandates(arn: String, serviceName: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"$serviceUrl$authLink/$mandateUri/service/$arn/$serviceName"
    Logger.info(s"[AgentClientMandateConnector][fetchAllMandates] - GET - $getUrl")
    http.GET[HttpResponse](getUrl)
  }

  def rejectClient(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val postUrl = s"$serviceUrl$authLink/$mandateUri/$rejectClientUri/$mandateId"
    Logger.info(s"[AgentClientMandateConnector][rejectClient] - POST - $postUrl")
    http.POST[JsValue, HttpResponse](postUrl, Json.parse("{}"))
  }

  def fetchAgentDetails()(implicit hc: HeaderCarrier, ac: AuthContext): Future[AgentDetails] = {
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"$serviceUrl$authLink/$mandateUri/agentDetails"
    Logger.info(s"[AgentClientMandateConnector][fetchAgentDetails] - GET - $getUrl")
    http.GET[AgentDetails](getUrl)
  }

  def activateMandate(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val postUrl = s"$serviceUrl$authLink/$mandateUri/$activateUri/$mandateId"
    Logger.info(s"[AgentClientMandateConnector][activateMandate] - POST - $postUrl")
    http.POST[JsValue, HttpResponse](postUrl, Json.parse("{}"))
  }

  def removeAgent(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val postUrl = s"$serviceUrl$authLink/$mandateUri/removeAgent/$mandateId"
    Logger.info(s"[AgentClientMandateConnector][removeAgent] - POST - $postUrl")
    http.POST[JsValue, HttpResponse](postUrl, Json.parse("{}"))
  }

  def removeClient(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val postUrl = s"$serviceUrl$authLink/$mandateUri/removeClient/$mandateId"
    Logger.info(s"[AgentClientMandateConnector][removeClient] - POST - $postUrl")
    http.POST[JsValue, HttpResponse](postUrl, Json.parse("{}"))
  }

}

object AgentClientMandateConnector extends AgentClientMandateConnector {
  // $COVERAGE-OFF$
  val serviceUrl = baseUrl("agent-client-mandate")
  val http = WSHttp
  // $COVERAGE-ON$
}
