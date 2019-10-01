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
import uk.gov.hmrc.agentclientmandate.controllers.agent.NRLQuestionController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.NRLQuestion
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.RunMode
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class NRLQuestionControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MockitoSugar with MockControllerSetup {

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
          document.title() must be("Is your client a non-resident landlord? - GOV.UK")
          document.getElementById("header").text() must include("Is your client a non-resident landlord?")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("nrl_legend").text() must be("Is your client a non-resident landlord?")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "agent requests(GET) for 'nrl question' view with some data saved" in new Setup {
        viewWithAuthorisedAgentWithSomeData { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Is your client a non-resident landlord? - GOV.UK")
          document.getElementById("header").text() must include("Is your client a non-resident landlord?")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("nrl_legend").text() must be("Is your client a non-resident landlord?")
          document.getElementById("nrl-true").attr("checked") must be("checked")
          document.getElementById("submit").text() must be("Continue")
        }
      }

    }

    "redirect agent to 'mandate details' page" when {
      "valid form is submitted and YES is selected as client pays self-assessment" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("nrl" -> "true")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/mandate/agent/paySA-question")
        }
      }
    }

    "redirect agent to 'client permission' page" when {
      "valid form is submitted and NO is selected as client pays self-assessment" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("nrl" -> "false")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/mandate/agent/client-permission/nrl")
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("nrl" -> "")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the non-resident landlord question")
          document.getElementsByClass("error-notification").text() must include("You must answer the non-resident landlord question")
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val service: String = "ATED"
  val mockDataCacheService: DataCacheService = mock[DataCacheService]



  class Setup {
    val controller = new NRLQuestionController(
      mockDataCacheService,
      app.injector.instanceOf[MessagesControllerComponents],
      mockAuthConnector,
      implicitly,
      mockAppConfig
    )

    def viewWithUnAuthenticatedAgent(test: Future[Result] => Any) {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewWithUnAuthorisedAgent(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedAgent(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedAgentWithSomeData(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[NRLQuestion](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(NRLQuestion(Some(true)))))
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }



}
