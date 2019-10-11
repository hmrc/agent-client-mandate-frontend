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

package unit.uk.gov.hmrc.agentclientmandate.controllers.client

import java.util.UUID

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.client.ChangeAgentController
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ChangeAgentControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockControllerSetup {

  "ChangeAgentController" must {

    "redirect to login page for UNAUTHENTICATED client" when {
      "client requests(GET) for agent removal view" in new Setup {
        viewUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED client" when {
      "client requests(GET) for agent removal view" in new Setup {
        viewUnAuthorisedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return 'change agent question' view for AUTHORISED agent" when {
      "client requests(GET) for 'change agent question' view" in new Setup {

        val request = FakeRequest().withJsonBody(Json.toJson("""{}"""))
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        viewAuthorisedClient(request, { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Do you want to appoint another agent to act for you? - GOV.UK")
          document.getElementById("header").text() must include("Do you want to appoint another agent to act for you?")
          document.getElementById("pre-heading").text() must be("This section is: Manage your ATED service")
          document.getElementById("yesNo_legend").text() must be("Do you want to appoint another agent to act for you?")
          document.getElementById("submit").text() must be("Confirm")
        })
      }
    }

    "submitting form" when {
      "invalid form is submitted" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "")
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with change agent question")
          document.getElementsByClass("error-notification").text() must include("The change agent question must be answered")
        }
      }

      "submitted with true will redirect to collect agent email" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "true")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/client/email")
        }
      }

      "submitted with false will redirect to remove agent confirmation" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "false")
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/client/remove/1/confirmation")
        }
      }
    }
  }

  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  class Setup {
    val controller = new ChangeAgentController(
      mockAgentClientMandateService,
      mockDataCacheService,
      app.injector.instanceOf[MessagesControllerComponents],
      mockAuthConnector,
      implicitly,
      mockAppConfig
    )

    def viewUnAuthenticatedClient(test: Future[Result] => Any) {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, mandateId).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }


    def viewUnAuthorisedClient(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)

      val result = controller.view(service, mandateId).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewAuthorisedClient(request: FakeRequest[AnyContentAsJson], test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()

      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
      val result = controller.view(service, mandateId).apply(SessionBuilder.updateRequestWithSession(request, userId))
      test(result)
    }

    def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()

      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)

      val result = controller.submit(service, mandateId).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }

  override def beforeEach: Unit = {
    reset(mockAgentClientMandateService)
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  val mandateId: String = "1"
  val service: String = "ATED"




}
