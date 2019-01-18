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
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.{AgentClientMandateConnector, BusinessCustomerConnector}
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, Mandates}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientMandateDisplayDetails, ClientDisplayName, EditAgentAddressDetails}
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AgentBusinessUtrGenerator, AuthBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }


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
        val cachedEmail = AgentEmail("aa@aa.com")
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cachedEmail))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestAgentClientMandateService.clientDisplayNameFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)

        val response = TestAgentClientMandateService.createMandate(service)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Client Display Name not found in cache")

      }

      "there is a problem while creating the mandate" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val cachedEmail = AgentEmail("aa@aa.com")
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
        val cachedEmail = AgentEmail("aa@aa.com")
        val displayName = ClientDisplayName("client display name")
        val respJson = Json.parse("""{"mandateId": "AS12345678"}""")

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cachedEmail))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestAgentClientMandateService.clientDisplayNameFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(displayName))
        when(mockAgentClientMandateConnector.createMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(CREATED, Some(respJson)))
        when(mockDataCacheService.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        when(mockDataCacheService.cacheFormData[ClientMandateDisplayDetails](Matchers.eq(TestAgentClientMandateService.agentRefCacheId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(ClientMandateDisplayDetails("test name", "AS12345678", agentLastUsedEmail)))

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

    "fetch correct mandate client name" when {

      "correct mandate id is passed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.fetchMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandateClientName(mandateId)
        await(response) must be(mandateNew)
      }

      "throws an exception when no Mandate found" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.parse("{}")
        when(mockAgentClientMandateConnector.fetchMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandateClientName(mandateId)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include(s"[AgentClientMandateService][fetchClientMandateClientName] No Mandate returned for id $mandateId")
      }
    }

    "fetch correct mandate agent name" when {

      "correct mandate id is passed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.fetchMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandateAgentName(mandateId)
        await(response) must be(mandateNew.agentParty.name)
      }
      "throws an exception when no Mandate found" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.parse("{}")
        when(mockAgentClientMandateConnector.fetchMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandateAgentName(mandateId)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include(s"[AgentClientMandateService][fetchClientMandateAgentName] No Mandate Agent Name returned with id $mandateId")
      }
    }

    "fetch all mandates" when {

      "filter mandates when status is checked" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.toJson(Seq(mandateNew, mandateActive, mandatePendingCancellation, mandateApproved))
        when(mockAgentClientMandateConnector.fetchAllMandates(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchAllClientMandates(arn.utr, serviceName)
        await(response) must be(Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingCancellation, mandateApproved))))

      }

      "return none when json wont map to case class" in {

        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.obj("Wrong" -> "format")
        when(mockAgentClientMandateConnector.fetchAllMandates(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchAllClientMandates(arn.utr, serviceName)
        await(response) must be(None)
      }

      "return none when no mandates found" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.obj("Wrong" -> "format")
        when(mockAgentClientMandateConnector.fetchAllMandates(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(NOT_FOUND, None))

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

    "fetch mandate for client" when {
      "returns a mandate when client party exists, is active, and for correct service" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "client")
        val respJson = Json.toJson(mandateActive)
        when(mockAgentClientMandateConnector.fetchMandateByClient(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))
        val response = TestAgentClientMandateService.fetchClientMandateByClient("clientId", "service")
        await(response) must be(Some(mandateActive))
      }

      "returns None for all other" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "client")
        val respJson = Json.toJson(mandateActive)
        when(mockAgentClientMandateConnector.fetchMandateByClient(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND)))
        val response = TestAgentClientMandateService.fetchClientMandateByClient("clientId", "service")
        await(response) must be(None)
      }
    }

    "check for agent missing email" must {
      "return false if agent is missing email" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.doesAgentHaveMissingEmail(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(NO_CONTENT))
        when(mockAgentClientMandateConnector.updateAgentCredId(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK))
        val response = TestAgentClientMandateService.doesAgentHaveMissingEmail("ated", "arn")
        await(response) must be(false)
      }

      "return true if agent is missing email" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.doesAgentHaveMissingEmail(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockAgentClientMandateConnector.updateAgentCredId(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK))
        val response = TestAgentClientMandateService.doesAgentHaveMissingEmail("ated", "arn")
        await(response) must be(true)
      }
    }

    "update agent email" must {
      "update an agents missing email address" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        TestAgentClientMandateService.updateAgentMissingEmail("test@mail.com", "arn", "ated")
      }
    }

    "update client email" must {
      "update a clients email address" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "client")
        TestAgentClientMandateService.updateClientEmail("test@mail.com", "mandateId")
      }
    }

    "update the agent business details" when {
      "business details are changed and saved" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val editAgentAddress = EditAgentAddressDetails("Org name", RegisteredAddressDetails("address1", "address2", countryCode = "FR"))
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(false, None, Some(Organisation("Org name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"), EtmpContactDetails(None, None, None, None), true, true, None))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(cachedData)
        when(mockBusinessCustomerConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.updateRegisteredDetails(editAgentDetails = Some(editAgentAddress))
        await(response) must be(updateRegDetails)
      }

      "ocr details are changed and saved" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val nonUkiOcrChanges = Identification("idnumber", "FR", "issuingInstitution")
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(false, None, Some(Organisation("Org Name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"), EtmpContactDetails(None, None, None, None), true, true, Some(Identification("idnumber", "FR", "issuingInstitution"))))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(cachedData)
        when(mockBusinessCustomerConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.updateRegisteredDetails(editNonUKIdDetails = Some(nonUkiOcrChanges))
        await(response) must be(updateRegDetails)
      }
    }

    "fail to update the agent business details" when {
      "none of the inputs are passed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(false, None, Some(Organisation("Org Name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"), EtmpContactDetails(None, None, None, None), true, true, None))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(cachedData)
        when(mockBusinessCustomerConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.updateRegisteredDetails()
        await(response) must be(updateRegDetails)
      }

      "no data found in cache" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val nonUkiOcrChanges = Identification("idnumber", "FR", "issuingInstitution")
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(false, None, Some(Organisation("Org Name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"), EtmpContactDetails(None, None, None, None), true, true, Some(Identification("idnumber", "FR", "issuingInstitution"))))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)
        when(mockBusinessCustomerConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.updateRegisteredDetails(editNonUKIdDetails = Some(nonUkiOcrChanges))
        await(response) must be(None)
      }

      "ETMP update for business details failed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val editAgentAddress = EditAgentAddressDetails("Org name", RegisteredAddressDetails("address1", "address2", countryCode = "FR"))
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(cachedData)
        when(mockBusinessCustomerConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(INTERNAL_SERVER_ERROR))
        when(mockDataCacheService.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.updateRegisteredDetails(editAgentDetails = Some(editAgentAddress))
        await(response) must be(None)
      }

      "ETMP update for ocr details failed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val nonUkiOcrChanges = Identification("idnumber", "FR", "issuingInstitution")
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestAgentClientMandateService.agentDetailsFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)
        when(mockBusinessCustomerConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(INTERNAL_SERVER_ERROR))
        when(mockDataCacheService.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = TestAgentClientMandateService.updateRegisteredDetails(editNonUKIdDetails = Some(nonUkiOcrChanges), editAgentDetails = None)
        await(response) must be(None)
      }
    }

    "get clients that have cancelled" when {
      "call is unsuccessful" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.fetchClientsCancelled(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(NOT_FOUND))
        val response = TestAgentClientMandateService.fetchClientsCancelled(arn.utr, service)
        await(response) must be(None)
      }
      "call is successful" in {
        implicit val user = AuthBuilder.createOrgAuthContext(userId, "agent")
        val respJson = Json.toJson(List("AAA", "BBB"))
        when(mockAgentClientMandateConnector.fetchClientsCancelled(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))
        val response = TestAgentClientMandateService.fetchClientsCancelled(arn.utr, service)
        await(response) must be(Some(Seq("AAA", "BBB")))
      }
    }
  }

  val registeredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails = AgentBuilder.buildAgentDetails

  val mandateDto: CreateMandateDto = CreateMandateDto("test@test.com", "ATED", "client display name")
  val time1 = DateTime.now()

  val mockAgentClientMandateConnector = mock[AgentClientMandateConnector]
  val mockDataCacheService = mock[DataCacheService]
  val mockBusinessCustomerConnector = mock[BusinessCustomerConnector]
  val arn = new AgentBusinessUtrGenerator().nextAgentBusinessUtr

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
    currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  val mandateActive: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("X0101000000101", "client name", PartyType.Organisation, ContactDetails("agent@agent.com", None))),
    currentStatus = MandateStatus(Status.Active, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  val mandateApproved: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = None, currentStatus = MandateStatus(Status.Approved, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  val mandatePendingCancellation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123458", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = None, currentStatus = MandateStatus(Status.PendingCancellation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")


  object TestAgentClientMandateService extends AgentClientMandateService {
    override val dataCacheService = mockDataCacheService
    override val agentClientMandateConnector = mockAgentClientMandateConnector
    override val businessCustomerConnector: BusinessCustomerConnector = mockBusinessCustomerConnector
  }

  override def beforeEach = {
    reset(mockDataCacheService)
    reset(mockAgentClientMandateConnector)
  }

}
