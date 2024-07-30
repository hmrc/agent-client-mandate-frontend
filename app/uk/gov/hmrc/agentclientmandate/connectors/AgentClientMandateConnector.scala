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
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentClientMandateConnector @Inject()(val servicesConfig: ServicesConfig,
                                            val http: HttpClientV2) extends Logging {
  def serviceUrl: String = servicesConfig.baseUrl("agent-client-mandate")

  val mandateUri = "mandate"
  val activateUri = "activate"
  val rejectClientUri = "rejectClient"
  val removeUri = "remove"
  val importExistingUri = "importExisting"
  val editMandate = "edit"
  val deleteMandate = "delete"

  def createMandate(mandateDto: CreateMandateDto, agentAuthRetrievals: AgentAuthRetrievals)
                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val agentLink = agentAuthRetrievals.mandateConnectorUri
    val postUrl = s"$serviceUrl$agentLink/$mandateUri"
    val jsonData = Json.toJson(mandateDto)
    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse]
  }

  def fetchMandate(mandateId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val getUrl = s"$serviceUrl/$mandateUri/$mandateId"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def approveMandate(mandate: Mandate, authRetrievals: ClientAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = authRetrievals.mandateConnectorUri
    val jsonData = Json.toJson(mandate)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/approve"
    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse]
  }

  def fetchAllMandates(agentAuthRetrievals: AgentAuthRetrievals, serviceName: String, allClients: Boolean, displayName: Option[String])
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val AgentAuthRetrievals(arn, _, _, _, _) = agentAuthRetrievals
    val authLink = s"/agent"

    val name = displayName.map { x => "displayName=" + x } getOrElse ""
    val getUrl = if (allClients) {
      s"$serviceUrl$authLink/$mandateUri/service/$arn/$serviceName?$name"
    } else {
      val credId = agentAuthRetrievals.providerId
      s"$serviceUrl$authLink/$mandateUri/service/$arn/$serviceName?credId=$credId&$name"
    }
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def rejectClient(mandateId: String, agentCode: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = s"/agent/$agentCode"
    val postUrl = s"$serviceUrl$authLink/$mandateUri/$rejectClientUri/$mandateId"
    http.post(url"$postUrl").withBody(Json.parse("{}")).execute[HttpResponse]
  }

  def fetchAgentDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AgentDetails] = {
    val authLink = s"/agent"
    val getUrl = s"$serviceUrl$authLink/$mandateUri/agentDetails"
    http.get(url"$getUrl").execute[AgentDetails]
  }

  def activateMandate(mandateId: String, agentCode: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = s"/agent/$agentCode"
    val postUrl = s"$serviceUrl$authLink/$mandateUri/$activateUri/$mandateId"
    http.post(url"$postUrl").withBody(Json.parse("{}")).execute[HttpResponse]
  }

  def remove(mandateId: String)
            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val postUrl = s"$serviceUrl/$mandateUri/$removeUri/$mandateId"
    http.post(url"$postUrl").withBody(Json.parse("{}")).execute[HttpResponse]
  }

  def editMandate(mandate: Mandate, agentAuthRetrievals: AgentAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = s"/agent/${agentAuthRetrievals.agentCode}"
    val jsonData = Json.toJson(mandate)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/$editMandate"
    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse]
  }

  def fetchMandateByClient(clientId: String, service: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val getUrl = s"$serviceUrl/org/$mandateUri/$clientId/$service"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def doesAgentHaveMissingEmail(service: String, authRetrievals: AgentAuthRetrievals)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val AgentAuthRetrievals(arn, _, _, _, _) = authRetrievals
    val authLink = s"/agent"
    val getUrl = s"$serviceUrl$authLink/$mandateUri/isAgentMissingEmail/$arn/$service"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def updateAgentMissingEmail(emailAddress: String, agentAuthRetrievals: AgentAuthRetrievals, service: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = agentAuthRetrievals.mandateConnectorUriNoAuthId
    val arn = agentAuthRetrievals.agentRef

    val jsonData = Json.toJson(emailAddress)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/updateAgentEmail/$arn/$service"
    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse]
  }

  def updateClientEmail(emailAddress: String, mandateId: String, clientAuthRetrievals: ClientAuthRetrievals)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = clientAuthRetrievals.mandateConnectorUri
    val jsonData = Json.toJson(emailAddress)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/updateClientEmail/$mandateId"
    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse]
  }

  def updateAgentCredId(mandateAuthRetrievals: AgentAuthRetrievals)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = mandateAuthRetrievals.mandateConnectorUriNoAuthId
    val jsonData = Json.toJson(mandateAuthRetrievals.providerId)
    val postUrl = s"$serviceUrl$authLink/$mandateUri/updateAgentCredId"
    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse]
  }

  def fetchClientsCancelled(agentAuthRetrievals: AgentAuthRetrievals, serviceName: String)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val AgentAuthRetrievals(arn, _, _, _, _) = agentAuthRetrievals
    val authLink = s"/agent"
    val getUrl = s"$serviceUrl$authLink/$mandateUri/clientCancelledNames/$arn/$serviceName"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  // $COVERAGE-OFF$
  def testOnlyCreateMandate(mandate: Mandate)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val jsonData = Json.toJson(mandate)
    val postUrl = s"$serviceUrl/$mandateUri/test-only/create"
    logger.debug("postUrl: " + postUrl)
    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse]
  }

  def testOnlyDeleteMandate(mandateId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val deleteUrl = s"$serviceUrl/$mandateUri/test-only/$deleteMandate/$mandateId"
    logger.debug("deleteUrl: " + deleteUrl)
    http.delete(url"$deleteUrl").execute[HttpResponse]
  }

  // $COVERAGE-ON$

}
