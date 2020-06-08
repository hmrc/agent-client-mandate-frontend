/*
 * Copyright 2020 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.{AgentClientMandateConnector, BusinessCustomerConnector}
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, Mandates}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayName, ClientMandateDisplayDetails, EditAgentAddressDetails}
import uk.gov.hmrc.domain.AgentBusinessUtr
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AgentBusinessUtrGenerator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class AgentClientMandateServiceSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach {

  class Setup {
    val service = new AgentClientMandateService(
      mockDataCacheService,
      mockAgentClientMandateConnector,
      mockBusinessCustomerConnector
    )
  }

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

      "no agent email is found in the keystore" in new Setup {
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](ArgumentMatchers.eq(service.agentEmailFormId))(any(), any()))
          .thenReturn (Future.successful(None))

        val response = service.createMandate(serviceName, testAgentAuthRetrievals)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Email not found in cache")

      }

      "no client display name is found in the keystore" in new Setup {
        val cachedEmail = AgentEmail("aa@aa.com")
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](ArgumentMatchers.eq(service.agentEmailFormId))(any(), any()))
          .thenReturn (Future.successful(Some(cachedEmail)))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](ArgumentMatchers.eq(service.clientDisplayNameFormId))(any(), any()))
          .thenReturn (Future.successful(None))

        val response = service.createMandate(serviceName, testAgentAuthRetrievals)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Client Display Name not found in cache")

      }

      "there is a problem while creating the mandate" in new Setup {
        val cachedEmail = AgentEmail("aa@aa.com")
        val displayName = ClientDisplayName("client display name")

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](ArgumentMatchers.eq(service.agentEmailFormId))(any(), any()))
          .thenReturn (Future.successful(Some(cachedEmail)))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](ArgumentMatchers.eq(service.clientDisplayNameFormId))(any(), any()))
          .thenReturn (Future.successful(Some(displayName)))
        when(mockAgentClientMandateConnector.createMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = service.createMandate(serviceName, testAgentAuthRetrievals)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Mandate not created")

      }
    }

    "create a mandate" when {

      "agent email is found in the keystore" in new Setup {
        val cachedEmail = AgentEmail("aa@aa.com")
        val displayName = ClientDisplayName("client display name")
        val respJson = Json.parse("""{"mandateId": "AS12345678"}""")

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](ArgumentMatchers.eq(service.agentEmailFormId))(any(), any()))
          .thenReturn (Future.successful(Some(cachedEmail)))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](ArgumentMatchers.eq(service.clientDisplayNameFormId))(any(), any()))
          .thenReturn (Future.successful(Some(displayName)))
        when(mockAgentClientMandateConnector.createMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(CREATED, Some(respJson)))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        when(mockDataCacheService.cacheFormData[ClientMandateDisplayDetails](ArgumentMatchers.eq(service.agentRefCacheId), any())(any(), any()))
          .thenReturn(Future.successful(ClientMandateDisplayDetails("test name", "AS12345678", agentLastUsedEmail)))

        val response = service.createMandate(serviceName, testAgentAuthRetrievals)
        await(response) must be("AS12345678")

      }

    }

    "not fetch any mandate" when {

      "incorrect mandate id is passed" in new Setup {
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = service.fetchClientMandate(mandateId, testAgentAuthRetrievals)
        await(response) must be(None)
      }

    }

    "fetch correct mandate" when {

      "correct mandate id is passed" in new Setup {
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = service.fetchClientMandate(mandateId, testClientAuthRetrievals)
        await(response) must be(Some(mandateNew))
      }

      "fail to fetch" in new Setup {
        val respJson: JsValue = Json.parse("""{
            |"id":"12345678",
            |"createdBy":{"credId":"credId","name":"agentName","groupId":"agentCode"},
            |"agentParty":{"id":"JARN123456","name":"agency name","type":"Organisation","contactDetails":{"email":"agent@agent.com"}},
            |"clientParty":{"id":"X0101000000101","name":"client name","type":"Organisation",
            |"contactDetails":{"email":"agent@agent.com"}},
            |"currentStatus":{"status":"New","timestamp":"weqweqeqweqe","updatedBy":"credId"},
            |"statusHistory":[],
            |"subscription":{"service":{"id":"ated","name":"ATED"}},
            |"clientDisplayName":"client display name"
            |}
          |""".stripMargin)

        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = service.fetchClientMandate(mandateId, testClientAuthRetrievals)
        await(response) must be(None)
      }

    }

    "fetch correct mandate client name" when {

      "correct mandate id is passed" in new Setup {
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = service.fetchClientMandateClientName(mandateId, testAgentAuthRetrievals)
        await(response) must be(mandateNew)
      }

      "throws an exception when no Mandate found" in new Setup {
        val respJson = Json.parse("{}")
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = service.fetchClientMandateClientName(mandateId, testClientAuthRetrievals)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include(s"[AgentClientMandateService][fetchClientMandateClientName] No Mandate returned for id $mandateId")
      }
    }

    "fetch correct mandate agent name" when {

      "correct mandate id is passed" in new Setup {
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = service.fetchClientMandateAgentName(mandateId, testClientAuthRetrievals)
        await(response) must be(mandateNew.agentParty.name)
      }
      "throws an exception when no Mandate found" in new Setup {
        val respJson = Json.parse("{}")
        when(mockAgentClientMandateConnector.fetchMandate(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = service.fetchClientMandateAgentName(mandateId, testAgentAuthRetrievals)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include(s"[AgentClientMandateService][fetchClientMandateAgentName] No Mandate Agent Name returned with id $mandateId")
      }
    }

    "fetch all mandates" when {

      "filter mandates when status is checked" in new Setup {
        val respJson = Json.toJson(Seq(mandateNew, mandateActive, mandatePendingCancellation, mandateApproved))
        when(mockAgentClientMandateConnector.fetchAllMandates(any(), any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = service.fetchAllClientMandates(testAgentAuthRetrievals, serviceName)
        await(response) must be(Some(Mandates(
          activeMandates = Seq(mandateActive),
          pendingMandates = Seq(mandateNew, mandatePendingCancellation, mandateApproved))))

      }

      "return none when json wont map to case class" in new Setup {

        val respJson = Json.obj("Wrong" -> "format")
        when(mockAgentClientMandateConnector.fetchAllMandates(any(), any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = service.fetchAllClientMandates(testAgentAuthRetrievals, serviceName)
        await(response) must be(None)
      }

      "return none when no mandates found" in new Setup {
        //        val respJson = Json.obj("Wrong" -> "format")
        when(mockAgentClientMandateConnector.fetchAllMandates(any(), any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(NOT_FOUND, None))

        val response = service.fetchAllClientMandates(testAgentAuthRetrievals, serviceName)
        await(response) must be(None)
      }

    }

    "send approved mandate to backend and caches the response in keystore" when {
      "client approves it and response status is OK" in new Setup {
        val responseJson = Json.toJson(mandateNew)

        when(mockAgentClientMandateConnector.approveMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        when(mockDataCacheService.cacheFormData[Mandate](ArgumentMatchers.eq(service.clientApprovedMandateId), any())(any(), any()))
          .thenReturn(Future.successful(mandateNew))

        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.approveMandate(mandateNew, testClientAuthRetrievals)
        await(response) must be(Some(mandateNew))
      }
    }

    "return none" when {
      "backend call failed with status other than OK" in new Setup {
        when(mockAgentClientMandateConnector.approveMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
        val response = service.approveMandate(mandateNew, testClientAuthRetrievals)
        await(response) must be(None)
      }
    }

    "reject client" when {
      "agent rejects client status returned ok" in new Setup {
        when(mockAgentClientMandateConnector.rejectClient(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.rejectClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(true)
      }

      "agent rejects client status returned not ok" in new Setup {
        when(mockAgentClientMandateConnector.rejectClient(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = service.rejectClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(false)
      }
    }

    "fetch agent details" in new Setup {
      when(mockAgentClientMandateConnector.fetchAgentDetails(any())(any()))
        .thenReturn(Future.successful(agentDetails))
      val response = service.fetchAgentDetails(testAgentAuthRetrievals)
      await(response) must be(agentDetails)
    }

    "accept a client" when {

      "backend connector call succeeds with status OK" in new Setup {
        when(mockAgentClientMandateConnector.activateMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.acceptClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(true)
      }
    }

    "not accept a client" when {

      "backend connector call fails with status not OK" in new Setup {
        when(mockAgentClientMandateConnector.activateMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = service.acceptClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(false)
      }
    }

    "remove client" when {
      "agent removes client status returned ok" in new Setup {
        when(mockAgentClientMandateConnector.remove(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.removeClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(true)
      }

      "agent removes client status returned not ok" in new Setup {
        when(mockAgentClientMandateConnector.remove(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = service.removeClient(mandateId, testAgentAuthRetrievals)
        await(response) must be(false)
      }
    }

    "remove agent" when {
      "client removes agent status returned ok" in new Setup {
        when(mockAgentClientMandateConnector.remove(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.removeAgent(mandateId, testClientAuthRetrievals)
        await(response) must be(true)
      }

      "client removes agent status returned not ok" in new Setup {
        when(mockAgentClientMandateConnector.remove(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        val response = service.removeAgent(mandateId, testClientAuthRetrievals)
        await(response) must be(false)
      }
    }

    "edit client details" when {
      "edit mandate status returned OK" in new Setup {
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.editMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))
        val response = service.editMandate(mandateNew, testAgentAuthRetrievals)
        await(response) must be(Some(mandateNew))
      }
    }


    "not edit client details" when {
      "edit mandate status does not return OK" in new Setup {
        when(mockAgentClientMandateConnector.editMandate(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))
        val response = service.editMandate(mandateNew, testAgentAuthRetrievals)
        await(response) must be(None)
      }
    }

    "fetch mandate for client" when {
      "returns a mandate when client party exists, is active, and for correct service" in new Setup {
        val respJson = Json.toJson(mandateActive)
        when(mockAgentClientMandateConnector.fetchMandateByClient(any(), any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))
        val response = service.fetchClientMandateByClient("clientId", "service", testClientAuthRetrievals)
        await(response) must be(Some(mandateActive))
      }

      "returns None for all other" in new Setup {
//        val respJson = Json.toJson(mandateActive)
        when(mockAgentClientMandateConnector.fetchMandateByClient(any(), any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND)))
        val response = service.fetchClientMandateByClient("clientId", "service", testClientAuthRetrievals)
        await(response) must be(None)
      }
    }

    "fetch mandate for client id" when {
      "returns a mandate when client party exists, is active, and for correct service" in new Setup {
        val respJson = Json.toJson(mandateActive)
        when(mockAgentClientMandateConnector.fetchMandateByClientId(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))
        val response = service.fetchClientMandateByClientId("clientId", "service")
        await(response) must be(Some(mandateActive))
      }

      "returns None for all other" in new Setup {
        when(mockAgentClientMandateConnector.fetchMandateByClientId(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND)))
        val response = service.fetchClientMandateByClientId("clientId", "service")
        await(response) must be(None)
      }
    }

    "check for agent missing email" must {
      "return false if agent is missing email" in new Setup {
        when(mockAgentClientMandateConnector.doesAgentHaveMissingEmail(any(), any())(any())) thenReturn Future.successful(HttpResponse(NO_CONTENT))
        when(mockAgentClientMandateConnector.updateAgentCredId(any())(any())) thenReturn Future.successful(HttpResponse(OK))
        val response = service.doesAgentHaveMissingEmail("ated", testAgentAuthRetrievals)
        await(response) must be(false)
      }

      "return true if agent is missing email" in new Setup {
        when(mockAgentClientMandateConnector.doesAgentHaveMissingEmail(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockAgentClientMandateConnector.updateAgentCredId(any())(any())) thenReturn Future.successful(HttpResponse(OK))
        val response = service.doesAgentHaveMissingEmail("ated", testAgentAuthRetrievals)
        await(response) must be(true)
      }
    }

    "update agent email" must {
      "update an agents missing email address" in new Setup {
        service.updateAgentMissingEmail("test@mail.com", testAgentAuthRetrievals, "ated")
      }
    }

    "update client email" must {
      "update a clients email address" in new Setup {
        service.updateClientEmail("test@mail.com", "mandateId", testClientAuthRetrievals)
      }
    }

    "update the agent business details" when {
      "business details are changed and saved" in new Setup {
        val editAgentAddress = EditAgentAddressDetails("Org name", RegisteredAddressDetails("address1", "address2", countryCode = "FR"))
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(isAnIndividual = false, None,
          Some(Organisation("Org name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"),
          EtmpContactDetails(None, None, None, None), isAnAgent = true, isAGroup = true, None))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](ArgumentMatchers.eq(service.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(cachedData))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals,  editAgentDetails = Some(editAgentAddress))
        await(response) must be(updateRegDetails)
      }

      "ocr details are changed and saved" in new Setup {
        val nonUkiOcrChanges = Identification("idnumber", "FR", "issuingInstitution")
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(isAnIndividual = false, None,
          Some(Organisation("Org Name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"),
          EtmpContactDetails(None, None, None, None), isAnAgent = true, isAGroup = true, Some(Identification("idnumber", "FR", "issuingInstitution"))))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](ArgumentMatchers.eq(service.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(cachedData))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals, editNonUKIdDetails = Some(nonUkiOcrChanges))
        await(response) must be(updateRegDetails)
      }
    }

    "fail to update the agent business details" when {
      "none of the inputs are passed" in new Setup {
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(isAnIndividual = false, None,
          Some(Organisation("Org Name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"),
          EtmpContactDetails(None, None, None, None), isAnAgent = true, isAGroup = true, None))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](ArgumentMatchers.eq(service.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(cachedData))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals)
        await(response) must be(updateRegDetails)
      }

      "no data found in cache" in new Setup {
        val nonUkiOcrChanges = Identification("idnumber", "FR", "issuingInstitution")
//        val cachedData = Some(AgentBuilder.buildAgentDetails)
//        val updateRegDetails = Some(UpdateRegistrationDetailsRequest(isAnIndividual = false, None,
//          Some(Organisation("Org Name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"),
//          EtmpContactDetails(None, None, None, None), isAnAgent = true, isAGroup = true, Some(Identification("idnumber", "FR", "issuingInstitution"))))
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](ArgumentMatchers.eq(service.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(None))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any())) thenReturn Future.successful(HttpResponse(OK))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals, editNonUKIdDetails = Some(nonUkiOcrChanges))
        await(response) must be(None)
      }

      "ETMP update for business details failed" in new Setup {
        val editAgentAddress = EditAgentAddressDetails("Org name", RegisteredAddressDetails("address1", "address2", countryCode = "FR"))
        val cachedData = Some(AgentBuilder.buildAgentDetails)
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](ArgumentMatchers.eq(service.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(cachedData))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any()))
          .thenReturn (Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals, editAgentDetails = Some(editAgentAddress))
        await(response) must be(None)
      }

      "ETMP update for ocr details failed" in new Setup {
        val nonUkiOcrChanges = Identification("idnumber", "FR", "issuingInstitution")
        when(mockDataCacheService.fetchAndGetFormData[AgentDetails](ArgumentMatchers.eq(service.agentDetailsFormId))(any(), any()))
          .thenReturn (Future.successful(None))
        when(mockBusinessCustomerConnector.updateRegistrationDetails(any(), any(), any())(any()))
          .thenReturn (Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        when(mockDataCacheService.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))
        val response = service.updateRegisteredDetails(agentAuthRetrievals = testAgentAuthRetrievals,
          editNonUKIdDetails = Some(nonUkiOcrChanges), editAgentDetails = None)
        await(response) must be(None)
      }
    }

    "get clients that have cancelled" when {
      "call is unsuccessful" in new Setup {
        when(mockAgentClientMandateConnector.fetchClientsCancelled(any(), any())(any())) thenReturn Future.successful(HttpResponse(NOT_FOUND))
        val response = service.fetchClientsCancelled(testAgentAuthRetrievals, serviceName)
        await(response) must be(None)
      }
      "call is successful" in new Setup {
        val respJson = Json.toJson(List("AAA", "BBB"))
        when(mockAgentClientMandateConnector.fetchClientsCancelled(any(), any())(any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))
        val response = service.fetchClientsCancelled(testAgentAuthRetrievals, serviceName)
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
  val serviceName = "ATED"
  val mandateId = "12345678"
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


  override def beforeEach: Unit = {
    reset(mockDataCacheService)
    reset(mockAgentClientMandateConnector)
  }

}
