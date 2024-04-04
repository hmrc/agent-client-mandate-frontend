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
import uk.gov.hmrc.agentclientmandate.controllers.client.ChangeAgentController
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.client.changeAgent
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ChangeAgentControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val injectedViewInstanceChangeAgent: changeAgent = app.injector.instanceOf[views.html.client.changeAgent]

  class Setup {
    val controller = new ChangeAgentController(
      mockAgentClientMandateService,
      mockDataCacheService,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig,
      injectedViewInstanceChangeAgent
    )

    def viewUnAuthenticatedClient(test: Future[Result] => Any): Unit = {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, mandateId).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewUnAuthorisedClient(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)

      val result = controller.view(service, mandateId).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewAuthorisedClient(request: FakeRequest[AnyContentAsJson], test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"


      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
      val result = controller.view(service, mandateId).apply(SessionBuilder.updateRequestWithSession(request, userId))
      test(result)
    }

    def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"


      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)

      val result = controller.submit(service, mandateId).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAgentClientMandateService)
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  val mandateId: String = "1"
  val service: String = "ATED"

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

        val request: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.toJson("""{}"""))
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        viewAuthorisedClient(request, { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.change-agent.title - service.name - GOV.UK")
          document.getElementsByTag("header").text() must include("client.change-agent.header")
          document.getElementsByTag("header").text() must include("ated.screen-reader.section agent.edit-mandate-details.pre-header")
          document.getElementsByClass("govuk-fieldset__legend").text() must be("client.change-agent.header")
          document.getElementById("submit").text() must be("confirm-button")
        })
      }
    }

    "submitting form" when {
      "invalid form is submitted" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("yesNo" -> "")
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-error-summary__body").text() mustBe "yes-no.error.mandatory.changeAgent"
          document.getElementsByClass("govuk-error-message").text() mustBe "govukErrorMessage.visuallyHiddenText: yes-no.error.mandatory.changeAgent"
        }
      }

      "submitted with true will redirect to collect agent email" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yesNo" -> "true")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/client/email")
        }
      }

      "submitted with false will redirect to remove agent confirmation" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yesNo" -> "false")
        when(mockAgentClientMandateService.fetchClientMandateAgentName(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/client/remove/1/confirmation")
        }
      }
    }
  }

}
