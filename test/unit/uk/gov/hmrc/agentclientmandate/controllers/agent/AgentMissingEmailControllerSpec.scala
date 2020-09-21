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

package unit.uk.gov.hmrc.agentclientmandate.controllers.agent

import java.util.UUID

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
import uk.gov.hmrc.agentclientmandate.controllers.agent.AgentMissingEmailController
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmail
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentMissingEmailControllerSpec  extends PlaySpec  with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  "AgentMissingEmailControllerSpec" must {

    "redirect to login page for UNAUTHENTICATED agent" when {
      "agent requests(GET) for 'what is your email address' view" in new Setup {
        viewEmailUnAuthenticatedAgent() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {
      "agent requests(GET) for 'what is your email address' view" in new Setup {
        viewEmailUnAuthorisedAgent() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "view page" when {
      "agent requests(GET) for 'what is your email address' view" in new Setup {
        viewEmailAuthorisedAgent() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("agent.missing-email.title - GOV.UK")
          document.getElementById("header").text() must include("agent.missing-email.header")
          document.getElementById("pre-header").text() must include("ated.screen-reader.section agent.edit-mandate-details.pre-header")
          document.getElementById("info").text() must be("agent.missing-email.text")
          document.getElementById("email_field").text() must be("agent.missing-email.email_address")
          document.getElementById("submit_button").text() must be("continue-button")
          document.getElementById("skip_question").text() must be("agent.missing-email.trapdoor")
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody()
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("agent.enter-email.error.general.useEmailAddress")
          document.getElementsByClass("error-notification").text() must include("agent.missing-email.must_answer")
        }
      }

      "user selected option 'yes' for use email address and left email as empty" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("useEmailAddress" -> "true","email" -> "")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("agent.enter-email.error.general.email")
          document.getElementsByClass("error-notification").text() must include("client.email.error.email.empty")
        }
      }

      "email field has more than expected length" in new Setup {
        val tooLongEmail: String = "aaa@" + "a" * 237 + ".com"
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("useEmailAddress" -> "true", "email" -> tooLongEmail)
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = false) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("agent.enter-email.error.general.email")
          document.getElementsByClass("error-notification").text() must include("client.email.error.email.too.long")
        }
      }

      "invalid email is passed" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("useEmailAddress" -> "true","email" -> "testtest.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = false) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("agent.enter-email.error.general.email")
          document.getElementsByClass("error-notification").text() must include("client.email.error.email.invalid")
        }
      }
    }

    "returns OK and redirects" when {
      "valid form is submitted" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@invalid.com", "useEmailAddress" -> "true")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("summary")
          verify(mockAgentClientMandateService, times(1)).updateAgentMissingEmail(ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]


  val service: String = "ated".toUpperCase
  val agentEmail: AgentEmail = AgentEmail("aa@aa.com")
  val injectedViewInstanceAgentMissingEmail = app.injector.instanceOf[views.html.agent.agentMissingEmail]

  override def beforeEach(): Unit = {
    reset(mockAgentClientMandateService)
    reset(mockAuthConnector)
  }

  class Setup {
    val controller = new AgentMissingEmailController(
      mockAgentClientMandateService,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig,
      injectedViewInstanceAgentMissingEmail
    )

    def viewEmailUnAuthenticatedAgent()(test: Future[Result] => Any) {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewEmailUnAuthorisedAgent()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewEmailAuthorisedAgent()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitEmailAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded], isValidEmail: Boolean)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }


}
