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

package unit.uk.gov.hmrc.agentclientmandate.services

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.{AgentClientMandateConnector, GovernmentGatewayConnector}
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, Mandates}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayDetails, ClientDisplayName}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBusinessUtrGenerator, AuthBuilder}

import scala.concurrent.Future


class AgentClientMandateServiceSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with BeforeAndAfterEach {

  "AgentClientMandateService" should {

    "not create a mandate" when {

      "no agent email is found in the keystore" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)

        val response = TestAgentClientMandateService.createMandate(service)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Email not found in cache")

      }

      "no client display name is found in the keystore" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val cachedEmail = AgentEmail("aa@aa.com", "aa@aa.com")
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cachedEmail))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestAgentClientMandateService.clientDisplayNameFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)

        val response = TestAgentClientMandateService.createMandate(service)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Client Display Name not found in cache")

      }

      "there is a problem while creating the mandate" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val cachedEmail = AgentEmail("aa@aa.com", "aa@aa.com")
        val displayName = ClientDisplayName("client display name")

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cachedEmail))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestAgentClientMandateService.clientDisplayNameFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(displayName))
        when(mockAgentClientMandateConnector.createMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = TestAgentClientMandateService.createMandate(service)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Mandate not created")

      }
    }

    "create a mandate" when {

      "agent email is found in the keystore" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val cachedEmail = AgentEmail("aa@aa.com", "aa@aa.com")
        val displayName = ClientDisplayName("client display name")
        val respJson = Json.parse("""{"mandateId": "AS12345678"}""")

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cachedEmail))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestAgentClientMandateService.clientDisplayNameFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(displayName))
        when(mockAgentClientMandateConnector.createMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(CREATED, Some(respJson)))
        when(mockDataCacheService.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        when(mockDataCacheService.cacheFormData[ClientDisplayDetails](Matchers.eq(TestAgentClientMandateService.agentRefCacheId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(ClientDisplayDetails("test name","AS12345678")))

        val response = TestAgentClientMandateService.createMandate(service)
        await(response) must be("AS12345678")

      }

    }

    "not fetch any mandate" when {

      "incorrect mandate id is passed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.fetchMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = TestAgentClientMandateService.fetchClientMandate(mandateId)
        await(response) must be(None)
      }

    }

    "fetch correct mandate" when {

      "correct mandate id is passed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.fetchMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandate(mandateId)
        await(response) must be(Some(mandateNew))
      }

    }

    "fetch all mandates" when {

      "return no mandates when the list is empty" in {

        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.fetchAllMandates(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = TestAgentClientMandateService.fetchAllClientMandates(arn.utr, serviceName)
        await(response) must be(None)

      }

      "filter mandates when status is checked" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.toJson(Seq(mandateNew, mandateActive, mandatePendingCancellation, mandateApproved))
        when(mockAgentClientMandateConnector.fetchAllMandates(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchAllClientMandates(arn.utr, serviceName)
        await(response) must be(Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingCancellation, mandateApproved))))

      }

      "return none when json wont map to case class" in {

        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.obj("Wrong" -> "format")
        when(mockAgentClientMandateConnector.fetchAllMandates(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchAllClientMandates(arn.utr, serviceName)
        await(response) must be(None)
      }

      "return no mandate list when agent logs in for first time and import process return OK" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.parse("""{}""")
        val identifierForDispList = List(IdentifierForDisplay("type", "X12345678"))
        val clientList = List(RetrieveClientAllocation("friendlyName", identifierForDispList))
        val ggDtoList = List(GGRelationshipDto(serviceName, arn.utr, "credId", "X12345678"))
        when(mockAgentClientMandateConnector.fetchAllMandates(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(NOT_FOUND, None))
        when(mockGovernmentGatewayConnector.retrieveClientList(Matchers.any(), Matchers.any())) thenReturn Future.successful(clientList)
        when(mockAgentClientMandateConnector.importExistingRelationships(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchAllClientMandates(arn.utr, serviceName)
        await(response) must be(None)

      }

      "return no mandate list when agent logs in for first time and import process return any other Status" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.parse("""{}""")
        val identifierForDispList = List(IdentifierForDisplay("type", "X12345678"))
        val clientList = List(RetrieveClientAllocation("friendlyName", identifierForDispList))
        val ggDtoList = List(GGRelationshipDto(serviceName, arn.utr, "credId", "X12345678"))
        when(mockAgentClientMandateConnector.fetchAllMandates(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(NOT_FOUND, None))
        when(mockGovernmentGatewayConnector.retrieveClientList(Matchers.any(), Matchers.any())) thenReturn Future.successful(clientList)
        when(mockAgentClientMandateConnector.importExistingRelationships(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None))

        val response = TestAgentClientMandateService.fetchAllClientMandates(arn.utr, serviceName)
        await(response) must be(None)

      }

    }

    "send approved mandate to backend and caches the response in keystore" when {
      "client approves it and response status is OK" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "client")
        val responseJson = Json.toJson(mandateNew)

        when(mockAgentClientMandateConnector.approveMandate(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        when(mockDataCacheService.cacheFormData[Mandate](Matchers.eq(TestAgentClientMandateService.clientApprovedMandateId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mandateNew))

        when(mockDataCacheService.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.approveMandate(mandateNew)
        await(response) must be(Some(mandateNew))
      }
    }

    "return none" when {
      "backend call failed with status other than OK" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "client")
        when(mockAgentClientMandateConnector.approveMandate(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
        val response = TestAgentClientMandateService.approveMandate(mandateNew)
        await(response) must be(None)
      }
    }

    "reject client" when {
      "agent rejects client status returned ok" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.rejectClient(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.rejectClient(mandateId)
        await(response) must be(true)
      }

      "agent rejects client status returned not ok" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.rejectClient(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = TestAgentClientMandateService.rejectClient(mandateId)
        await(response) must be(false)
      }
    }

    "fetch agent details" in {
      implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
      when(mockAgentClientMandateConnector.fetchAgentDetails()(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(agentDetails))
      val response = TestAgentClientMandateService.fetchAgentDetails()
      await(response) must be(agentDetails)
    }

    "accept a client" when {

      "backend connector call succeeds with status OK" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.activateMandate(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.acceptClient(mandateId)
        await(response) must be(true)
      }
    }

    "not accept a client" when {

      "backend connector call fails with status not OK" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.activateMandate(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = TestAgentClientMandateService.acceptClient(mandateId)
      }
    }

    "remove client" when {
      "agent removes client status returned ok" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.remove(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.removeClient(mandateId)
        await(response) must be(true)
      }

      "agent removes client status returned not ok" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.remove(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = TestAgentClientMandateService.removeClient(mandateId)
        await(response) must be(false)
      }
    }

    "remove agent" when {
      "client removes agent status returned ok" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.remove(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.removeAgent(mandateId)
        await(response) must be(true)
      }

      "client removes agent status returned not ok" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.remove(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = TestAgentClientMandateService.removeAgent(mandateId)
        await(response) must be(false)
      }
    }

    "edit client details" when {
      "edit mandate status returned OK" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.editMandate(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))
        val response = TestAgentClientMandateService.editMandate(mandateNew)
        await(response) must be(Some(mandateNew))
      }
    }


    "not edit client details" when {
      "edit mandate status does not return OK" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.editMandate(Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
        val response = TestAgentClientMandateService.editMandate(mandateNew)
        await(response) must be(None)
      }
    }

  }


  val registeredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails = AgentDetails("Agent Ltd.", registeredAddressDetails)

  val mandateDto: CreateMandateDto = CreateMandateDto("test@test.com", "ATED", "client display name")
  val time1 = DateTime.now()

  val mockAgentClientMandateConnector = mock[AgentClientMandateConnector]
  val mockDataCacheService = mock[DataCacheService]
  val mockGovernmentGatewayConnector = mock[GovernmentGatewayConnector]
  val arn = new AgentBusinessUtrGenerator().nextAgentBusinessUtr

  val validFormId: String = "some-from-id"
  val service = "ATED"
  val mandateId = "12345678"
  val serviceName = "ATED"

  val userId = s"user-${UUID.randomUUID}"
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mandateNew: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = None, currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  val mandateActive: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = None, currentStatus = MandateStatus(Status.Active, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  val mandateApproved: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = None, currentStatus = MandateStatus(Status.Approved, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  val mandatePendingCancellation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123458", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = None, currentStatus = MandateStatus(Status.PendingCancellation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")


  object TestAgentClientMandateService extends AgentClientMandateService {
    override val dataCacheService = mockDataCacheService
    override val agentClientMandateConnector = mockAgentClientMandateConnector
    override val ggConnector = mockGovernmentGatewayConnector
  }

  override def beforeEach = {
    reset(mockDataCacheService)
    reset(mockAgentClientMandateConnector)
    reset(mockGovernmentGatewayConnector)
  }

}
