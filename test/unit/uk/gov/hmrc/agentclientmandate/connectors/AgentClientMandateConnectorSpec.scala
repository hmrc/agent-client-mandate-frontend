/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.models.{CreateMandateDto, _}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost}
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AgentBusinessUtrGenerator}
import unit.uk.gov.hmrc.agentclientmandate.builders.AuthBuilder._

import scala.concurrent.Future

class AgentClientMandateConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  class MockHttp extends WSGet with WSPost with WSDelete {
    override val hooks = NoneRequired
  }

  override def beforeEach(): Unit = {
    reset(mockWSHttp)
  }

  val mockWSHttp = mock[MockHttp]

  object TestAgentClientMandateConnector extends AgentClientMandateConnector {
    override def serviceUrl: String = baseUrl("agent-client-mandate")

    override def http: HttpGet with HttpPost with HttpDelete = mockWSHttp
  }

  val mandateId = "12345678"
  val serviceName = "ATED"
  val arn = new AgentBusinessUtrGenerator().nextAgentBusinessUtr

  val mandateDto: CreateMandateDto = CreateMandateDto("test@test.com", "ATED", "client display name")
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ac: AuthContext = createRegisteredAgentAuthContext("agent", "agentId")

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

  //val registeredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails = AgentBuilder.buildAgentDetails

  "AgentClientMandateConnector" must {

    "create a mandate" in {
      val successResponse = Json.toJson(mandateDto)

      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))

      val response = TestAgentClientMandateConnector.createMandate(mandateDto)
      await(response).status must be(OK)

    }

    "fetch a valid mandate" in {
      val successResponse = Json.toJson(mandate)

      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))

      val response = TestAgentClientMandateConnector.fetchMandate(mandateId)
      await(response).status must be(OK)

    }

    "return valid response, when client approves it" in {
      val successResponse = Json.toJson(mandate)

      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))

      val response = await(TestAgentClientMandateConnector.approveMandate(mandate))
      response.status must be(OK)
    }

    "fetch all valid mandates" in {
      val successResponse = Json.toJson(mandateDto)

      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))

      val response = TestAgentClientMandateConnector.fetchAllMandates(arn.utr, serviceName, true, None)
      await(response).status must be(OK)
    }

    "fetch all valid mandates for only users clients" in {
      val successResponse = Json.toJson(mandateDto)

      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))

      val response = TestAgentClientMandateConnector.fetchAllMandates(arn.utr, serviceName, false, None)
      await(response).status must be(OK)
    }

    "reject a client" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, None)))

      val response = await(TestAgentClientMandateConnector.rejectClient(mandateId))
      response.status must be(OK)
    }

    "get agent details" in {
      when(mockWSHttp.GET[AgentDetails]
        (Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(agentDetails))

      val response = await(TestAgentClientMandateConnector.fetchAgentDetails())
      response.agentName must be("Org Name")
    }

    "activate a client" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, None)))

      val response = await(TestAgentClientMandateConnector.activateMandate(mandateId))
      response.status must be(OK)
    }

    "remove an agent" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, None)))

      val response = await(TestAgentClientMandateConnector.remove(mandateId))
      response.status must be(OK)
    }

    "remove a client" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, None)))

      val response = await(TestAgentClientMandateConnector.remove(mandateId))
      response.status must be(OK)
    }

    "import existing agent-client relationship" in {
      val ggRelationshipDtoList = List(GGRelationshipDto("serviceName", "agentPartyId", "cred-id", "clinetSubscriptionId"))

      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, None)))

      val response = await(TestAgentClientMandateConnector.importExistingRelationships(ggRelationshipDtoList))
      response.status must be(OK)
    }

    "edit client mandate" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, None)))

      val response = await(TestAgentClientMandateConnector.editMandate(mandate))
      response.status must be(OK)
    }

    "fetch a mandate for a client" in {
      val successResponse = Json.toJson(mandate)

      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(successResponse))))

      val response = TestAgentClientMandateConnector.fetchMandateByClient("clientId", "service")
      await(response).status must be(OK)
    }

    "does an agent have a missing email" in {
      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200)))

      val response = TestAgentClientMandateConnector.doesAgentHaveMissingEmail("ated", "arn")
      await(response).status must be(OK)
    }

    "update an agents email address" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, None)))

      val response = await(TestAgentClientMandateConnector.updateAgentMissingEmail("test@mail.com", "arn", "ated"))
      response.status must be(OK)
    }

    "update a client email address" in {
      when(mockWSHttp.POST[JsValue, HttpResponse]
        (Matchers.any(), Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, None)))

      val response = await(TestAgentClientMandateConnector.updateClientEmail("test@mail.com", "mandateId"))
      response.status must be(OK)
    }
  }


}
