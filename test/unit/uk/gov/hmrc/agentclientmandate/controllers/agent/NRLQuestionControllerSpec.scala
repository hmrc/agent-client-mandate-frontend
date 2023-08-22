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
import uk.gov.hmrc.agentclientmandate.controllers.agent.NRLQuestionController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.NRLQuestion
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.nrl_question
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NRLQuestionControllerSpec extends PlaySpec with BeforeAndAfterEach with MockitoSugar with MockControllerSetup with GuiceOneServerPerSuite {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val service: String = "ATED"
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val injectedViewInstanceNrlQuestion: nrl_question = app.injector.instanceOf[views.html.agent.nrl_question]

  class Setup {
    val controller = new NRLQuestionController(
      mockDataCacheService,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig,
      injectedViewInstanceNrlQuestion
    )

    def viewWithUnAuthenticatedAgent(test: Future[Result] => Any): Unit = {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewWithUnAuthorisedAgent(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedAgent(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedAgentWithSomeData(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[NRLQuestion](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(NRLQuestion(Some(true)))))
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "NRLQuestionController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {
      "agent requests(GET) for 'nrl question' view" in new Setup {
        viewWithUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {
      "agent requests(GET) for 'nrl question' view" in new Setup {
        viewWithUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return 'nrl question' view for AUTHORISED agent" when {
      "agent requests(GET) for 'nrl question' view" in new Setup {
        viewWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("agent.nrl-question.title - GOV.UK")
          document.getElementsByTag("header").text() must include("agent.nrl-question.header")
          document.getElementsByTag("header").text() must include("ated.screen-reader.section agent.add-a-client.sub-header")
          document.getElementsByClass("govuk-fieldset__legend").text() must be("agent.nrl-question.header")
          document.getElementById("submit").text() must be("continue-button")
        }
      }

      "agent requests(GET) for 'nrl question' view with some data saved" in new Setup {
        viewWithAuthorisedAgentWithSomeData { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("agent.nrl-question.title - GOV.UK")
          document.getElementsByTag("header").text() must include("agent.nrl-question.header")
          document.getElementsByTag("header").text() must include("ated.screen-reader.section agent.add-a-client.sub-header")
          document.getElementsByClass("govuk-fieldset__legend").text() must be("agent.nrl-question.header")
          document.getElementById("nrl").attr("checked") must be("")
          document.getElementById("submit").text() must be("continue-button")
        }
      }

    }

    "redirect agent to 'mandate details' page" when {
      "valid form is submitted and YES is selected as client pays self-assessment" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("nrl" -> "true")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/agent/paySA-question")
        }
      }
    }

    "redirect agent to 'client permission' page" when {
      "valid form is submitted and NO is selected as client pays self-assessment" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("nrl" -> "false")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/agent/client-permission/nrl")
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("nrl" -> "")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-error-summary__body").text() mustBe "agent.nrl-question.nrl.not-selected.error"
          document.getElementsByClass("govuk-error-message").text() mustBe "govukErrorMessage.visuallyHiddenText: agent.nrl-question.nrl.not-selected.error"
        }
      }
    }
  }

}
