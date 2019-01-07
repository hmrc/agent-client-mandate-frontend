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

package unit.uk.gov.hmrc.agentclientmandate.controllers.agent

import java.util.UUID

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.AgentSummaryController
import uk.gov.hmrc.agentclientmandate.models.{MandateStatus, Service, Status, Subscription, _}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, Mandates}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class AgentSummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {


  "AgentClientSummaryController" must {
    "not return NOT_FOUND at route " when {
      "Get /mandate/agent/summary/:service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/summary")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "return page with UR banner" when {
      "the UR banner toggle is activated" in {
        val mockMandates = Some(Mandates(activeMandates = Nil, pendingMandates = Nil))
        viewAuthorisedAgent(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("ATED clients - GOV.UK")
          document.getElementById("ur-panel") must not be null
          document.getElementsByClass("banner-panel__close").text must be("No thanks")
        }
      }
    }

    "return check client details view for agent" when {
      "they have no data" in {
        val mockMandates = Some(Mandates(activeMandates = Nil, pendingMandates = Nil))
        viewAuthorisedAgent(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("ATED clients - GOV.UK")
          document.getElementById("header").text must be("ATED clients")
          document.getElementById("add-client-btn").text() must be("Add a client")
          document.getElementById("add-client-link") must be(null)
          document.getElementById("view-pending-clients") must be(null)
          document.getElementById("view-clients") must be(null)
        }
      }

      "client requests(GET) there are active mandates" in {
        val mockMandates = Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))
        viewAuthorisedAgent(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("ATED clients - GOV.UK")
          document.getElementById("header").text must be("ATED clients")
          document.getElementById("add-client-link").text() must be("Add a client")
          document.getElementById("filter-clients") must be(null)
          document.getElementById("displayName_field") must be(null)
          document.getElementById("add-client-btn") must be(null)
          document.getElementById("view-pending-clients").attr("href") must be("/mandate/agent/summary?tabName=pending-clients")
          document.getElementById("view-clients") must be(null)
        }
      }

      "client requests(GET) there are more than or equal to 15 active mandates" in {
        val mockMandates = Some(Mandates(activeMandates = Seq(mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))
        viewAuthorisedAgent(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("ATED clients - GOV.UK")
          document.getElementById("header").text must be("ATED clients")
          document.getElementById("add-client-link").text() must be("Add a client")
          document.getElementById("filter-clients").text() must be("Filter clients")
          document.getElementById("displayName_field").text() must be("Display name (optional)")
          document.getElementById("add-client-btn") must be(null)
          document.getElementById("view-pending-clients").attr("href") must be("/mandate/agent/summary?tabName=pending-clients")
          document.getElementById("view-clients") must be(null)
        }
      }
    }

    "return check pending details view for agent who wants to see this" when {
      "client requests(GET) for check client details view" in {
        val mockMandates = Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))

        viewAuthorisedAgent(mockMandates, Some("pending-clients")) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("ATED clients - GOV.UK")
          document.getElementById("header").text must be("ATED clients")
          document.getElementById("add-client-link").text() must be("Add a client")
          document.getElementById("view-pending-clients") must be(null)
          document.getElementById("view-clients").attr("href") must be("/mandate/agent/summary")
        }
      }
    }

    "return check pending details view for agent when that's all they have" when {
      "client requests(GET) for check client details view" in {
        val mockMandates = Some(Mandates(activeMandates = Nil, pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))

        viewAuthorisedAgent(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("ATED clients - GOV.UK")
          document.getElementById("header").text must be("ATED clients")
          document.getElementById("add-client-link").text() must be("Add a client")
          document.getElementById("view-pending-clients") must be(null)
          document.getElementById("view-clients") must be(null)
        }
      }
    }

    "redirect to delegated service specific page" when {
      "agent selects and begins delegation on a particular client" in {

        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(Some(mandateActive))
        }

        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

        when(mockDelegationConnector.startDelegation(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(()))
        val result = TestAgentSummaryController.doDelegation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("http://localhost:9916/ated/account-summary"))
      }

      "agent selects and begins delegation but client does not exist" in {

        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(Some(mandateActive.copy(clientParty = None)))
        }

        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

        when(mockDelegationConnector.startDelegation(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(()))
        val result = TestAgentSummaryController.doDelegation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("http://localhost:9916/ated/account-summary"))
      }

      "agent selects client but it fails as we have no serviceId" in {

        val mandateWithNoSubscription = mandateActive.copy(subscription = mandateActive.subscription.copy(referenceNumber = None))
        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(Some(mandateWithNoSubscription))
        }

        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

        when(mockDelegationConnector.startDelegation(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(()))

        val thrown = the[RuntimeException] thrownBy await(TestAgentSummaryController.doDelegation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId)))
        thrown.getMessage must include(s"[AgentSummaryController][doDelegation] Failed to doDelegation to for mandateId 1 for service $service")
      }
    }

    "activate client" when {
      "agent selects and activates client" in {
        activateClientByAuthorisedAgent { result =>

          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/mandate/agent/summary")
        }
      }

      "could not accept client" in {
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

        when(mockAgentClientMandateService.acceptClient(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(false)
        }

        val thrown = the[RuntimeException] thrownBy await(TestAgentSummaryController.activate(service, "mandateId").apply(SessionBuilder.buildRequestWithSession(userId)))
        thrown.getMessage must include("Failed to accept client")
      }

      "could not fetch mandate when accepting client" in {
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
        AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

        when(mockAgentClientMandateService.acceptClient(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(true)
        }
        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(None)
        }

        val thrown = the[RuntimeException] thrownBy await(TestAgentSummaryController.activate(service, "mandateId").apply(SessionBuilder.buildRequestWithSession(userId)))
        thrown.getMessage must include("Failed to fetch client")
      }
    }

    "update view" when {
      "user updates filters" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("showAllClients" -> "allClients")
        updateAuthorisedAgent(fakeRequest, Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))) { result =>
          status(result) must be(OK)
        }
      }

      "user updates filters but there areno mandates" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("showAllClients" -> "allClients")
        updateAuthorisedAgent(fakeRequest, None) { result =>
          status(result) must be(OK)
        }
      }

      "user submits bad data" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("allClients" -> "client display name")
        updateAuthorisedAgent(fakeRequest, Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }

  val mockAuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService = mock[AgentClientMandateService]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockDataCacheService = mock[DataCacheService]

  object TestAgentSummaryController extends AgentSummaryController {
    override val authConnector = mockAuthConnector
    override val agentClientMandateService = mockAgentClientMandateService
    override val delegationConnector = mockDelegationConnector
    override val dataCacheService = mockDataCacheService
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(mockAgentClientMandateService)
    reset(mockDelegationConnector)
    reset(mockDataCacheService)
  }

  val registeredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails = AgentBuilder.buildAgentDetails

  val mandateId = "12345678"
  val time1 = DateTime.now()
  val service = "ATED"
  val atedUtr = new Generator().nextAtedUtr

  val clientParty = Party("12345678", "test client", PartyType.Individual, ContactDetails("a.a@a.com", None))
  val clientParty1 = Party("12345679", "test client1", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty2 = Party("12345671", "test client2", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty3 = Party("12345671", "test client3", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty4 = Party("12345671", "test client4", PartyType.Individual, ContactDetails("aa.aa@a.com", None))

  val mandateNew: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty1), currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 1")

  val mandateActive: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty), currentStatus = MandateStatus(Status.Active, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(Some(atedUtr.utr), Service("ated", "ATED")), clientDisplayName = "client display name 2")

  val mandateApproved: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty3), currentStatus = MandateStatus(Status.Approved, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(Some(atedUtr.utr), Service("ated", "ATED")), clientDisplayName = "client display name 3")

  val mandatePendingCancellation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123458", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty4), currentStatus = MandateStatus(Status.PendingCancellation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 4")

  val mandatePendingActivation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123451", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty2), currentStatus = MandateStatus(Status.PendingActivation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 5")

  def viewAuthorisedAgent(mockMandates: Option[Mandates], tabName: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    when(mockAgentClientMandateService.fetchAllClientMandates(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
      Future.successful(mockMandates)
    }
    when(mockAgentClientMandateService.fetchAgentDetails()(Matchers.any(), Matchers.any())) thenReturn Future.successful(agentDetails)
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some("text"))
    when(mockDataCacheService.cacheFormData[String](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful("text")
    when(mockAgentClientMandateService.fetchClientsCancelled(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)

    val result = TestAgentSummaryController.view(service, tabName).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def activateClientByAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    when(mockAgentClientMandateService.acceptClient(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
      Future.successful(true)
    }
    when(mockAgentClientMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
      Future.successful(Some(mandateActive))
    }
    when(mockAgentClientMandateService.fetchAllClientMandates(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
      Future.successful(Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation))))
    }
    when(mockAgentClientMandateService.fetchAgentDetails()(Matchers.any(), Matchers.any())) thenReturn Future.successful(agentDetails)

    when(mockDataCacheService.cacheFormData[String](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful("text")

    val result = TestAgentSummaryController.activate(service, "mandateId").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def updateAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded], mockMandates: Option[Mandates])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    when(mockAgentClientMandateService.fetchAllClientMandates(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
      Future.successful(mockMandates)
    }
    when(mockAgentClientMandateService.fetchAgentDetails()(Matchers.any(), Matchers.any())) thenReturn Future.successful(agentDetails)
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some("text"))
    when(mockDataCacheService.cacheFormData[String](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful("text")
    when(mockAgentClientMandateService.fetchClientsCancelled(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)

    val result = TestAgentSummaryController.update(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
