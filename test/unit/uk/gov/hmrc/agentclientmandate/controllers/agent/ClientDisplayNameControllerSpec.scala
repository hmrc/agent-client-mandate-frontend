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
import uk.gov.hmrc.agentclientmandate.controllers.agent.ClientDisplayNameController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientDisplayName
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.RunMode
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ClientDisplayNameControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockControllerSetup {

  "ClientDisplayNameController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'what is your email address' view" in new Setup {
        viewClientDisplayNameUnAuthenticatedAgent() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'what is your email address' view" in new Setup {
        viewClientDisplayNameUnAuthorisedAgent() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return view for AUTHORISED agent" when {

      "agent requests(GET) view and the data hasn't been cached" in new Setup {
        viewClientDisplayNameAuthorisedAgent() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What display name do you want to use for this client? - GOV.UK")
          document.getElementById("header").text() must include("What display name do you want to use for this client?")
        }
      }

      "agent requests(GET) view pre-populated and the data has been cached" in new Setup {
        viewClientDisplayNameAuthorisedAgent(Some(ClientDisplayName("client display name")), Some("/api/anywhere")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What display name do you want to use for this client? - GOV.UK")
          document.getElementById("clientDisplayName").`val`() must be("client display name")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientDisplayName](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "return url is invalid format" in new Setup {
        viewClientDisplayNameAuthorisedAgent(None, Some("http://website.com")) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }

      "agents try to edit client display name but data is not cached" in new Setup {
        editClientDisplayNameAuthorisedAgent() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What display name do you want to use for this client? - GOV.UK")
          document.getElementById("header").text() must include("What display name do you want to use for this client?")
        }
      }

      "agents try to edit client display name view pre-populated and the data has been cached" in new Setup {
        editClientDisplayNameAuthorisedAgent(Some(ClientDisplayName("client display name")), Some("/api/anywhere")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What display name do you want to use for this client? - GOV.UK")
          document.getElementById("clientDisplayName").`val`() must be("client display name")
        }
      }

      "agent tries to client display name but url format is invalied" in new Setup {
        editClientDisplayNameAuthorisedAgent(None, Some("http://website.com")) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "redirect when valid form is submitted with valid data" when {
      "to 'mandate details' when we have no redirectUrl" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "client display name")
        submitClientDisplayNameAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/overseas-client-question"))
          verify(mockDataCacheService, times(1)).cacheFormData[ClientDisplayName](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "to redirectUrl if we have one" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "client display name")
        submitClientDisplayNameAuthorisedAgent(fakeRequest, Some("/api/anywhere")) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/api/anywhere"))
          verify(mockDataCacheService, times(1)).cacheFormData[ClientDisplayName](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "return url is invalid format" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "client display name")
        submitClientDisplayNameAuthorisedAgent(fakeRequest, Some("http://website.com")) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("clientDisplayName" -> "")
        submitClientDisplayNameAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the client display name question")
          document.getElementsByClass("error-notification").text() must include("You must answer the client display name question")
        }
      }

      "clientDisplayName field value is too long" in new Setup {
        val fakeRequest = FakeRequest()
          .withFormUrlEncodedBody("clientDisplayName" -> "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFFGGGGGGGGGGHHHHHHHHHHIIIIIIIIIIJJJJJJJJJJ")
        submitClientDisplayNameAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the client display name question")
          document.getElementsByClass("error-notification").text() must include("A client display name cannot be more than 99 characters")
        }
      }
    }

    "retrieve client display name stored in session" when {
      "return ok" in new Setup {
        retrieveClientDisplayNameFromSessionAuthorisedAgent(Some(ClientDisplayName("client display name"))) { result =>
          status(result) must be(OK)
        }
      }
    }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  val service: String = "ated".toUpperCase
  val clientDisplayName: ClientDisplayName = ClientDisplayName("client display name")



  override def beforeEach(): Unit = {
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }

  class Setup {
    val controller = new ClientDisplayNameController(
      mockDataCacheService,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig
    )

    def viewClientDisplayNameUnAuthenticatedAgent()(test: Future[Result] => Any) {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, None).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewClientDisplayNameUnAuthorisedAgent()(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewClientDisplayNameAuthorisedAgent(cachedData: Option[ClientDisplayName] = None, redirectUrl: Option[String] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(cachedData))
      val result = controller.view(service, redirectUrl).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def editClientDisplayNameAuthorisedAgent(cachedData: Option[ClientDisplayName] = None, redirectUrl: Option[String] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(cachedData))
      when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.eq(controller.callingPageCacheId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("callingPage")))
      val result = controller.editFromSummary(service, redirectUrl).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitClientDisplayNameAuthorisedAgent
    (request: FakeRequest[AnyContentAsFormUrlEncoded], redirectUrl: Option[String] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.cacheFormData[ClientDisplayName]
        (ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(clientDisplayName))
      val result = controller.submit(service, redirectUrl)
        .apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }

    def retrieveClientDisplayNameFromSessionAuthorisedAgent(cachedData: Option[ClientDisplayName] = None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(cachedData))
      val result = controller.getClientDisplayName(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }
}
