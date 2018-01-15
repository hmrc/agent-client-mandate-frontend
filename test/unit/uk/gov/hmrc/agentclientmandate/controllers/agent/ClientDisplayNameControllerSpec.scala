/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.ClientDisplayNameController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientDisplayName
import uk.gov.hmrc.play.binders.ContinueUrl
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ClientDisplayNameControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "ClientDisplayNameController" must {

    "not return NOT_FOUND at route " when {

      "GET /mandate/agent/client-display-name/:service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/client-display-name")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "GET /mandate/agent/client-display-name/:service?redirectUrl=http://" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/client-display-name?redirectUrl=http://")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "POST /mandate/agent/client-display-name/:service" in {
        val result = route(FakeRequest(POST, s"/mandate/agent/client-display-name")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'what is your email address' view" in {
        viewClientDisplayNameUnAuthenticatedAgent() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'what is your email address' view" in {
        viewClientDisplayNameUnAuthorisedAgent() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return view for AUTHORISED agent" when {

      "agent requests(GET) view and the data hasn't been cached" in {
        viewClientDisplayNameAuthorisedAgent() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What display name do you want to use for this client? - GOV.UK")
          document.getElementById("header").text() must include("What display name do you want to use for this client?")
        }
      }

      "agent requests(GET) view pre-populated and the data has been cached" in {
        viewClientDisplayNameAuthorisedAgent(Some(ClientDisplayName("client display name")), Some(ContinueUrl("/api/anywhere"))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What display name do you want to use for this client? - GOV.UK")
          document.getElementById("clientDisplayName").`val`() must be("client display name")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientDisplayName](Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "return url is invalid format" in {
        viewClientDisplayNameAuthorisedAgent(None, Some(ContinueUrl("http://website.com"))) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }

      "agents try to edit client display name but data is not cached" in {
        editClientDisplayNameAuthorisedAgent() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What display name do you want to use for this client? - GOV.UK")
          document.getElementById("header").text() must include("What display name do you want to use for this client?")
        }
      }

      "agents try to edit client display name view pre-populated and the data has been cached" in {
        editClientDisplayNameAuthorisedAgent(Some(ClientDisplayName("client display name")), Some(ContinueUrl("/api/anywhere"))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What display name do you want to use for this client? - GOV.UK")
          document.getElementById("clientDisplayName").`val`() must be("client display name")
        }
      }

      "agent tries to client display name but url format is invalied" in {
        editClientDisplayNameAuthorisedAgent(None, Some(ContinueUrl("http://website.com"))) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "redirect when valid form is submitted with valid data" when {
      "to 'mandate details' when we have no redirectUrl" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "client display name")
        submitClientDisplayNameAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/overseas-client-question"))
          verify(mockDataCacheService, times(1)).cacheFormData[ClientDisplayName](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "to redirectUrl if we have one" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "client display name")
        submitClientDisplayNameAuthorisedAgent(fakeRequest, Some(ContinueUrl("/api/anywhere"))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/api/anywhere"))
          verify(mockDataCacheService, times(1)).cacheFormData[ClientDisplayName](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "return url is invalid format" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "client display name")
        submitClientDisplayNameAuthorisedAgent(fakeRequest, Some(ContinueUrl("http://website.com"))) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "")
        submitClientDisplayNameAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the client display name question")
          document.getElementsByClass("error-notification").text() must include("You must answer the client display name question")
        }
      }

      "clientDisplayName field value is too long" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFFGGGGGGGGGGHHHHHHHHHHIIIIIIIIIIJJJJJJJJJJ")
        submitClientDisplayNameAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the client display name question")
          document.getElementsByClass("error-notification").text() must include("A client display name cannot be more than 99 characters")
        }
      }
    }

    "retrieve client display name stored in session" when {
      "return ok" in {
        retrieveClientDisplayNameFromSessionAuthorisedAgent(Some(ClientDisplayName("client display name"))) { result =>
          status(result) must be(OK)
        }
      }
    }
  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  val service = "ated".toUpperCase
  val clientDisplayName = ClientDisplayName("client display name")

  override def beforeEach(): Unit = {
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }

  def viewClientDisplayNameUnAuthenticatedAgent()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestClientDisplayNameController.view(service, None).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewClientDisplayNameUnAuthorisedAgent()(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestClientDisplayNameController.view(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewClientDisplayNameAuthorisedAgent(cachedData:  Option[ClientDisplayName] = None, redirectUrl: Option[ContinueUrl] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestClientDisplayNameController.view(service, redirectUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editClientDisplayNameAuthorisedAgent(cachedData:  Option[ClientDisplayName] = None, redirectUrl: Option[ContinueUrl] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestClientDisplayNameController.callingPageCacheId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("callingPage")))
    val result = TestClientDisplayNameController.editFromSummary(service, redirectUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def submitClientDisplayNameAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded], redirectUrl: Option[ContinueUrl] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.cacheFormData[ClientDisplayName](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(clientDisplayName))
    val result = TestClientDisplayNameController.submit(service, redirectUrl).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  def retrieveClientDisplayNameFromSessionAuthorisedAgent(cachedData:  Option[ClientDisplayName] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestClientDisplayNameController.getClientDisplayName(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  object TestClientDisplayNameController extends ClientDisplayNameController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
  }
}
