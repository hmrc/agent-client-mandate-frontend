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

package unit.uk.gov.hmrc.agentclientmandate.services

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.{AgentClientMandateConnector, BusinessCustomerConnector}
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, Mandates}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayName, ClientMandateDisplayDetails, EditAgentAddressDetails}
import uk.gov.hmrc.domain.AgentBusinessUtr
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AgentBusinessUtrGenerator}

import scala.concurrent.Future


class AgentClientMandateServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

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

  "AgentClientMandateService" should {

    "not create a mandate" when {

      "no agent email is found in the keystore" in {
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(any(), any()))
          .thenReturn (Future.successful(None))

        val response = TestAgentClientMandateService.createMandate(service, testAgentAuthRetrievals)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Email not found in cache")

      }

      "no client display name is found in the keystore" in {
        val cachedEmail = AgentEmail("aa@aa.com")
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(any(), any()))
          .thenReturn (Future.successful(Some(cachedEmail)))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestAgentClientMandateService.clientDisplayNameFormId))(any(), any()))
          .thenReturn (Future.successful(None))

        val response = TestAgentClientMandateService.createMandate(service, testAgentAuthRetrievals)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Client Display Name not found in cache")

      }

      "there is a problem while creating the mandate" in {
        val cachedEmail = AgentEmail("aa@aa.com")
        val displayName = ClientDisplayName("client display name")

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(any(), any()))
          .thenReturn (Future.successful(Some(cachedEmail)))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestAgentClientMandateService.clientDisplayNameFormId))(any(), any()))
          .thenReturn (Future.successful(Some(displayName)))
        when(mockAgentClientMandateConnector.createMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = TestAgentClientMandateService.createMandate(service, testAgentAuthRetrievals)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Mandate not created")

      }
    }

    "create a mandate" when {

      "agent email is found in the keystore" in {
        val cachedEmail = AgentEmail("aa@aa.com")
        val displayName = ClientDisplayName("client display name")
        val respJson = Json.parse("""{"mandateId": "AS12345678"}""")

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(any(), any()))
          .thenReturn (Future.successful(Some(cachedEmail)))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestAgentClientMandateService.clientDisplayNameFormId))(any(), any()))
          .thenReturn (Future.successful(Some(displayName)))
        when(mockAgentClientMandateConnector.createMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(CREATED, Some(respJson)))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        when(mockDataCacheService.cacheFormData[ClientMandateDisplayDetails](Matchers.eq(TestAgentClientMandateService.agentRefCacheId), any())(any(), any()))
          .thenReturn(Future.successful(ClientMandateDisplayDetails("test name", "AS12345678", agentLastUsedEmail)))

        val response = TestAgentClientMandateService.createMandate(service, testAgentAuthRetrievals)
        await(response) must be("AS12345678")

      }

    }

    "not fetch any mandate" when {

      "incorrect mandate id is passed" in {
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = TestAgentClientMandateService.fetchClientMandate(mandateId, testAgentAuthRetrievals)
        await(response) must be(None)
      }

    }

    "fetch correct mandate" when {

      "correct mandate id is passed" in {
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandate(mandateId, testClientAuthRetrievals)
        await(response) must be(Some(mandateNew))
      }

    }

    "fetch correct mandate client name" when {

      "correct mandate id is passed" in {
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandateClientName(mandateId, testAgentAuthRetrievals)
        await(response) must be(mandateNew)
      }

      "throws an exception when no Mandate found" in {
        val respJson = Json.parse("{}")
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandateClientName(mandateId, testClientAuthRetrievals)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include(s"[AgentClientMandateService][fetchClientMandateClientName] No Mandate returned for id $mandateId")
      }
    }

    "fetch correct mandate agent name" when {

      "correct mandate id is passed" in {
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandateAgentName(mandateId, testClientAuthRetrievals)
        await(response) must be(mandateNew.agentParty.name)
      }
      "throws an exception when no Mandate found" in {
        val respJson = Json.parse("{}")
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandateAgentName(mandateId, testAgentAuthRetrievals)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include(s"[AgentClientMandateService][fetchClientMandateAgentName] No Mandate Agent Name returned with id $mandateId")
      }
    }

    "fetch all mandates" when {

      "filter mandates when status is checked" in {
        val respJson = Json.toJson(Seq(mandateNew, mandateActive, mandatePendingCancellation, mandateApproved))
        when(mockAgentClientMandateConnector.fetchAllMandates(any(), any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchAllClientMandates(testAgentAuthRetrievals, serviceName)
        await(response) must be(Some(Mandates(
          activeMandates = Seq(mandateActive),
          pendingMandates = Seq(mandateNew, mandatePendingCancellation, mandateApproved))))

      }

      "return none when json wont map to case class" in {

        val respJson = Json.obj("Wrong" -> "format")
        when(mockAgentClientMandateConnector.fetchAllMandates(any(), any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchAllClientMandates(testAgentAuthRetrievals, serviceName)
        await(response) must be(None)
      }

      "return none when no mandates found" in {
//        val respJson = Json.obj("Wrong" -> "format")
        when(mockAgentClientMandateConnector.fetchAllMandates(any(), any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(NOT_FOUND, None))

        val response = TestAgentClientMandateService.fetchAllClientMandates(testAgentAuthRetrievals, serviceName)
        await(response) must be(None)
      }

    }

    "send approved mandate to backend and caches the response in keystore" when {
      "client approves it and response status is OK" in {
        val responseJson = Json.toJson(mandateNew)

        when(mockAgentClientMandateConnector.approveMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        when(mockDataCacheService.cacheFormData[Mandate](Matchers.eq(TestAgentClientMandateService.clientApprovedMandateId), any())(any(), any()))
          .thenReturn(Future.successful(mandateNew))

        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.approveMandate(mandateNew, testClientAuthRetrievals)
        await(response) must be(Some(mandateNew))
      }
    }

    "return none" when {
      "backend call failed with status other than OK" in {
        when(mockAgentClientMandateConnector.approveMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
        val response = TestAgentClientMandateService.approveMandate(mandateNew, testClientAuthRetrievals)
        await(response) must be(None)
      }
    }

    "reject client" when {
      "agent rejects client status returned ok" in {
        when(mockAgentClientMandateConnector.rejectClient(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.rejectClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(true)
      }

      "agent rejects client status returned not ok" in {
        when(mockAgentClientMandateConnector.rejectClient(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = TestAgentClientMandateService.rejectClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(false)
      }
    }

    "fetch agent details" in {
      when(mockAgentClientMandateConnector.fetchAgentDetails(any())(any()))
        .thenReturn(Future.successful(agentDetails))
      val response = TestAgentClientMandateService.fetchAgentDetails(testAgentAuthRetrievals)
      await(response) must be(agentDetails)
    }

    "accept a client" when {

      "backend connector call succeeds with status OK" in {
        when(mockAgentClientMandateConnector.activateMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.acceptClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(true)
      }
    }

    "not accept a client" when {

      "backend connector call fails with status not OK" in {
        when(mockAgentClientMandateConnector.activateMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = TestAgentClientMandateService.acceptClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(false)
      }
    }

    "remove client" when {
      "agent removes client status returned ok" in {
        when(mockAgentClientMandateConnector.remove(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.removeClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(true)
      }

      "agent removes client status returned not ok" in {
        when(mockAgentClientMandateConnector.remove(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = TestAgentClientMandateService.removeClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(false)
      }
    }

    "remove agent" when {
      "client removes agent status returned ok" in {
        when(mockAgentClientMandateConnector.remove(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.removeAgent(mandateId, testClientAuthRetrievals)
        await(response) must be(true)
      }

      "client removes agent status returned not ok" in {
        when(mockAgentClientMandateConnector.remove(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = TestAgentClientMandateService.removeAgent(mandateId, testClientAuthRetrievals)
        await(response) must be(false)
      }
    }

    "edit client details" when {
      "edit mandate status returned OK" in {
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.editMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))
        val response = TestAgentClientMandateService.editMandate(mandateNew, testAgentAuthRetrievals)
        await(response) must be(Some(mandateNew))
      }
    }


    "not edit client details" when {
      "edit mandate status does not return OK" in {
        when(mockAgentClientMandateConnector.editMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
        val response = TestAgentClientMandateService.editMandate(mandateNew, testAgentAuthRetrievals)
        await(response) must be(None)
      }
    }

    "fetch mandate for client" when {
      "returns a mandate when client party exists, is active, and for correct service" in {
        val respJson = Json.toJson(mandateActive)
        when(mockAgentClientMandateConnector.fetchMandateByClient(any(), any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))
        val response = TestAgentClientMandateService.fetchClientMandateByClient("clientId", "service", testClientAuthRetrievals)
        await(response) must be(Some(mandateActive))
      }

      "returns None for all other" in {
//        val respJson = Json.toJson(mandateActive)
        when(mockAgentClientMandateConnector.fetchMandateByClient(any(), any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND)))
        val response = TestAgentClientMandateService.fetchClientMandateByClient("clientId", "service", testClientAuthRetrievals)
        await(response) must be(None)
      }
    }

    "check for agent missing email" must {
      "return false if agent is missing email" in {
        when(mockAgentClientMandateConnector.doesAgentHaveMissingEmail(any(), any())(any())) thenReturn Future.successful(HttpResponse(NO_CONTENT))
        when(mockAgentClientMandateConnector.updateAgentCredId(any())(any())) thenReturn Future.successful(HttpResponse(OK))
        val response = TestAgentClientMandateService.doesAgentHaveMissingEmail("ated", testAgentAuthRetrievals)
        await(response) must be(false)
      }

      "return true if agent is missing email" in {
        when(mockAgentClientMandateConnector.doesAgentHaveMissingEmail(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockAgentClientMandateConnector.updateAgentCredId(any())(any())) thenReturn Future.successful(HttpResponse(OK))
        val response = TestAgentClientMandateService.doesAgentHaveMissingEmail("ated", testAgentAuthRetrievals)
        await(response) must be(true)
      }
    }

    "update agent email" must {
      "update an agents missing email address" in {
        TestAgentClientMandateService.updateAgentMissingEmail("test@mail.com", testAgentAuthRetrievals, "ated")
      }
    }

    "update client email" must {
      "update a clients email address" in {
        TestAgentClientMandateService.updateClientEmail("test@mail.com", "mandateId", testClientAuthRetrievals)
      }
    }

    "update the agent business details" when {
      "business details are changed and saved" in {
        val editAgentAddress = EditAgentAddressDetails("Org name", RegisteredAddressDetails("address1", "address2", countryCode = "FR"))
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(isAnIndividual = false, None,
          Some(Organisation("Org name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"),
          EtmpContactDetails(None, None, None, None), isAnAgent = true, isAGroup = true, None))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(cachedData))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService
          .updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals,  editAgentDetails = Some(editAgentAddress))
        await(response) must be(updateRegDetails)
      }

      "ocr details are changed and saved" in {
        val nonUkiOcrChanges = Identification("idnumber", "FR", "issuingInstitution")
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(isAnIndividual = false, None,
          Some(Organisation("Org Name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"),
          EtmpContactDetails(None, None, None, None), isAnAgent = true, isAGroup = true, Some(Identification("idnumber", "FR", "issuingInstitution"))))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(cachedData))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService
          .updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals, editNonUKIdDetails = Some(nonUkiOcrChanges))
        await(response) must be(updateRegDetails)
      }
    }

    "fail to update the agent business details" when {
      "none of the inputs are passed" in {
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(isAnIndividual = false, None,
          Some(Organisation("Org Name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"),
          EtmpContactDetails(None, None, None, None), isAnAgent = true, isAGroup = true, None))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(cachedData))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals)
        await(response) must be(updateRegDetails)
      }

      "no data found in cache" in {
        val nonUkiOcrChanges = Identification("idnumber", "FR", "issuingInstitution")
//        val cachedData = Some(AgentBuilder.buildAgentDetails)
//        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(isAnIndividual = false, None,
//          Some(Organisation("Org Name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"),
//          EtmpContactDetails(None, None, None, None), isAnAgent = true, isAGroup = true, Some(Identification("idnumber", "FR", "issuingInstitution"))))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(None))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService
          .updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals, editNonUKIdDetails = Some(nonUkiOcrChanges))
        await(response) must be(None)
      }

      "ETMP update for business details failed" in {
        val editAgentAddress = EditAgentAddressDetails("Org name", RegisteredAddressDetails("address1", "address2", countryCode = "FR"))
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(cachedData))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any()))
          .thenReturn (Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService
          .updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals, editAgentDetails = Some(editAgentAddress))
        await(response) must be(None)
      }

      "ETMP update for ocr details failed" in {
        val nonUkiOcrChanges = Identification("idnumber", "FR", "issuingInstitution")
//        val cachedData = Some(AgentBuilder.buildAgentDetails)
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(None))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any()))
          .thenReturn (Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService
          .updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals, editNonUKIdDetails = Some(nonUkiOcrChanges), editAgentDetails = None)
        await(response) must be(None)
      }
    }

    "get clients that have cancelled" when {
      "call is unsuccessful" in {
        when(mockAgentClientMandateConnector.fetchClientsCancelled(any(), any())(any())) thenReturn Future.successful(HttpResponse(NOT_FOUND))
        val response = TestAgentClientMandateService.fetchClientsCancelled(testAgentAuthRetrievals, service)
        await(response) must be(None)
      }
      "call is successful" in {
        val respJson = Json.toJson(List("AAA", "BBB"))
        when(mockAgentClientMandateConnector.fetchClientsCancelled(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))
        val response = TestAgentClientMandateService.fetchClientsCancelled(testAgentAuthRetrievals, service)
        await(response) must be(Some(Seq("AAA", "BBB")))
      }
    }
  }

  val registeredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails: AgentDetails = AgentBuilder.buildAgentDetails

  val mandateDto: CreateMandateDto = CreateMandateDto("test@test.com", "ATED", "client display name")
  val time1: DateTime = DateTime.now()

  val mockAgentClientMandateConnector: AgentClientMandateConnector = mock[AgentClientMandateConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockBusinessCustomerConnector: BusinessCustomerConnector = mock[BusinessCustomerConnector]
  val arn: AgentBusinessUtr = new AgentBusinessUtrGenerator().nextAgentBusinessUtr

  val validFormId: String = "some-from-id"
  val service = "ATED"
  val mandateId = "12345678"
  val serviceName = "ATED"
  val agentLastUsedEmail = "a.b@mail.com"

  val userId = s"user-${UUID.randomUUID}"
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mandateNew: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("X0101000000101", "client name", PartyType.Organisation, ContactDetails("agent@agent.com", None))),
    currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Nil,
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  val mandateActive: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("X0101000000101", "client name", PartyType.Organisation, ContactDetails("agent@agent.com", None))),
    currentStatus = MandateStatus(Status.Active, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")),
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  val mandateApproved: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = None,
    currentStatus = MandateStatus(Status.Approved, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")),
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  val mandatePendingCancellation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123458", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = None,
    currentStatus = MandateStatus(Status.PendingCancellation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")),
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")


  object TestAgentClientMandateService extends AgentClientMandateService {
    override val dataCacheService: DataCacheService = mockDataCacheService
    override val agentClientMandateConnector: AgentClientMandateConnector = mockAgentClientMandateConnector
    override val businessCustomerConnector: BusinessCustomerConnector = mockBusinessCustomerConnector
  }

  override def beforeEach: Unit = {
    reset(mockDataCacheService)
    reset(mockAgentClientMandateConnector)
  }

}
