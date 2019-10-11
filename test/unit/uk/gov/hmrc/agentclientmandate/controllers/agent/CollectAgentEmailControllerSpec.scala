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
import uk.gov.hmrc.agentclientmandate.controllers.agent.CollectAgentEmailController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientMandateDisplayDetails}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.RunMode
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CollectAgentEmailControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockControllerSetup {

  "CollectAgentEmailController" must {

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

    "return 'what is your email address' for AUTHORISED agent who is editing the details of new client to be added" when {

      "agent requests(GET) for 'what is your email address' view pre-populated and the data has been cached" in new Setup {
        viewEmailAuthorisedAgent(Some(agentEmail)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("email").`val`() must be("aa@aa.com")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[AgentEmail](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "agents try to edit their email address redirecting to 'what is your email address' view pre-populated and the data has been cached" in new Setup {
        editEmailAuthorisedAgent(Some(agentEmail)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("email").`val`() must be("aa@aa.com")
        }
      }
    }

    "return 'what is your email address' for AUTHORISED agent" when {

      "agent requests(GET) for 'what is your email address' view and the data hasn't been cached" in new Setup {
        viewEmailAuthorisedAgent(None, Some("/api/anywhere")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("header").text() must include("What email address do you want to use for this client?")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("info").text() must include(s"We will use this email address to send you notifications about this client.")
          document.getElementById("email_field").text() must be("What email address do you want to use for this client?")
          document.getElementById("email").`val`() must be("")
          document.getElementById("submit").text() must be("Continue")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[AgentEmail](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "agents try to edit their email address redirecting to 'what is your email address' view and the data hasn't been cached" in new Setup {
        editEmailAuthorisedAgent(None, Some("/api/anywhere")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("header").text() must include("What email address do you want to use for this client?")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("info").text() must include(s"We will use this email address to send you notifications about this client.")
          document.getElementById("email_field").text() must be("What email address do you want to use for this client?")
          document.getElementById("email").`val`() must be("")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "return url is invalid format" in new Setup {
        viewEmailAuthorisedAgent(None, Some("http://website.com")) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }

      "agent requests(GET) for 'what is your email address' view pre-populated and the data has been cached" in new Setup {

        val clientMandatDisplay = ClientMandateDisplayDetails("name", "mandateId", "agent@mail.com")
        addClientAuthorisedAgent(Some(clientMandatDisplay)){ result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("email").`val`() must be("agent@mail.com")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientMandateDisplayDetails](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "agent requests(GET) for 'what is your email address' but the agent email is not pre-populated as it's not cached" in new Setup {
        addClientAuthorisedAgent(None) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("email").`val`() must be("")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientMandateDisplayDetails](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

    }

    "valid form is submitted with valid email" when {
      "redirect to 'client display name' Page" in new Setup {

        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/client-display-name"))
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[AgentEmail](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "redirect to redirect Page if one is supplied" in new Setup {

        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true, redirectUrl = Some("/api/anywhere")) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/api/anywhere"))
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[AgentEmail](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "return url is invalid format" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true, redirectUrl = Some("http://website.com")) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "")
        submitEmailAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must include("Enter the email address you want to use for this client")
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[AgentEmail](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }


      "invalid email id is passed" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aainvalid.com")
        submitEmailAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must include("Enter an email address in the correct format, like name@example.com")
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[AgentEmail](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "email provided is too long" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aaa@aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.com")
        submitEmailAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must
            include("The email address you want to use for this client must be 241 characters or less")
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[AgentEmail](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }

    "retrieve client display name stored in session" when {
      "return ok" in new Setup {
        retrieveAgentEmailFromSessionAuthorisedAgent(Some(AgentEmail("agent@agency.com"))) { result =>
          status(result) must be(OK)
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  val service: String = "ated".toUpperCase
  val formId1: String = "agent-email"
  val agentEmail: AgentEmail = AgentEmail("aa@aa.com")
  val agentRefCacheId: String = "agent-ref-id"

  override def beforeEach(): Unit = {
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }



  class Setup {
    val controller = new CollectAgentEmailController(
      app.injector.instanceOf[MessagesControllerComponents],
      mockAuthConnector,
      mockDataCacheService,
      implicitly,
      mockAppConfig
    )

    def addClientAuthorisedAgent(clientMandateDisplayDetails: Option[ClientMandateDisplayDetails])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[ClientMandateDisplayDetails](ArgumentMatchers.eq(agentRefCacheId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(clientMandateDisplayDetails))
      val result = controller.addClient(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewEmailUnAuthenticatedAgent()(test: Future[Result] => Any) {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, None).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewEmailUnAuthorisedAgent()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewEmailAuthorisedAgent(cachedData: Option[AgentEmail] = None, redirectUrl: Option[String]=None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[AgentEmail](ArgumentMatchers.eq(formId1))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(cachedData))
      val result = controller.view(service, redirectUrl).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def editEmailAuthorisedAgent(cachedData: Option[AgentEmail] = None, redirectUrl: Option[String]=None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[AgentEmail](ArgumentMatchers.eq(formId1))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(cachedData))
      when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.eq(controller.callingPageCacheId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("callingPage")))
      val result = controller.editFromSummary(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitEmailAuthorisedAgent
    (request: FakeRequest[AnyContentAsFormUrlEncoded], isValidEmail: Boolean = false, redirectUrl: Option[String]=None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.cacheFormData[AgentEmail](ArgumentMatchers.eq(formId1), ArgumentMatchers.eq(agentEmail))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(agentEmail))
      val result = controller.submit(service, redirectUrl).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }

    def retrieveAgentEmailFromSessionAuthorisedAgent(cachedData:  Option[AgentEmail] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[AgentEmail](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(cachedData))
      val result = controller.getAgentEmail(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

}
