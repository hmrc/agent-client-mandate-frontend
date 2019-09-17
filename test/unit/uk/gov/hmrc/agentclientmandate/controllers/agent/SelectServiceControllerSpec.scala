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
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.SelectServiceController
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.utils.{FeatureSwitch, MandateFeatureSwitches}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, SessionBuilder}

import scala.concurrent.Future

class SelectServiceControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "SelectServiceController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'select service question' view" in {
        viewWithUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'select service question' view" in {
        viewWithUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return 'select service question' view for AUTHORISED agent" when {
      "agent requests(GET) for 'select service question' view and single service feature is disabled" in {
        FeatureSwitch.disable(MandateFeatureSwitches.singleService)
        viewWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Select a service - GOV.UK")
          document.getElementById("header").text() must include("Select a service")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("service_legend").text() must be("Select a service")
          document.getElementById("submit").text() must be("Submit")
        }
      }

    }

    "agent requests(GET) for 'select service question' view and single service feature is enabled" when {
      "redirect to 'summary page for ated' view for AUTHORISED agent" in {
        when(mockAgentClientMandateService.doesAgentHaveMissingEmail(Matchers.any(), Matchers.any())(Matchers.any()))
          .thenReturn (Future.successful(false))
        viewWithAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/summary"))
        }
      }

      "redirect to 'missing email' view for AUTHORISED agent" in {
        when(mockAgentClientMandateService.doesAgentHaveMissingEmail(Matchers.any(), Matchers.any())(Matchers.any()))
          .thenReturn (Future.successful(true))
        viewWithAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/missing-email"))
        }
      }
    }

    "valid form is submitted" when {
      "redirect to 'agent summary page for service' Page" in {
        when(mockAgentClientMandateService.doesAgentHaveMissingEmail(Matchers.any(), Matchers.any())(Matchers.any()))
          .thenReturn (Future.successful(false))
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("service" -> "ated")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/summary"))
        }
      }

      "redirect to 'missing email' Page" in {
        when(mockAgentClientMandateService.doesAgentHaveMissingEmail(Matchers.any(), Matchers.any())(Matchers.any()))
          .thenReturn (Future.successful(true))
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("service" -> "ated")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/missing-email"))
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("service" -> "")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the select service question")
          document.getElementsByClass("error-notification").text() must include("You must select one service")
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]

  object TestSelectServiceController extends SelectServiceController {
    override val authConnector: AuthConnector = mockAuthConnector
    override val agentClientMandateService: AgentClientMandateService = mockAgentClientMandateService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockAgentClientMandateService)
    FeatureSwitch.enable(MandateFeatureSwitches.singleService)
  }

  def viewWithUnAuthenticatedAgent(test: Future[Result] => Any) {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestSelectServiceController.view().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithUnAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestSelectServiceController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
    val result = TestSelectServiceController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
    val result = TestSelectServiceController.submit().apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
