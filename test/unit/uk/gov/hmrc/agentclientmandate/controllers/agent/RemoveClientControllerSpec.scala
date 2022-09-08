/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.RemoveClientController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.{removeClient, removeClientConfirmation}
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveClientControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val service: String = "ATED"
  val mandateId: String = "1"
  val agentName: String = "Acme"
  val injectedViewInstanceRemoveClient: removeClient = app.injector.instanceOf[views.html.agent.removeClient]
  val injectedViewInstanceRemoveClientConfirmation: removeClientConfirmation = app.injector.instanceOf[views.html.agent.removeClientConfirmation]

  val mandate: Mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
    currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil,
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "ACME Limited")

  class Setup {
    val controller = new RemoveClientController(
      stubbedMessagesControllerComponents,
      mockAgentClientMandateService,
      implicitly,
      mockAppConfig,
      mockAuthConnector,
      injectedViewInstanceRemoveClient,
      injectedViewInstanceRemoveClientConfirmation
    )

    def viewWithAuthorisedAgent(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.view(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithUnAuthenticatedAgent(test: Future[Result] => Any): Unit = {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, "1").apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewWithUnAuthorisedAgent(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def showConfirmationWithAuthorisedAgent(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.showConfirmation(service, "Acme Ltd").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.confirm(service, "1").apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }

  override def beforeEach: Unit = {
    reset(mockAgentClientMandateService)
  }

  "redirect to login page for UNAUTHENTICATED agent" when {

    "agent requests(GET) for 'overseas client question' view" in new Setup {
      viewWithUnAuthenticatedAgent { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }
  }

  "redirect to unauthorised page for UNAUTHORISED agent" when {

    "agent requests(GET) for 'overseas client question' view" in new Setup {
      viewWithUnAuthorisedAgent { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/gg/sign-in")
      }
    }
  }

  "return 'remove client question' view for AUTHORISED agent" when {

    "agent requests(GET) for 'remove client question' view" in new Setup {

      when(mockAgentClientMandateService.fetchClientMandateClientName(ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(mandate))

      viewWithAuthorisedAgent { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title() must be("agent.remove-client.header - GOV.UK")
        document.getElementById("pre-header").text() must include("ated.screen-reader.section agent.edit-mandate-details.pre-header")
        document.getElementById("header").text() must include("agent.remove-client.header")
        document.getElementsByClass("govuk-fieldset__legend").text() must be("agent.remove-client.header")
        document.getElementById("submit").text() must be("confirm-button")
      }
    }
  }

  "returns BAD_REQUEST" when {
    "invalid form is submitted" in new Setup {
      when(mockAgentClientMandateService.fetchClientMandateClientName(ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(mandate))

      val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("yesNo" -> "")
      submitWithAuthorisedAgent(fakeRequest) { result =>
        status(result) must be(BAD_REQUEST)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("error-list").text() must include("yes-no.error.general.yesNo")
        document.getElementsByClass("govuk-error-message").text() must include("yes-no.error.mandatory.removeClient")
      }
    }
  }

  "submitting form " when {
    "submitted with false will redirect to agent summary" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yesNo" -> "false")
      when(mockAgentClientMandateService.fetchClientMandateClientName(ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(mandate))
      submitWithAuthorisedAgent(fakeRequest) { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include(s"/agent/summary")
      }
    }

    "submitted with true will redirect to confirmation" in new Setup {
      when(mockAgentClientMandateService.removeClient(ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(true)
      val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yesNo" -> "true")
      when(mockAgentClientMandateService.fetchClientMandateClientName(ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(mandate))
      submitWithAuthorisedAgent(fakeRequest) { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/agent/remove-client/showConfirmation")
      }
    }

    "submitted with true throws exception" in new Setup {
      when(mockAgentClientMandateService.removeClient(ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(false)
      val userId = s"user-${UUID.randomUUID}"

      val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yesNo" -> "true")
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

      when(mockAgentClientMandateService.fetchClientMandateClientName(ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(mandate))

      val thrown: RuntimeException = the[RuntimeException] thrownBy await(controller.confirm(service, "ABC123")
        .apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId)))

      thrown.getMessage must include("Client removal Failed")
    }
  }

  "return 'client remove confirmation' view for AUTHORISED agent" when {

    "agent requests(GET) for 'client remove confirmation' view" in new Setup {

      when(mockAgentClientMandateService.fetchClientMandateClientName(ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(mandate))

      showConfirmationWithAuthorisedAgent { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title() must be("agent.remove-client-confirmation.title - GOV.UK")
      }
    }
  }

}
