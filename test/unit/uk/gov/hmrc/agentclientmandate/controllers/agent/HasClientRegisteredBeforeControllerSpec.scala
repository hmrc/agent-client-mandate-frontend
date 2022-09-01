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
import uk.gov.hmrc.agentclientmandate.controllers.agent.HasClientRegisteredBeforeController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.ControllerPageIdConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevRegistered
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.hasClientRegisteredBefore
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HasClientRegisteredBeforeControllerSpec extends PlaySpec with BeforeAndAfterEach with MockitoSugar with MockControllerSetup with GuiceOneServerPerSuite {

  "HasClientRegisteredBeforeController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {
      "agent requests(GET) for 'has previously registered question page' view" in new Setup {
        viewWithUnAuthenticatedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {
      "agent requests(GET) for 'has previously registered question page' view" in new Setup {
        viewWithUnAuthorisedAgent("paySa") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }
    "redirect to 'has client registered page'" when {
      "agent requests(GET) for 'has client registered page', with service = ATED" in new Setup {
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.paySAQuestionControllerId, Some(PrevRegistered(Some(true)))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include("agent.client-prev-registered.title - GOV.UK")
        }
      }

      "agent requests(GET) for 'has client registered page', with service = ATED but no prev reg info" in new Setup {
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.paySAQuestionControllerId) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include("agent.client-prev-registered.title - GOV.UK")
        }
      }
    }

    "redirect to ''" when {
      "agent requests(GET) for 'has client registered page', with service = any service" in new Setup {
        viewWithAuthorisedAgent("any", ControllerPageIdConstants.paySAQuestionControllerId, Some(PrevRegistered(Some(true)))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include("agent.client-prev-registered.title - GOV.UK")
        }
      }
    }

    "redirect agent to previous mandate ref page" when {
      "valid form is submitted and YES is selected" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("prevRegistered" -> "true")
        submitWithAuthorisedAgent("callPage", fakeRequest, Some(PrevRegistered(Some(true)))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/agent/previous-authorisation-number/callPage")
        }
      }
    }

    "redirect agent to business-customer enter business details page" when {
      "valid form is submitted and NO" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("prevRegistered" -> "false")
        submitWithAuthorisedAgent("callPage", fakeRequest, Some(PrevRegistered(Some(true)))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"http://localhost:9923/business-customer/agent/register/non-uk-client" +
            s"/ated?backLinkUrl=http://localhost:9959/mandate/agent/client-registered-previously/callPage"))
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("prevRegistered" -> "")
        submitWithAuthorisedAgent("callPage", fakeRequest, Some(PrevRegistered(Some(true)))) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("agent.client-prev-registered.not-selected.field-error")
          document.getElementsByClass("govuk-error-message").text() must include("agent.client-prev-registered.not-selected.field-error")
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockBusinessCustomerConnector: BusinessCustomerFrontendConnector = mock[BusinessCustomerFrontendConnector]
  val mockAtedSubscriptionConnector: AtedSubscriptionFrontendConnector = mock[AtedSubscriptionFrontendConnector]
  val service: String = "ATED"
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val injectedViewInstanceHasClientRegisteredBefore: hasClientRegisteredBefore = app.injector.instanceOf[views.html.agent.hasClientRegisteredBefore]

  class Setup {
    val controller = new HasClientRegisteredBeforeController(
      stubbedMessagesControllerComponents,
      mockDataCacheService,
      mockBusinessCustomerConnector,
      mockAtedSubscriptionConnector,
      implicitly,
      mockAppConfig,
      mockAuthConnector,
      injectedViewInstanceHasClientRegisteredBefore
    )

    def viewWithUnAuthenticatedAgent(callingPage: String)(test: Future[Result] => Any): Unit = {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, callingPage).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewWithUnAuthorisedAgent(callingPage: String)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedAgent
    (serviceUsed: String = service, callingPage: String, prevReg: Option[PrevRegistered] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      when(mockBusinessCustomerConnector.clearCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(HttpResponse(OK, "")))
      when(mockAtedSubscriptionConnector.clearCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(HttpResponse(OK, "")))
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[PrevRegistered](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(prevReg))
      val result = controller.view(serviceUsed, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedAgent
    (callingPage: String, request: FakeRequest[AnyContentAsFormUrlEncoded], prevReg: Option[PrevRegistered] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[PrevRegistered](ArgumentMatchers.any())
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
