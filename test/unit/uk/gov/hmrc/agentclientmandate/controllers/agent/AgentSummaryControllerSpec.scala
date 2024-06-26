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

package unit.uk.gov.hmrc.agentclientmandate.controllers.agent

import java.util.UUID
import java.time.Instant
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.DelegationConnector
import uk.gov.hmrc.agentclientmandate.controllers.agent.AgentSummaryController
import uk.gov.hmrc.agentclientmandate.models.{MandateStatus, Service, Status, Subscription, _}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, Mandates}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.agentSummary.{clients, noClientsNoPending}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.{AtedUtr, Generator}
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentSummaryControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val mockDelegationConnector: DelegationConnector = mock[DelegationConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val injectedViewInstanceClients: clients = app.injector.instanceOf[views.html.agent.agentSummary.clients]
  val injectedViewInstanceNoClientsNoPending: noClientsNoPending = app.injector.instanceOf[views.html.agent.agentSummary.noClientsNoPending]

  class Setup {
    val controller = new AgentSummaryController(
      mockAgentClientMandateService,
      mockDataCacheService,
      mockDelegationConnector,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig,
      injectedViewInstanceClients,
      injectedViewInstanceNoClientsNoPending
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
  val time1: Instant = Instant.now()
  val service: String = "ATED"
  val atedUtr: AtedUtr = new Generator().nextAtedUtr

  val clientParty: Party = Party("12345678", "test client", PartyType.Individual, ContactDetails("a.a@a.com", None))
  val clientParty1: Party = Party("12345679", "test client1", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty2: Party = Party("12345671", "test client2", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty3: Party = Party("12345671", "test client3", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty4: Party = Party("12345671", "test client4", PartyType.Individual, ContactDetails("aa.aa@a.com", None))

  val mandateNew: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None,
    None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(clientParty1), currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Nil,
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 1")

  val mandateActive: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")),
    None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com",
      None)), clientParty = Some(clientParty), currentStatus = MandateStatus(Status.Active, time1, "credId"),
    statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(Some(atedUtr.utr),
      Service("ated", "ATED")), clientDisplayName = "client display name 2")

  val mandateApproved: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")),
    None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com",
      None)), clientParty = Some(clientParty3), currentStatus = MandateStatus(Status.Approved, time1, "credId"),
    statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(Some(atedUtr.utr),
      Service("ated", "ATED")), clientDisplayName = "client display name 3")

  val mandatePendingCancellation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName",
    Some("agentCode")), None, None, agentParty = Party("JARN123458", "agency name", PartyType.Organisation,
    ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty4),
    currentStatus = MandateStatus(Status.PendingCancellation, time1, "credId"),
    statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name 4")

  val mandatePendingActivation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName",
    Some("agentCode")), None, None, agentParty = Party("JARN123451", "agency name", PartyType.Organisation,
    ContactDetails("agent@agent.com", None)), clientParty = Some(clientParty2),
    currentStatus = MandateStatus(Status.PendingActivation, time1, "credId"),
    statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name 5")

  def viewAuthorisedAgent(controller: AgentSummaryController)(mockMandates: Option[Mandates])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

    when(mockAgentClientMandateService.fetchAllClientMandates(ArgumentMatchers.any(), ArgumentMatchers.any(),
      ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
      Future.successful(mockMandates)
    }
    when(mockAgentClientMandateService.fetchAgentDetails(ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any())) thenReturn Future.successful(agentDetails)
    when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(Some("text"))

    when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful("text")

    when(mockAgentClientMandateService.fetchClientsCancelled(ArgumentMatchers.any(),
      ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(None)

    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def activateClientByAuthorisedAgent(controller: AgentSummaryController)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

    when(mockAgentClientMandateService.acceptClient(ArgumentMatchers.any(),
      ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
      Future.successful(true)
    }
    when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(),
      ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
    ) thenReturn {
      Future.successful(Some(mandateActive))
    }
    when(mockAgentClientMandateService.fetchAllClientMandates(ArgumentMatchers.any(),
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any())) thenReturn {
      Future.successful(Some(Mandates(activeMandates = Seq(mandateActive),
        pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation))))
    }
    when(mockAgentClientMandateService.fetchAgentDetails(ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any())) thenReturn Future.successful(agentDetails)

    when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful("text")

    val result = controller.activate(service, "mandateId").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def updateAuthorisedAgent(controller: AgentSummaryController)(
    request: FakeRequest[AnyContentAsFormUrlEncoded], mockMandates: Option[Mandates])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

    when(mockAgentClientMandateService.fetchAllClientMandates(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any())) thenReturn {
      Future.successful(mockMandates)
    }
    when(mockAgentClientMandateService.fetchAgentDetails(ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any())) thenReturn Future.successful(agentDetails)
    when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(Some("text"))
    when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.any(), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful("text")
    when(mockAgentClientMandateService.fetchClientsCancelled(ArgumentMatchers.any(), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(None)

    val result = controller.update(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  "AgentClientSummaryController" must {

    "return check client details view for agent" when {
      "they have no data" in new Setup {
        val mockMandates: Option[Mandates] = Some(Mandates(activeMandates = Nil, pendingMandates = Nil))
        viewAuthorisedAgent(controller)(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.summary.title - service.name - GOV.UK")
          document.getElementById("header").text must be("client.summary.title")
          document.getElementById("add-client-btn").text() must be("client.summary.add-client")
          document.getElementById("add-client-link") must be(null)
          document.getElementById("view-pending-clients") must be(null)
          document.getElementById("view-clients") must be(null)
        }
      }

      "client requests(GET) there are active mandates" in new Setup {
        val mockMandates: Option[Mandates] = Some(Mandates(activeMandates = Seq(mandateActive),
          pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation))
        )
        viewAuthorisedAgent(controller)(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-visually-hidden").eq(0).text must be("client.summary.hidden.client_activated")
          document.title() must be("client.summary.title - service.name - GOV.UK")
          document.getElementById("header").text must be("client.summary.title")
          document.getElementById("add-client-link").text() must be("client.summary.add-client")
          document.getElementById("filter-clients") must be(null)
          document.getElementById("displayName_field") must be(null)
          document.getElementById("add-client-btn") must be(null)
          document.getElementById("pending-mandate-tab").attr("href") must be("#pending-mandates")
        }
      }

      "client requests(GET) there are more than or equal to 15 active mandates" in new Setup {
        val mockMandates: Option[Mandates] = Some(Mandates(activeMandates = Seq(mandateActive, mandateActive, mandateActive,
          mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive,
          mandateActive, mandateActive, mandateActive, mandateActive, mandateActive),
          pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation))
        )
        viewAuthorisedAgent(controller)(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.summary.title - service.name - GOV.UK")
          document.getElementById("header").text must be("client.summary.title")
          document.getElementById("add-client-link").text() must be("client.summary.add-client")
          document.getElementById("filter-clients").text() must be("client.summary.filter-clients")
          document.getElementsByClass("govuk-label").first().text() must be("client.summary.filter-display_name")
          document.getElementById("add-client-btn") must be(null)
          document.getElementById("pending-mandate-tab").attr("href") must be("#pending-mandates")
        }
      }
    }

    "return check pending details view for agent who wants to see this" when {
      "client requests(GET) for check client details view" in new Setup {
        val mockMandates: Option[Mandates] = Some(Mandates(activeMandates = Seq(mandateActive),
          pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation))
        )

        viewAuthorisedAgent(controller)(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.summary.title - service.name - GOV.UK")
          document.getElementById("header").text must be("client.summary.title")
          document.getElementById("add-client-link").text() must be("client.summary.add-client")
          document.getElementById("active-mandate-tab").attr("href") must be("#active-mandates")
        }
      }
    }

    "return check pending details view for agent when that's all they have" when {
      "client requests(GET) for check client details view" in new Setup {
        val mockMandates: Option[Mandates] = Some(Mandates(activeMandates = Nil, pendingMandates = Seq(mandateNew,
          mandatePendingActivation, mandateApproved, mandatePendingCancellation))
        )

        viewAuthorisedAgent(controller)(mockMandates) { result =>

          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.summary.title - service.name - GOV.UK")
          document.getElementById("header").text must be("client.summary.title")
          document.getElementById("add-client-link").text() must be("client.summary.add-client")
          document.getElementById("pending-mandate-tab").attr("href") must be("#pending-mandates")
          document.getElementById("active-mandate-tab").attr("href") must be("#active-mandates")
        }
      }
    }

    "redirect to delegated service specific page" when {
      "agent selects and begins delegation on a particular client" in new Setup {

        when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        ) thenReturn {
          Future.successful(Some(mandateActive))
        }

        val userId = s"user-${UUID.randomUUID}"

        AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

        when(mockDelegationConnector.startDelegation(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true)
        )
        val result: Future[Result] = controller.doDelegation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("http://localhost:9916/ated/account-summary"))
      }

      "agent selects and begins delegation but client does not exist" in new Setup {

        when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        ) thenReturn {
          Future.successful(Some(mandateActive.copy(clientParty = None)))
        }

        val userId = s"user-${UUID.randomUUID}"

        AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

        when(mockDelegationConnector.startDelegation(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true)
        )
        val result: Future[Result] = controller.doDelegation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some("http://localhost:9916/ated/account-summary"))
      }

      "agent selects client but it fails as we have no serviceId" in new Setup {

        val mandateWithNoSubscription: Mandate = mandateActive.copy(subscription = mandateActive.subscription.copy(referenceNumber = None))
        when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        ) thenReturn {
          Future.successful(Some(mandateWithNoSubscription))
        }

        val userId = s"user-${UUID.randomUUID}"

        AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

        when(mockDelegationConnector.startDelegation(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true)
        )

        val thrown: RuntimeException = the[RuntimeException] thrownBy
          await(controller.doDelegation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId)))
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

        AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

        when(mockAgentClientMandateService.acceptClient(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        ) thenReturn {
          Future.successful(false)
        }

        val thrown: RuntimeException = the[RuntimeException] thrownBy
          await(controller.activate(service, "mandateId").apply(SessionBuilder.buildRequestWithSession(userId)))
        thrown.getMessage must include("Failed to accept client")
      }

      "could not fetch mandate when accepting client" in new Setup {
        val userId = s"user-${UUID.randomUUID}"

        AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

        when(mockAgentClientMandateService.acceptClient(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        ) thenReturn {
          Future.successful(true)
        }
        when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        ) thenReturn {
          Future.successful(None)
        }

        val thrown: RuntimeException = the[RuntimeException] thrownBy
          await(controller.activate(service, "mandateId").apply(SessionBuilder.buildRequestWithSession(userId)))
        thrown.getMessage must include("Failed to fetch client")
      }
    }

    "update view" when {
      "user updates filters" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("showAllClients" -> "allClients")
        updateAuthorisedAgent(controller)(fakeRequest, Some(Mandates(activeMandates = Seq(mandateActive),
          pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))
        ) { result =>
          status(result) must be(OK)
        }
      }

      "user updates filters but there areno mandates" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("showAllClients" -> "allClients")
        updateAuthorisedAgent(controller)(fakeRequest, None) { result =>
          status(result) must be(OK)
        }
      }

      "user submits bad data" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("allClients" -> "client display name")
        updateAuthorisedAgent(controller)(fakeRequest, Some(Mandates(activeMandates = Seq(mandateActive),
          pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)))
        ) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }

}
