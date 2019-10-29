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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.connectors.DelegationConnector
import uk.gov.hmrc.agentclientmandate.controllers.agent.AgentSummaryController
import uk.gov.hmrc.agentclientmandate.models.{MandateStatus, Service, Status, Subscription, _}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, Mandates}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.{AtedUtr, Generator}
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AgentSummaryControllerSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach with MockControllerSetup {


  "AgentClientSummaryController" must {

    "return page with UR banner" when {
      "the UR banner toggle is activated" in new Setup {
        val mockMandates = Some(Mandates(activeMandates = Nil, pendingMandates = Nil))
        viewAuthorisedAgent(controller)(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.summary.title - GOV.UK")
          document.getElementById("ur-panel") must not be null
          document.getElementsByClass("banner-panel__close").text must be("urbanner.message.reject")
        }
      }
    }

    "return check client details view for agent" when {
      "they have no data" in new Setup {
        val mockMandates = Some(Mandates(activeMandates = Nil, pendingMandates = Nil))
        viewAuthorisedAgent(controller)(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.summary.title - GOV.UK")
          document.getElementById("header").text must be("client.summary.title")
          document.getElementById("add-client-btn").text() must be("client.summary.add-client")
          document.getElementById("add-client-link") must be(null)
          document.getElementById("view-pending-clients") must be(null)
          document.getElementById("view-clients") must be(null)
        }
      }

      "client requests(GET) there are active mandates" in new Setup {
        val mockMandates = Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))
        viewAuthorisedAgent(controller)(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.summary.title - GOV.UK")
          document.getElementById("header").text must be("client.summary.title")
          document.getElementById("add-client-link").text() must be("client.summary.add-client")
          document.getElementById("filter-clients") must be(null)
          document.getElementById("displayName_field") must be(null)
          document.getElementById("add-client-btn") must be(null)
          document.getElementById("view-pending-clients").attr("href") must be("/agent/summary?tabName=pending-clients")
          document.getElementById("view-clients") must be(null)
        }
      }

      "client requests(GET) there are more than or equal to 15 active mandates" in new Setup {
        val mockMandates = Some(Mandates(activeMandates = Seq(mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))
        viewAuthorisedAgent(controller)(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.summary.title - GOV.UK")
          document.getElementById("header").text must be("client.summary.title")
          document.getElementById("add-client-link").text() must be("client.summary.add-client")
          document.getElementById("filter-clients").text() must be("client.summary.filter-clients")
          document.getElementById("displayName_field").text() must be("client.summary.filter-display_name")
          document.getElementById("add-client-btn") must be(null)
          document.getElementById("view-pending-clients").attr("href") must be("/agent/summary?tabName=pending-clients")
          document.getElementById("view-clients") must be(null)
        }
      }
    }

    "return check pending details view for agent who wants to see this" when {
      "client requests(GET) for check client details view" in new Setup {
        val mockMandates = Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))

        viewAuthorisedAgent(controller)(mockMandates, Some("pending-clients")) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.summary.title - GOV.UK")
          document.getElementById("header").text must be("client.summary.title")
          document.getElementById("add-client-link").text() must be("client.summary.add-client")
          document.getElementById("view-pending-clients") must be(null)
          document.getElementById("view-clients").attr("href") must be("/agent/summary")
        }
      }
    }

    "return check pending details view for agent when that's all they have" when {
      "client requests(GET) for check client details view" in new Setup {
        val mockMandates = Some(Mandates(activeMandates = Nil, pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))

        viewAuthorisedAgent(controller)(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.summary.title - GOV.UK")
          document.getElementById("header").text must be("client.summary.title")
          document.getElementById("add-client-link").text() must be("client.summary.add-client")
          document.getElementById("view-pending-clients") must be(null)
          document.getElementById("view-clients") must be(null)
        }
      }
    }

    "redirect to delegated service specific page" when {
      "agent selects and begins delegation on a particular client" in new Setup {

        when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(Some(mandateActive))
        }

        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

        when(mockDelegationConnector.startDelegation(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(true))
        val result = controller.doDelegation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("http://localhost:9916/ated/account-summary"))
      }

      "agent selects and begins delegation but client does not exist" in new Setup {

        when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(Some(mandateActive.copy(clientParty = None)))
        }

        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

        when(mockDelegationConnector.startDelegation(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(true))
        val result = controller.doDelegation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("http://localhost:9916/ated/account-summary"))
      }

      "agent selects client but it fails as we have no serviceId" in new Setup {

        val mandateWithNoSubscription = mandateActive.copy(subscription = mandateActive.subscription.copy(referenceNumber = None))
        when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(Some(mandateWithNoSubscription))
        }

        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

        when(mockDelegationConnector.startDelegation(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(true))

        val thrown = the[RuntimeException] thrownBy await(controller.doDelegation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId)))
        thrown.getMessage must include(s"[AgentSummaryController][doDelegation] Failed to doDelegation to for mandateId 1 for service $service")
      }
    }

    "activate client" when {
      "agent selects and activates client" in new Setup {
        activateClientByAuthorisedAgent(controller) { result =>

          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/agent/summary")
        }
      }

      "could not accept client" in new Setup {
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

        when(mockAgentClientMandateService.acceptClient(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(false)
        }

        val thrown = the[RuntimeException] thrownBy await(controller.activate(service, "mandateId").apply(SessionBuilder.buildRequestWithSession(userId)))
        thrown.getMessage must include("Failed to accept client")
      }

      "could not fetch mandate when accepting client" in new Setup {
        val userId = s"user-${UUID.randomUUID}"
        implicit val hc: HeaderCarrier = HeaderCarrier()
        AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

        when(mockAgentClientMandateService.acceptClient(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(true)
        }
        when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(None)
        }

        val thrown = the[RuntimeException] thrownBy await(controller.activate(service, "mandateId").apply(SessionBuilder.buildRequestWithSession(userId)))
        thrown.getMessage must include("Failed to fetch client")
      }
    }

    "update view" when {
      "user updates filters" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("showAllClients" -> "allClients")
        updateAuthorisedAgent(controller)(fakeRequest, Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))) { result =>
          status(result) must be(OK)
        }
      }

      "user updates filters but there areno mandates" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("showAllClients" -> "allClients")
        updateAuthorisedAgent(controller)(fakeRequest, None) { result =>
          status(result) must be(OK)
        }
      }

      "user submits bad data" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("allClients" -> "client display name")
        updateAuthorisedAgent(controller)(fakeRequest, Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val mockDelegationConnector: DelegationConnector = mock[DelegationConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  class Setup {
    val controller = new AgentSummaryController(
      mockAgentClientMandateService,
      mockDataCacheService,
      mockDelegationConnector,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig
    )
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockAgentClientMandateService)
    reset(mockDelegationConnector)
    reset(mockDataCacheService)
  }

  val registeredAddressDetails: RegisteredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails: AgentDetails = AgentBuilder.buildAgentDetails

  val mandateId: String = "12345678"
  val time1: DateTime = DateTime.now()
  val service: String = "ATED"
  val atedUtr: AtedUtr = new Generator().nextAtedUtr

  val clientParty: Party = Party("12345678", "test client", PartyType.Individual, ContactDetails("a.a@a.com", None))
  val clientParty1: Party = Party("12345679", "test client1", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty2: Party = Party("12345671", "test client2", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty3: Party = Party("12345671", "test client3", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty4: Party = Party("12345671", "test client4", PartyType.Individual, ContactDetails("aa.aa@a.com", None))

  val mandateNew: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty1), currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 1")

  val mandateActive: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty), currentStatus = MandateStatus(Status.Active, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(Some(atedUtr.utr), Service("ated", "ATED")), clientDisplayName = "client display name 2")

  val mandateApproved: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty3), currentStatus = MandateStatus(Status.Approved, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(Some(atedUtr.utr), Service("ated", "ATED")), clientDisplayName = "client display name 3")

  val mandatePendingCancellation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123458", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty4), currentStatus = MandateStatus(Status.PendingCancellation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 4")

  val mandatePendingActivation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123451", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty2), currentStatus = MandateStatus(Status.PendingActivation, time1, "credId"), statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 5")

  def viewAuthorisedAgent(controller: AgentSummaryController)(mockMandates: Option[Mandates], tabName: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

    when(mockAgentClientMandateService.fetchAllClientMandates(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
      Future.successful(mockMandates)
    }
    when(mockAgentClientMandateService.fetchAgentDetails(ArgumentMatchers.any())(ArgumentMatchers.any())) thenReturn Future.successful(agentDetails)
    when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(Some("text"))
    when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful("text")
    when(mockAgentClientMandateService.fetchClientsCancelled(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(None)

    val result = controller.view(service, tabName).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def activateClientByAuthorisedAgent(controller: AgentSummaryController)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

    when(mockAgentClientMandateService.acceptClient(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
      Future.successful(true)
    }
    when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
      Future.successful(Some(mandateActive))
    }
    when(mockAgentClientMandateService.fetchAllClientMandates(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
      Future.successful(Some(Mandates(activeMandates = Seq(mandateActive), pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation))))
    }
    when(mockAgentClientMandateService.fetchAgentDetails(ArgumentMatchers.any())(ArgumentMatchers.any())) thenReturn Future.successful(agentDetails)

    when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful("text")

    val result = controller.activate(service, "mandateId").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def updateAuthorisedAgent(controller: AgentSummaryController)(request: FakeRequest[AnyContentAsFormUrlEncoded], mockMandates: Option[Mandates])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

    when(mockAgentClientMandateService.fetchAllClientMandates(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
      Future.successful(mockMandates)
    }
    when(mockAgentClientMandateService.fetchAgentDetails(ArgumentMatchers.any())(ArgumentMatchers.any())) thenReturn Future.successful(agentDetails)
    when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(Some("text"))
    when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful("text")
    when(mockAgentClientMandateService.fetchClientsCancelled(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(None)

    val result = controller.update(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
