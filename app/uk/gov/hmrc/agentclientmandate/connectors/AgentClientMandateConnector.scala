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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.agentclientmandate.config.WSHttp
import uk.gov.hmrc.agentclientmandate.models.{AgentDetails, CreateMandateDto, GGRelationshipDto, Mandate}
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future

trait AgentClientMandateConnector extends ServicesConfig with RawResponseReads {

  def serviceUrl: String

  val mandateUri = "mandate"
  val activateUri = "activate"
  val rejectClientUri = "rejectClient"
  val removeUri = "remove"
  val importExistingUri = "importExisting"
  val editMandate = "edit"
  val deleteMandate = "delete"

  def http: CoreGet with CorePost with CoreDelete

  def createMandate(mandateDto: CreateMandateDto)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val agentLink = ac.principal.accounts.agent.map(_.link).getOrElse("")
    val postUrl = s"$serviceUrl$agentLink/$mandateUri"
    val jsonData = Json.toJson(mandateDto)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def fetchMandate(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"$serviceUrl$authLink/$mandateUri/$mandateId"
    http.GET[HttpResponse](getUrl)
  }

  def approveMandate(mandate: Mandate)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val jsonData = Json.toJson(mandate)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/approve"
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def fetchAllMandates(arn: String, serviceName: String, allClients: Boolean, displayName: Option[String])(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val name = displayName.map {x => "displayName=" + x} getOrElse ""
    val getUrl = if (allClients) {
      s"$serviceUrl$authLink/$mandateUri/service/$arn/$serviceName?$name"
    } else {
      val credId = ac.user.userId
      s"$serviceUrl$authLink/$mandateUri/service/$arn/$serviceName?credId=$credId&$name"
    }
    http.GET[HttpResponse](getUrl)
  }

  def rejectClient(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val postUrl = s"$serviceUrl$authLink/$mandateUri/$rejectClientUri/$mandateId"
    http.POST[JsValue, HttpResponse](postUrl, Json.parse("{}"))
  }

  def fetchAgentDetails()(implicit hc: HeaderCarrier, ac: AuthContext): Future[AgentDetails] = {
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"$serviceUrl$authLink/$mandateUri/agentDetails"
    http.GET[AgentDetails](getUrl)
  }

  def activateMandate(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val postUrl = s"$serviceUrl$authLink/$mandateUri/$activateUri/$mandateId"
    http.POST[JsValue, HttpResponse](postUrl, Json.parse("{}"))
  }

  def remove(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val postUrl = s"$serviceUrl$authLink/$mandateUri/$removeUri/$mandateId"
    http.POST[JsValue, HttpResponse](postUrl, Json.parse("{}"))
  }

  def importExistingRelationships(ggRelationshipDtoList: List[GGRelationshipDto])(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val jsonData = Json.toJson(ggRelationshipDtoList)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/$importExistingUri"
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def editMandate(mandate: Mandate)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val jsonData = Json.toJson(mandate)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/$editMandate"
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def fetchMandateByClient(clientId: String, service: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"$serviceUrl$authLink/$mandateUri/$clientId/$service"
    http.GET[HttpResponse](getUrl)
  }

  def doesAgentHaveMissingEmail(service: String, arn: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"$serviceUrl$authLink/$mandateUri/isAgentMissingEmail/$arn/$service"
    http.GET[HttpResponse](getUrl)
  }

  def updateAgentMissingEmail(emailAddress: String, arn: String, service: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val jsonData = Json.toJson(emailAddress)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/updateAgentEmail/$arn/$service"
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def updateClientEmail(emailAddress: String, mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val jsonData = Json.toJson(emailAddress)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/updateClientEmail/$mandateId"
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def updateAgentCredId(credId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val jsonData = Json.toJson(credId)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/updateAgentCredId"
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def fetchClientsCancelled(arn: String, serviceName: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = {
    val authLink = AuthUtils.getAuthLink
    val getUrl = s"$serviceUrl$authLink/$mandateUri/clientCancelledNames/$arn/$serviceName"
    http.GET[HttpResponse](getUrl)
  }


  // $COVERAGE-OFF$
  def testOnlyCreateMandate(mandate: Mandate)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val jsonData = Json.toJson(mandate)
    val postUrl = s"$serviceUrl/$mandateUri/test-only/create"
    Logger.debug("postUrl: " + postUrl)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def testOnlyDeleteMandate(mandateId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val deleteUrl = s"$serviceUrl/$mandateUri/test-only/$deleteMandate/$mandateId"
    Logger.debug("deleteUrl: " + deleteUrl)
    http.DELETE[HttpResponse](deleteUrl)
  }
  // $COVERAGE-ON$
}

object AgentClientMandateConnector extends AgentClientMandateConnector {
  // $COVERAGE-OFF$
  val serviceUrl = baseUrl("agent-client-mandate")
  val http = WSHttp
  // $COVERAGE-ON$
}
