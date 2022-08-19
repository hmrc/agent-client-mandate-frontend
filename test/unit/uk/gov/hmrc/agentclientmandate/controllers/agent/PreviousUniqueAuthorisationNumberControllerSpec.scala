/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.agentclientmandate.connectors.{AtedSubscriptionFrontendConnector, BusinessCustomerFrontendConnector}
import uk.gov.hmrc.agentclientmandate.controllers.agent.PreviousUniqueAuthorisationNumberController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.ControllerPageIdConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevUniqueAuthNum
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.previousUniqueAuthorisationNumber
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PreviousUniqueAuthorisationNumberControllerSpec extends PlaySpec
  with BeforeAndAfterEach
  with MockitoSugar
  with MockControllerSetup
  with GuiceOneServerPerSuite {

  "PreviousUniqueAuthorisationNumberController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {
      "agent requests(GET) for 'has previously registered question page' view" in new Setup {
        viewWithUnAuthenticatedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to 'has client registered page'" when {
      "agent requests(GET) for 'has client registered page', with service = ATED" in new Setup {
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.paySAQuestionControllerId, Some(PrevUniqueAuthNum(Some(true)))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include("agent.prev-auth-num.title - GOV.UK")
        }
      }

      "agent requests(GET) for 'has client registered page', with service = ATED but no prev reg info" in new Setup {
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.paySAQuestionControllerId) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include("agent.prev-auth-num.title - GOV.UK")
        }
      }
    }

    "redirect to Do you have the previous unique authorisation number for this client?" when {
      "agent requests(GET) for 'has client registered page', with service = any service" in new Setup {
        viewWithAuthorisedAgent("any", ControllerPageIdConstants.paySAQuestionControllerId, Some(PrevUniqueAuthNum(Some(true)))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include("agent.prev-auth-num.title - GOV.UK")
        }
      }
    }

    "redirect agent to previous mandate ref page" when {
      "valid form is submitted and YES is selected" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("authNum" -> "true")
        submitWithAuthorisedAgent("callPage", fakeRequest, Some(PrevUniqueAuthNum(Some(true)))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/agent/search-previous/callPage")
        }
      }
    }


    "redirect agent to business-customer enter business details page" when {
      "valid form is submitted and NO" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("authNum" -> "false")
        submitWithAuthorisedAgent("callPage", fakeRequest, Some(PrevUniqueAuthNum(Some(true)))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/inform-HMRC/callPage"))
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("authNum" -> "")
        submitWithAuthorisedAgent("callPage", fakeRequest, Some(PrevUniqueAuthNum(Some(true)))) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("agent.prev-auth-num.not-selected.field-error")
          document.getElementsByClass("govuk-error-message").text() must include("agent.prev-auth-num.not-selected.field-error")
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockBusinessCustomerConnector: BusinessCustomerFrontendConnector = mock[BusinessCustomerFrontendConnector]
  val mockAtedSubscriptionConnector: AtedSubscriptionFrontendConnector = mock[AtedSubscriptionFrontendConnector]
  val service: String = "ATED"
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val injectedViewInstancePreviousUniqueAuthorisationNumber: previousUniqueAuthorisationNumber =
    app.injector.instanceOf[views.html.agent.previousUniqueAuthorisationNumber]



  class Setup {
    val controller = new PreviousUniqueAuthorisationNumberController(
      stubbedMessagesControllerComponents,
      mockDataCacheService,
      mockBusinessCustomerConnector,
      mockAtedSubscriptionConnector,
      implicitly,
      mockAppConfig,
      mockAuthConnector,
      injectedViewInstancePreviousUniqueAuthorisationNumber
    )

    def viewWithUnAuthenticatedAgent(callingPage: String)(test: Future[Result] => Any): Unit = {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, callingPage).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewWithAuthorisedAgent
    (serviceUsed: String = service, callingPage: String, prevReg: Option[PrevUniqueAuthNum] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      when(mockBusinessCustomerConnector.clearCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(HttpResponse(OK, "")))
      when(mockAtedSubscriptionConnector.clearCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(HttpResponse(OK, "")))
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[PrevUniqueAuthNum](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(prevReg))
      val result = controller.view(serviceUsed, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedAgent
    (callingPage: String, request: FakeRequest[AnyContentAsFormUrlEncoded], prevReg: Option[PrevUniqueAuthNum] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[PrevUniqueAuthNum](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(prevReg))
      val result = controller.submit(service, callingPage).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBusinessCustomerConnector)
    reset(mockAtedSubscriptionConnector)
  }
}
