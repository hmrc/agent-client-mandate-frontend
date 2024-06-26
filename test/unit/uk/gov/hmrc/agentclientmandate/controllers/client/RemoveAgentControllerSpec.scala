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

package unit.uk.gov.hmrc.agentclientmandate.controllers.client

import java.util.UUID
import java.time.Instant
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.DelegationConnector
import uk.gov.hmrc.agentclientmandate.controllers.client.RemoveAgentController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.client.{removeAgent, removeAgentConfirmation}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveAgentControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDelegationConnector: DelegationConnector = mock[DelegationConnector]

  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val injectedViewInstanceRemoveAgent: removeAgent = app.injector.instanceOf[views.html.client.removeAgent]
  val injectedViewInstanceRemoveAgentConfirmation: removeAgentConfirmation = app.injector.instanceOf[views.html.client.removeAgentConfirmation]

  class Setup {
    val controller = new RemoveAgentController(
      mockAgentClientMandateService,
      mockDataCacheService,
      mockDelegationConnector,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig,
      injectedViewInstanceRemoveAgent,
      injectedViewInstanceRemoveAgentConfirmation
    )
  }

  override def beforeEach(): Unit = {
    reset(mockAgentClientMandateService)
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mandate: Mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
    currentStatus = MandateStatus(Status.New, Instant.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name")

  val service: String = "ATED"

  def viewUnAuthenticatedClient(controller: RemoveAgentController)(test: Future[Result] => Any): Unit = {

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view(service, "1", RedirectUrl("/api/anywhere")).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewUnAuthorisedClient(controller: RemoveAgentController)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view(service, "1", RedirectUrl("/api/anywhere")).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewAuthorisedClient(controller: RemoveAgentController)(request: FakeRequest[AnyContentAsJson], continueUrl: String)
                          (test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    val result = controller.view(service, "1", RedirectUrl(continueUrl)).apply(SessionBuilder.updateRequestWithSession(request, userId))
    test(result)
  }

  def submitWithAuthorisedClient(controller: RemoveAgentController)(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)

    val result = controller.submit(service, "1").apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  def returnToServiceWithAuthorisedClient(controller: RemoveAgentController)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    val result = controller.returnToService().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithAuthorisedClient(controller: RemoveAgentController)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    val result = controller.confirmation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  "RemoveAgentController" must {

    "redirect to login page for UNAUTHENTICATED client" when {
      "client requests(GET) for agent removal view" in new Setup {
        viewUnAuthenticatedClient(controller) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED client" when {
      "client requests(GET) for agent removal view" in new Setup {
        viewUnAuthorisedClient(controller) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return 'remove agent question' view for AUTHORISED agent" when {
      "client requests(GET) for 'remove agent question' view" in new Setup {

        when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(Some(mandate))
        when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("AS12345678"))
        val request: FakeRequest[AnyContentAsJson] = FakeRequest(GET, "/client/remove-agent/1?returnUrl=/app/return").withJsonBody(Json.toJson("""{}"""))
        viewAuthorisedClient(controller)(request, "/app/return") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.remove-agent.title - service.name - GOV.UK")
          document.getElementsByTag("header").text() must include("client.remove-agent.header")
          document.getElementsByTag("header").text() must include("ated.screen-reader.section agent.edit-mandate-details.pre-header")
          document.getElementsByClass("govuk-fieldset__legend").text() must be("client.remove-agent.header")
          document.getElementById("submit").text() must be("confirm-button")
        }
      }

      "can't find mandate, throw exception" in new Setup {
        when(mockAgentClientMandateService.fetchClientMandate(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(None)
        when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("AS12345678"))
        val userId = s"user-${UUID.randomUUID}"
        AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
        val request: FakeRequest[AnyContentAsJson] = FakeRequest(GET, "/client/remove-agent/1?returnUrl=/app/return").withJsonBody(Json.toJson("""{}"""))
        val thrown: RuntimeException = the[RuntimeException] thrownBy await(controller.view(service, "1", RedirectUrl("/api/anywhere"))
          .apply(SessionBuilder.updateRequestWithSession(request, userId)))

        thrown.getMessage must be("No Mandate returned")
      }
    }

    "submitting form" when {
      "invalid form is submitted" in new Setup {
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some("/api/anywhere")))

        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("yesNo" -> "")
        submitWithAuthorisedClient(controller)(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-error-summary__body").text() mustBe "yes-no.error.mandatory.removeAgent"
          document.getElementsByClass("govuk-error-message").text() mustBe "govukErrorMessage.visuallyHiddenText: yes-no.error.mandatory.removeAgent"
        }
      }

      "submitted with true will redirect to change agent" in new Setup {
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        when(mockAgentClientMandateService.removeAgent(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(true)
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yesNo" -> "true")
        submitWithAuthorisedClient(controller)(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/client/change")
        }
      }

      "submitted with true but agent removal fails" in new Setup {

        AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        when(mockAgentClientMandateService.removeAgent(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(false)
        val userId = s"user-${UUID.randomUUID}"
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yesNo" -> "true")
        val thrown: RuntimeException = the[RuntimeException] thrownBy await(controller.submit(service, "1")
          .apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId)))

        thrown.getMessage must be("Agent Removal Failed")
      }

      "submitted with false will redirect to cached return url" in new Setup {
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some("/api/anywhere")))
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yesNo" -> "false")
        submitWithAuthorisedClient(controller)(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must be("/api/anywhere")
        }
      }

      "submitted with false but retrieval of returnUrl from cache fails" in new Setup {
        AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())(
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        val userId = s"user-${UUID.randomUUID}"

        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yesNo" -> "false")
        val thrown: RuntimeException = the[RuntimeException] thrownBy await(controller.submit(service, "1")
          .apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId)))

        thrown.getMessage must be("Cache Retrieval Failed with id 1")
      }
    }

    "returnToService" when {
      "redirects to cached service" in new Setup {
        when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(Some("/api/anywhere"))
        }
        returnToServiceWithAuthorisedClient(controller) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must be("/api/anywhere")
        }
      }

      "fails when cache fails" in new Setup {
        when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())(
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        val userId = s"user-${UUID.randomUUID}"
        AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
        val thrown: RuntimeException = the[RuntimeException] thrownBy await(controller.returnToService()
          .apply(SessionBuilder.buildRequestWithSession(userId)))

        thrown.getMessage must be("Cache Retrieval Failed")
      }
    }

    "showConfirmation" when {
      "agent has been removed show confirmation page" in new Setup {
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))

        confirmationWithAuthorisedClient(controller) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.remove-agent-confirmation.title - service.name - GOV.UK")
          document.getElementById("banner").text() must include("client.remove-agent-confirmation.banner-text")
          document.getElementById("notification").text() must be("client.agent-confirmation.notification")
          document.getElementById("heading-1").text() must be("client.remove-agent-confirmation.header")
          document.getElementById("return_to_service_button").text() must be("client.remove-agent-confirmation.service_button")
        }
      }
    }
  }

}
