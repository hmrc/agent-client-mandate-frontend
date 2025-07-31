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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.AtedSubscriptionFrontendConnector
import uk.gov.hmrc.agentclientmandate.controllers.agent.ClientPermissionController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.{ControllerPageIdConstants, FeatureSwitch, MandateFeatureSwitches}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientPermission
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.{clientPermission, clientPermission_new}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientPermissionControllerSpec extends PlaySpec with BeforeAndAfterEach with MockitoSugar with MockControllerSetup with GuiceOneServerPerSuite {

  implicit val implicitMockServicesConfig: ServicesConfig = mockServicesConfig
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAtedSubscriptionConnector: AtedSubscriptionFrontendConnector = mock[AtedSubscriptionFrontendConnector]
  val service: String = "ATED"
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val injectedViewInstanceClientPermission: clientPermission = app.injector.instanceOf[views.html.agent.clientPermission]
  val injectedViewInstanceClientPermissionNew: clientPermission_new = app.injector.instanceOf[views.html.agent.clientPermission_new]

  class Setup {
    val controller = new ClientPermissionController(
      mockAtedSubscriptionConnector,
      mockDataCacheService,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig,
      mockServicesConfig,
      injectedViewInstanceClientPermission,
      injectedViewInstanceClientPermissionNew
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

    def viewWithAuthorisedAgent(serviceUsed: String = service, callingPage: String)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      when(mockAtedSubscriptionConnector.clearCache(
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(HttpResponse(OK, ""))
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = controller.view(serviceUsed, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedAgentWithSomeData(serviceUsed: String = service, callingPage: String)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      when(mockAtedSubscriptionConnector.clearCache(
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(HttpResponse(OK, ""))
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[ClientPermission](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(ClientPermission(Some(true)))))
      val result = controller.view(serviceUsed, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedAgent(callingPage: String, request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.submit(service, callingPage).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockAtedSubscriptionConnector)
    FeatureSwitch.disable(MandateFeatureSwitches.registeringClientContentUpdate)
  }

  "ClientPermissionController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {
      "agent requests(GET) for 'client permission' view" in new Setup {
        viewWithUnAuthenticatedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {
      "agent requests(GET) for 'client permission' view" in new Setup {
        viewWithUnAuthorisedAgent("paySa") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return 'nrl question' view for AUTHORISED agent" when {
      "agent requests(GET) for 'client permission' view from PaySA" in new Setup {
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.paySAQuestionControllerId) { result =>
          status(result) must be(OK)

        }
      }

      "agent requests(GET) for 'client permission' view from PaySA With saved data" in new Setup {
        viewWithAuthorisedAgentWithSomeData(service, ControllerPageIdConstants.paySAQuestionControllerId) { result =>
          status(result) must be(OK)
        }
      }

      "agent requests(GET) for 'client permission' view from nrl" in new Setup {
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.nrlQuestionControllerId) { result =>
          status(result) must be(OK)

        }
      }

      "agent requests(GET) for 'client permission' view for other service - it doesn't clear session cache for ated-subscription" in new Setup {
        viewWithAuthorisedAgent(serviceUsed = "otherService", "") { result =>
          status(result) must be(OK)
        }
      }
    }

    "render clientPermission_new page" when {
      "feature flag is turned on and user is authorised" in new Setup {
        FeatureSwitch.enable(MandateFeatureSwitches.registeringClientContentUpdate)
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.paySAQuestionControllerId) { result =>
          status(result) must be(OK)
        }
      }
    }

    "redirect agent to 'enter client non-uk details' page in business-customer-frontend application" when {
      "valid form is submitted and YES is selected" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("hasPermission" -> "true")
        submitWithAuthorisedAgent("", fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/agent/client-registered-previously")
        }
      }
    }

    "redirect agent to 'mandate summary' page" when {
      "valid form is submitted and NO is selected and feature flag is set to false" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("hasPermission" -> "false")
        submitWithAuthorisedAgent("", fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/summary"))
        }
      }
    }

    "redirect agent to kick out page" when {
      "valid form is submitted and user clicks NO and feature flag is set to true" in new Setup {
        FeatureSwitch.enable(MandateFeatureSwitches.registeringClientContentUpdate)
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("hasPermission" -> "false")
        submitWithAuthorisedAgent("", fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/permission-kickout/"))
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("hasPermission" -> "")
        submitWithAuthorisedAgent("", fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-error-summary__body").text() mustBe "agent.client-permission.hasPermission.not-selected.error"
          document.getElementById("hasPermission-error")
            .text() mustBe "govukErrorMessage.visuallyHiddenText: agent.client-permission.hasPermission.not-selected.error"
        }
      }
    }

    "render the original view page" when {
      "feature flag is set to false" in new Setup {
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.paySAQuestionControllerId) { result =>
          status(result) must be(OK)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-body").isEmpty mustBe false
        }
      }
    }

    "render the new view page " when {
      "feature flag is set to true" in new Setup {
        FeatureSwitch.enable(MandateFeatureSwitches.registeringClientContentUpdate)
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.paySAQuestionControllerId) { result =>
          status(result) must be(OK)
          val newViewDocument: Document =Jsoup.parse(contentAsString(result))
          newViewDocument.getElementsByClass("govuk-body").isEmpty mustBe true
        }
      }
    }

    "have the correct back link" when {
      "view is rendered from paySA" in new Setup {
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.paySAQuestionControllerId) { result =>
          status(result) must be(OK)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-back-link").text() must be("Back")
          document.getElementsByClass("govuk-back-link").attr("href") must be("/mandate/agent/paySA-question")
        }
      }

      "view is rendered from nrl" in new Setup {
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.nrlQuestionControllerId) { result =>
          status(result) must be(OK)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-back-link").text() must be("Back")
          document.getElementsByClass("govuk-back-link").attr("href") must be("/mandate/agent/nrl-question")
        }
      }

      "view is rendered from beforeRegisteringClient" in new Setup {
        FeatureSwitch.enable(MandateFeatureSwitches.registeringClientContentUpdate)
        viewWithAuthorisedAgent(service, ControllerPageIdConstants.beforeRegisteringClientControllerId) { result =>
          status(result) must be(OK)
          val newViewDocument: Document = Jsoup.parse(contentAsString(result))
          newViewDocument.getElementsByClass("govuk-back-link").text() must be("Back")
          newViewDocument.getElementsByClass("govuk-back-link").attr("href") must be("/mandate/agent/before-registering-client/" + ControllerPageIdConstants.beforeRegisteringClientControllerId)
        }
      }
    }
  }
}
