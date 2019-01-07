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
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.{AgentMissingEmailController, CollectAgentEmailController}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmail
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class AgentMissingEmailControllerSpec  extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "AgentMissingEmailControllerSpec" must {
    "not return NOT_FOUND at route " when {

      "GET /mandate/agent/email/:service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/email")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "POST /mandate/agent/email/:service" in {
        val result = route(FakeRequest(POST, s"/mandate/agent/email")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED agent" when {
      "agent requests(GET) for 'what is your email address' view" in {
        viewEmailUnAuthenticatedAgent() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {
      "agent requests(GET) for 'what is your email address' view" in {
        viewEmailUnAuthorisedAgent() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "view page" when {
      "agent requests(GET) for 'what is your email address' view" in {
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
      "empty form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "")
        submitEmailAuthorisedAgent(fakeRequest, true) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the question")
          document.getElementsByClass("error-notification").text() must include("You must answer the question")
          verify(mockEmailService, times(0)).validate(Matchers.any())(Matchers.any())
        }
      }


      "invalid email is passed" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aainvalid.com", "useEmailAddress" -> "true")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = false) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
        }
      }
    }

    "returns OK and redirects" when {
      "valid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@invalid.com", "useEmailAddress" -> "true")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("summary")
          verify(mockAgentClientMandateService, times(1)).updateAgentMissingEmail(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }


  }

  val mockAuthConnector = mock[AuthConnector]
  val mockEmailService: EmailService = mock[EmailService]
  val mockAgentClientMandateService = mock[AgentClientMandateService]

  val service = "ated".toUpperCase
  val agentEmail = AgentEmail("aa@aa.com")

  override def beforeEach(): Unit = {
    reset(mockAgentClientMandateService)
    reset(mockEmailService)
    reset(mockAuthConnector)
  }

  object TestAgentMissingEmailController extends AgentMissingEmailController {
    override val authConnector = mockAuthConnector
    override val agentClientMandateService = mockAgentClientMandateService
  }

  def viewEmailUnAuthenticatedAgent()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestAgentMissingEmailController.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewEmailUnAuthorisedAgent()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestAgentMissingEmailController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewEmailAuthorisedAgent()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestAgentMissingEmailController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitEmailAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded], isValidEmail: Boolean)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockEmailService.validate(Matchers.any())(Matchers.any())).thenReturn(Future.successful(isValidEmail))
    val result = TestAgentMissingEmailController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }
}
