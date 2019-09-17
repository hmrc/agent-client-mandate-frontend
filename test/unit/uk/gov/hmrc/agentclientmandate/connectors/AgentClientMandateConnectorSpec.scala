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

package unit.uk.gov.hmrc.agentclientmandate.connectors

import org.joda.time.{DateTime, LocalDate}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.models.{CreateMandateDto, _}
import uk.gov.hmrc.domain.AgentBusinessUtr
import uk.gov.hmrc.http._
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AgentBusinessUtrGenerator}

import scala.concurrent.Future

class AgentClientMandateConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  trait MockedVerbs extends CoreGet with CorePost with CoreDelete
  val mockWSHttp: CoreGet with CorePost with CoreDelete = mock[MockedVerbs]

  override def beforeEach(): Unit = {
    reset(mockWSHttp)
  }

  object TestAgentClientMandateConnector extends AgentClientMandateConnector {
    override def serviceUrl: String = baseUrl("agent-client-mandate")

    override val http: CoreGet with CorePost with CoreDelete = mockWSHttp
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

    "create a mandate" in {
      val successResponse = Json.toJson(mandateDto)

      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val response = TestAgentClientMandateConnector.createMandate(mandateDto, testAgentAuthRetrievals)
      await(response).status must be(OK)

    }

    "fetch a valid mandate" in {
      val successResponse = Json.toJson(mandate)

      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val response = TestAgentClientMandateConnector.fetchMandate(mandateId, testAgentAuthRetrievals)
      await(response).status must be(OK)

    }

    "return valid response, when client approves it" in {
      val successResponse = Json.toJson(mandate)

      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val response = await(TestAgentClientMandateConnector.approveMandate(mandate, testClientAuthRetrievals))
      response.status must be(OK)
    }

    "fetch all valid mandates" in {
      val successResponse = Json.toJson(mandateDto)

      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val response = TestAgentClientMandateConnector.fetchAllMandates(testAgentAuthRetrievals, serviceName, allClients = true, None)
      await(response).status must be(OK)
    }

    "fetch all valid mandates for only users clients" in {
      val successResponse = Json.toJson(mandateDto)

      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val response = TestAgentClientMandateConnector.fetchAllMandates(testAgentAuthRetrievals, serviceName, allClients = false, None)
      await(response).status must be(OK)
    }

    "reject a client" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val response = await(TestAgentClientMandateConnector.rejectClient(mandateId, testAgentAuthRetrievals.agentCode))
      response.status must be(OK)
    }

    "get agent details" in {
      when(mockWSHttp.GET[AgentDetails]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(agentDetails))

      val response: AgentDetails = await(TestAgentClientMandateConnector.fetchAgentDetails(testAgentAuthRetrievals.agentCode))
      response.agentName must be("Org Name")
    }

    "get agent details for an individual" in {
      when(mockWSHttp.GET[AgentDetails]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(agentDetails.copy(
          isAnIndividual = true,
          individual = Some(Individual("name", Some("middle"), "last", LocalDate.now()))
      )))

      val response: AgentDetails = await(TestAgentClientMandateConnector.fetchAgentDetails(testAgentAuthRetrievals.agentCode))
      response.agentName must be("name last")
    }

    "get agent details for an individual with no data" in {
      when(mockWSHttp.GET[AgentDetails]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(agentDetails.copy(
          isAnIndividual = true
      )))

      val response: AgentDetails = await(TestAgentClientMandateConnector.fetchAgentDetails(testAgentAuthRetrievals.agentCode))
      response.agentName must be(" ")
    }

    "activate a client" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val response = await(TestAgentClientMandateConnector.activateMandate(mandateId, testAgentAuthRetrievals.agentCode))
      response.status must be(OK)
    }

    "remove an agent" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val response = await(TestAgentClientMandateConnector.remove(mandateId, testClientAuthRetrievals))
      response.status must be(OK)
    }

    "remove a client" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val response = await(TestAgentClientMandateConnector.remove(mandateId, testAgentAuthRetrievals))
      response.status must be(OK)
    }

    "edit client mandate" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val response = await(TestAgentClientMandateConnector.editMandate(mandate, testAgentAuthRetrievals))
      response.status must be(OK)
    }

    "fetch a mandate for a client" in {
      val successResponse = Json.toJson(mandate)

      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val response = TestAgentClientMandateConnector.fetchMandateByClient("clientId", "service", testClientAuthRetrievals)
      await(response).status must be(OK)
    }

    "does an agent have a missing email" in {
      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))

      val response = TestAgentClientMandateConnector.doesAgentHaveMissingEmail("ated", testAgentAuthRetrievals)
      await(response).status must be(OK)
    }

    "update an agents email address" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val response = await(TestAgentClientMandateConnector.updateAgentMissingEmail("test@mail.com", testAgentAuthRetrievals, "ated"))
      response.status must be(OK)
    }

    "update a client email address" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val response = await(TestAgentClientMandateConnector.updateClientEmail("test@mail.com", "mandateId", testClientAuthRetrievals))
      response.status must be(OK)
    }

    "update an agent cred id" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))

      val response = await(TestAgentClientMandateConnector.updateAgentCredId(testAgentAuthRetrievals))
      response.status must be(OK)
    }

    "get clients that have cancelled" in {
      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))

      val response = TestAgentClientMandateConnector.fetchClientsCancelled(testAgentAuthRetrievals, "ated")
      await(response).status must be(OK)
    }
  }


}
