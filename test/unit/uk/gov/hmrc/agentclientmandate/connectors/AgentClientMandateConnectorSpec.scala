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

package unit.uk.gov.hmrc.agentclientmandate.connectors

import org.joda.time.{DateTime, LocalDate}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.models.{CreateMandateDto, _}
import uk.gov.hmrc.domain.AgentBusinessUtr
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AgentBusinessUtrGenerator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentClientMandateConnectorSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach {

  val mockDefaultHttpClient: DefaultHttpClient = mock[DefaultHttpClient]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  override def beforeEach(): Unit = {
    reset(mockServicesConfig)
  }

  class Setup {
    val agentClientMandateConnector = new AgentClientMandateConnector(
      mockServicesConfig,
      mockDefaultHttpClient
    )
  }

  val mandateId = "12345678"
  val serviceName = "ATED"
  val arn: AgentBusinessUtr = new AgentBusinessUtrGenerator().nextAgentBusinessUtr

  val mandateDto: CreateMandateDto = CreateMandateDto("test@test.com", "ATED", "client display name")
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testAgentAuthRetrievals = AgentAuthRetrievals(
    "agentRef",
    "agentCode",
    Some("agentFriendlyName"),
    "providerId",
    "internalId"
  )

  val testClientAuthRetrievals = ClientAuthRetrievals(
    "hashedCredId"
  )

  val mandate: Mandate =
    Mandate(
      id = "ABC123",
      createdBy = User("cerdId", "Joe Bloggs"),
      agentParty = Party("ated-ref-no", "name", `type` = PartyType.Organisation, contactDetails = ContactDetails("aa@aa.com", None)),
      clientParty = Some(Party("client-id", "client name", `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None))),
      currentStatus = MandateStatus(status = Status.New, DateTime.now(), updatedBy = ""),
      statusHistory = Nil,
      subscription = Subscription(referenceNumber = None, service = Service(id = "ated-ref-no", name = "")),
      clientDisplayName = "client display name"
    )

  val agentDetails: AgentDetails = AgentBuilder.buildAgentDetails

  "AgentClientMandateConnector" must {

    "create a mandate" in new Setup {
      val successResponse = Json.toJson(mandateDto)

      when(mockDefaultHttpClient.POST[JsValue, HttpResponse]
        (any(), any(), any())
        (any(), any(),any(),any()))
        .thenReturn(Future.successful(HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])))

      val response = agentClientMandateConnector.createMandate(mandateDto, testAgentAuthRetrievals)
      await(response).status must be(OK)

    }

    "fetch a valid mandate" in new Setup {
      val successResponse = Json.toJson(mandate)

      when(mockDefaultHttpClient.GET[HttpResponse]
        (any(), any(), any())
        (any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])))

      val response = agentClientMandateConnector.fetchMandate(mandateId)
      await(response).status must be(OK)

    }

    "return valid response, when client approves it" in new Setup {
      val successResponse = Json.toJson(mandate)

      when(mockDefaultHttpClient.POST[JsValue, HttpResponse]
        (any(), any(), any())
        (any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])))

      val response = await(agentClientMandateConnector.approveMandate(mandate, testClientAuthRetrievals))
      response.status must be(OK)
    }

    "fetch all valid mandates" in new Setup {
      val successResponse = Json.toJson(mandateDto)

      when(mockDefaultHttpClient.GET[HttpResponse]
        (any(), any(), any())
        (any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])))

      val response = agentClientMandateConnector.fetchAllMandates(testAgentAuthRetrievals, serviceName, allClients = true, None)
      await(response).status must be(OK)
    }

    "fetch all valid mandates for only users clients" in new Setup {
      val successResponse = Json.toJson(mandateDto)

      when(mockDefaultHttpClient.GET[HttpResponse]
        (any(), any(), any())
        (any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])))

      val response = agentClientMandateConnector.fetchAllMandates(testAgentAuthRetrievals, serviceName, allClients = false, None)
      await(response).status must be(OK)
    }

    "reject a client" in new Setup {
      when(mockDefaultHttpClient.POST[JsValue, HttpResponse]
        (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

      val response = await(agentClientMandateConnector.rejectClient(mandateId, testAgentAuthRetrievals.agentCode))
      response.status must be(OK)
    }

    "get agent details" in new Setup {
      when(mockDefaultHttpClient.GET[AgentDetails]
        (any(), any(), any())
        (any(), any(), any())).thenReturn(Future.successful(agentDetails))

      val response: AgentDetails = await(agentClientMandateConnector.fetchAgentDetails())
      response.agentName must be("Org Name")
    }

    "get agent details for an individual" in new Setup {
      when(mockDefaultHttpClient.GET[AgentDetails]
        (any(), any(), any())
        (any(), any(), any())).thenReturn(Future.successful(agentDetails.copy(
          isAnIndividual = true,
          individual = Some(Individual("name", Some("middle"), "last", LocalDate.now()))
      )))

      val response: AgentDetails = await(agentClientMandateConnector.fetchAgentDetails())
      response.agentName must be("name last")
    }

    "get agent details for an individual with no data" in new Setup {
      when(mockDefaultHttpClient.GET[AgentDetails]
        (any(), any(), any())
        (any(), any(), any())).thenReturn(Future.successful(agentDetails.copy(
          isAnIndividual = true
      )))

      val response: AgentDetails = await(agentClientMandateConnector.fetchAgentDetails())
      response.agentName must be(" ")
    }

    "activate a client" in new Setup {
      when(mockDefaultHttpClient.POST[JsValue, HttpResponse]
        (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

      val response = await(agentClientMandateConnector.activateMandate(mandateId, testAgentAuthRetrievals.agentCode))
      response.status must be(OK)
    }

    "remove an agent" in new Setup {
      when(mockDefaultHttpClient.POST[JsValue, HttpResponse]
        (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

      val response = await(agentClientMandateConnector.remove(mandateId))
      response.status must be(OK)
    }

    "remove a client" in new Setup {
      when(mockDefaultHttpClient.POST[JsValue, HttpResponse]
        (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

      val response = await(agentClientMandateConnector.remove(mandateId))
      response.status must be(OK)
    }

    "edit client mandate" in new Setup {
      when(mockDefaultHttpClient.POST[JsValue, HttpResponse]
        (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

      val response = await(agentClientMandateConnector.editMandate(mandate, testAgentAuthRetrievals))
      response.status must be(OK)
    }

    "fetch a mandate for a client" in new Setup {
      val successResponse = Json.toJson(mandate)

      when(mockDefaultHttpClient.GET[HttpResponse]
        (any(), any(), any())
        (any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])))

      val response = agentClientMandateConnector.fetchMandateByClient("clientId", "service")
      await(response).status must be(OK)
    }

    "fetch a mandate for a client by id" in new Setup {
      val successResponse = Json.toJson(mandate)

      when(mockDefaultHttpClient.GET[HttpResponse]
        (any(), any(), any())
        (any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])))

      val response = agentClientMandateConnector.fetchMandateByClient("clientId", "service")
      await(response).status must be(OK)
    }

    "does an agent have a missing email" in new Setup {
      when(mockDefaultHttpClient.GET[HttpResponse]
        (any(), any(), any())
        (any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

      val response = agentClientMandateConnector.doesAgentHaveMissingEmail("ated", testAgentAuthRetrievals)
      await(response).status must be(OK)
    }

    "update an agents email address" in new Setup {
      when(mockDefaultHttpClient.POST[JsValue, HttpResponse]
        (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

      val response = await(agentClientMandateConnector.updateAgentMissingEmail("test@mail.com", testAgentAuthRetrievals, "ated"))
      response.status must be(OK)
    }

    "update a client email address" in new Setup {
      when(mockDefaultHttpClient.POST[JsValue, HttpResponse]
        (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

      val response = await(agentClientMandateConnector.updateClientEmail("test@mail.com", "mandateId", testClientAuthRetrievals))
      response.status must be(OK)
    }

    "update an agent cred id" in new Setup {
      when(mockDefaultHttpClient.POST[JsValue, HttpResponse]
        (any(), any(), any())
        (any(), any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

      val response = await(agentClientMandateConnector.updateAgentCredId(testAgentAuthRetrievals))
      response.status must be(OK)
    }

    "get clients that have cancelled" in new Setup {
      when(mockDefaultHttpClient.GET[HttpResponse]
        (any(), any(), any())
        (any(), any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

      val response = agentClientMandateConnector.fetchClientsCancelled(testAgentAuthRetrievals, "ated")
      await(response).status must be(OK)
    }
  }


}
