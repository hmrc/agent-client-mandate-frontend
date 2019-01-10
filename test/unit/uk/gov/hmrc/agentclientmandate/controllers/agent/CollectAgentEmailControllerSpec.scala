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
import uk.gov.hmrc.agentclientmandate.controllers.agent.CollectAgentEmailController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayName, ClientMandateDisplayDetails}
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


class CollectAgentEmailControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "CollectAgentEmailController" must {

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

    "return 'what is your email address' for AUTHORISED agent who is editing the details of new client to be added" when {

      "agent requests(GET) for 'what is your email address' view pre-populated and the data has been cached" in {
        viewEmailAuthorisedAgent(Some(agentEmail)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("email").`val`() must be("aa@aa.com")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "agents try to edit their email address redirecting to 'what is your email address' view pre-populated and the data has been cached" in {
        editEmailAuthorisedAgent(Some(agentEmail)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("email").`val`() must be("aa@aa.com")
        }
      }
    }

    "return 'what is your email address' for AUTHORISED agent" when {

      "agent requests(GET) for 'what is your email address' view and the data hasn't been cached" in {
        viewEmailAuthorisedAgent(None, Some(ContinueUrl("/api/anywhere"))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("header").text() must include("What email address do you want to use for this client?")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("info").text() must include(s"We will use this email address to send you notifications about this client.")
          document.getElementById("email_field").text() must be("What email address do you want to use for this client?")
          document.getElementById("email").`val`() must be("")
          document.getElementById("submit").text() must be("Continue")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "agents try to edit their email address redirecting to 'what is your email address' view and the data hasn't been cached" in {
        editEmailAuthorisedAgent(None, Some(ContinueUrl("/api/anywhere"))) { result =>
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

      "return url is invalid format" in {
        viewEmailAuthorisedAgent(None, Some(ContinueUrl("http://website.com"))) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }

      "agent requests(GET) for 'what is your email address' view pre-populated and the data has been cached" in {

        val clientMandatDisplay = ClientMandateDisplayDetails("name", "mandateId", "agent@mail.com")
        addClientAuthorisedAgent(Some(clientMandatDisplay)){ result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("email").`val`() must be("agent@mail.com")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientMandateDisplayDetails](Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "agent requests(GET) for 'what is your email address' but the agent email is not pre-populated as it's not cached" in {
        addClientAuthorisedAgent(None) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What email address do you want to use for this client? - GOV.UK")
          document.getElementById("email").`val`() must be("")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientMandateDisplayDetails](Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

    }

    "valid form is submitted with valid email" when {
      "redirect to 'client display name' Page" in {

        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/client-display-name"))
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[AgentEmail](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "redirect to redirect Page if one is supplied" in {

        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true, redirectUrl = Some(ContinueUrl("/api/anywhere"))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/api/anywhere"))
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[AgentEmail](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "return url is invalid format" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = true, redirectUrl = Some(ContinueUrl("http://website.com"))) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "")
        submitEmailAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must include("You must answer the email address question")
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[AgentEmail](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }


      "invalid email id is passed" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aainvalid.com")
        submitEmailAuthorisedAgent(fakeRequest, isValidEmail = false) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must include("This email is invalid")
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[AgentEmail](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }

    "retrieve client display name stored in session" when {
      "return ok" in {
        retrieveAgentEmailFromSessionAuthorisedAgent(Some(AgentEmail("agent@agency.com"))) { result =>
          status(result) must be(OK)
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  val service = "ated".toUpperCase
  val formId1 = "agent-email"
  val agentEmail = AgentEmail("aa@aa.com")
  val agentRefCacheId = "agent-ref-id"

  override def beforeEach(): Unit = {
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }

  def addClientAuthorisedAgent(clientMandateDisplayDetails: Option[ClientMandateDisplayDetails])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientMandateDisplayDetails](Matchers.eq(agentRefCacheId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(clientMandateDisplayDetails))
    val result = TestCollectAgentEmailController.addClient(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewEmailUnAuthenticatedAgent()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestCollectAgentEmailController.view(service, None).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewEmailUnAuthorisedAgent()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestCollectAgentEmailController.view(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewEmailAuthorisedAgent(cachedData: Option[AgentEmail] = None, redirectUrl: Option[ContinueUrl]=None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(formId1))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestCollectAgentEmailController.view(service, redirectUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editEmailAuthorisedAgent(cachedData: Option[AgentEmail] = None, redirectUrl: Option[ContinueUrl]=None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(formId1))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestCollectAgentEmailController.callingPageCacheId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("callingPage")))
    val result = TestCollectAgentEmailController.editFromSummary(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitEmailAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded], isValidEmail: Boolean = false, redirectUrl: Option[ContinueUrl]=None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.cacheFormData[AgentEmail](Matchers.eq(formId1), Matchers.eq(agentEmail))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(agentEmail))
    val result = TestCollectAgentEmailController.submit(service, redirectUrl).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  def retrieveAgentEmailFromSessionAuthorisedAgent(cachedData:  Option[AgentEmail] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestCollectAgentEmailController.getAgentEmail(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  object TestCollectAgentEmailController extends CollectAgentEmailController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
  }

}
