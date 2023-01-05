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
import uk.gov.hmrc.agentclientmandate.controllers.agent.SelectServiceController
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.utils.{FeatureSwitch, MandateFeatureSwitches}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.selectService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SelectServiceControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  implicit val mockConfiguration: ServicesConfig = mock[ServicesConfig]

  "SelectServiceController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'select service question' view" in new Setup {
        viewWithUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'select service question' view" in new Setup {
        viewWithUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return 'select service question' view for AUTHORISED agent" when {
      "agent requests(GET) for 'select service question' view and single service feature is disabled" in new Setup {
        FeatureSwitch.disable(MandateFeatureSwitches.singleService)
        viewWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("agent.select-service.title - GOV.UK")
          document.getElementsByTag("header").text() must include("agent.select-service.header")
          document.getElementsByTag("header").text() must include("ated.screen-reader.section agent.add-a-client.sub-header")
          document.getElementsByClass("govuk-fieldset__legend").text() must be("agent.select-service.header")
          document.getElementById("submit").text() must be("submit-button")
        }
      }

    }

    "agent requests(GET) for 'select service question' view and single service feature is enabled" when {
      "redirect to 'summary page for ated' view for AUTHORISED agent" in new Setup {
        when(mockAgentClientMandateService.doesAgentHaveMissingEmail(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn (Future.successful(false))
        viewWithAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/summary"))
        }
      }

      "redirect to 'missing email' view for AUTHORISED agent" in new Setup {
        when(mockAgentClientMandateService.doesAgentHaveMissingEmail(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn (Future.successful(true))
        viewWithAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/missing-email"))
        }
      }
    }

    "valid form is submitted" when {
      "redirect to 'agent summary page for service' Page" in new Setup {
        when(mockAgentClientMandateService.doesAgentHaveMissingEmail(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn (Future.successful(false))
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("service" -> "ated")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/summary"))
        }
      }

      "redirect to 'missing email' Page" in new Setup {
        when(mockAgentClientMandateService.doesAgentHaveMissingEmail(ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn (Future.successful(true))
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("service" -> "ated")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/missing-email"))
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("service" -> "")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("agent.select-service.error.service")
          document.getElementsByClass("govuk-error-message").text() must include("agent.select-service.error.service")
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val injectedViewInstanceSelectServices: selectService = app.injector.instanceOf[views.html.agent.selectService]

  class Setup {
    val controller = new SelectServiceController(
      stubbedMessagesControllerComponents,
      mockAgentClientMandateService,
      implicitly,
      mockAppConfig,
      mockAuthConnector,
      injectedViewInstanceSelectServices
    )

    def viewWithUnAuthenticatedAgent(test: Future[Result] => Any): Unit = {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view().apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewWithUnAuthorisedAgent(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedAgent(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.view().apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.submit().apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockAgentClientMandateService)
    FeatureSwitch.enable(MandateFeatureSwitches.singleService)
  }

}
