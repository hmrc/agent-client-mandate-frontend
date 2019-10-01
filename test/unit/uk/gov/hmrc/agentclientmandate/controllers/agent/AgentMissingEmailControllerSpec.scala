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
import uk.gov.hmrc.agentclientmandate.controllers.agent.AgentMissingEmailController
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmail
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.RunMode
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AgentMissingEmailControllerSpec  extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockControllerSetup {

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
          document.title() must be("Receive email notifications from your clients - GOV.UK")
          document.getElementById("header").text() must include("Receive email notifications from your clients")
          document.getElementById("pre-header").text() must include("Manage your ATED service")
          document.getElementById("info").text() must be(s"We can send you a notification when a client accepts or rejects your requests in the $service service. You can use a group email address and change it later.")
          document.getElementById("email_field").text() must be("Your email address")
          document.getElementById("submit_button").text() must be("Continue")
          document.getElementById("skip_question").text() must be("Enter my email at a later date")
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody()
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the question")
          document.getElementsByClass("error-notification").text() must include("You must answer the question")
        }
      }

      " user selected option 'yes' for use email address and left email as empty" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("useEmailAddress" -> "true","email" -> "")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must include("Enter the email address you want to use for this client")
        }
      }


      "email field has more than expected length" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("useEmailAddress" -> "true","email" -> "aaa@aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = false) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must include("The email address you want to use for this client must be 241 characters or less")
        }
      }
      "invalid email is passed" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("useEmailAddress" -> "true","email" -> "testtest.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = false) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must include("Enter an email address in the correct format, like name@example.com")
        }
      }
    }

    "returns OK and redirects" when {
      "valid form is submitted" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@invalid.com", "useEmailAddress" -> "true")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("summary")
          verify(mockAgentClientMandateService, times(1)).updateAgentMissingEmail(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())
        }
      }
    }


  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]


  val service: String = "ated".toUpperCase
  val agentEmail: AgentEmail = AgentEmail("aa@aa.com")

  override def beforeEach(): Unit = {
    reset(mockAgentClientMandateService)
    reset(mockAuthConnector)
  }

  class Setup {
    val controller = new AgentMissingEmailController(
      mockAgentClientMandateService,
      app.injector.instanceOf[MessagesControllerComponents],
      mockAuthConnector,
      implicitly,
      mockAppConfig
    )

    def viewEmailUnAuthenticatedAgent()(test: Future[Result] => Any) {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewEmailUnAuthorisedAgent()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewEmailAuthorisedAgent()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitEmailAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded], isValidEmail: Boolean)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }


}
