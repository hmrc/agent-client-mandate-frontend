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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.{AtedSubscriptionFrontendConnector, BusinessCustomerFrontendConnector}
import uk.gov.hmrc.agentclientmandate.controllers.agent.{ClientPermissionController, NRLQuestionController, PaySAQuestionController}
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.{FeatureSwitch, MandateFeatureSwitches}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientPermission

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class ClientPermissionControllerSpec extends PlaySpec with OneServerPerSuite with BeforeAndAfterEach with MockitoSugar {

  "ClientPermissionController" must {

    "not return NOT_FOUND at route " when {

      "GET /mandate/agent/client-permission/:service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/client-permission/paySa")).get
        status(result) mustNot be(NOT_FOUND)
      }

      "POST /mandate/agent/client-permission/:service" in {
        val result = route(FakeRequest(POST, s"/mandate/agent/client-permission/nrl")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED agent" when {
      "agent requests(GET) for 'client permission' view" in {
        viewWithUnAuthenticatedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {
      "agent requests(GET) for 'client permission' view" in {
        viewWithUnAuthorisedAgent("paySa") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return 'nrl question' view for AUTHORISED agent" when {
      "agent requests(GET) for 'client permission' view from PaySA" in {
        viewWithAuthorisedAgent(service, PaySAQuestionController.controllerId) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Do you have permission to register on behalf of your client? - GOV.UK")
          document.getElementById("header").text() must include("Do you have permission to register on behalf of your client?")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("hasPermission_legend").text() must be("Do you have permission to register on behalf of your client?")
          document.getElementById("permission-text").text() must startWith("Your client must complete an ATED 1. Once you have registered, send their ATED 1 to HMRC and keep a copy for your records.")
          document.getElementById("continue").text() must be("Continue")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/mandate/agent/paySA-question")
        }
      }

      "agent requests(GET) for 'client permission' view from PaySA With saved data" in {
        viewWithAuthorisedAgentWithSomeData(service, PaySAQuestionController.controllerId) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Do you have permission to register on behalf of your client? - GOV.UK")
          document.getElementById("header").text() must include("Do you have permission to register on behalf of your client?")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("hasPermission_legend").text() must be("Do you have permission to register on behalf of your client?")
          document.getElementById("permission-text").text() must startWith("Your client must complete an ATED 1. Once you have registered, send their ATED 1 to HMRC and keep a copy for your records.")
          document.getElementById("hasPermission-true").attr("checked") must be("checked")
          document.getElementById("continue").text() must be("Continue")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/mandate/agent/paySA-question")
        }
      }

      "agent requests(GET) for 'client permission' view from nrl" in {
        viewWithAuthorisedAgent(service, NRLQuestionController.controllerId) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Do you have permission to register on behalf of your client? - GOV.UK")
          document.getElementById("header").text() must include("Do you have permission to register on behalf of your client?")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("hasPermission_legend").text() must be("Do you have permission to register on behalf of your client?")
          document.getElementById("continue").text() must be("Continue")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/mandate/agent/nrl-question")
        }
      }
      "agent requests(GET) for 'client permission' view for other service - it doesn't clear session cache for ated-subscription" in {
        viewWithAuthorisedAgent(serviceUsed = "otherService", "") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Do you have permission to register on behalf of your client? - GOV.UK")
        }
      }
    }

    "redirect agent to 'enter client non-uk details' page in business-customer-frontend application" when {
      "valid form is submitted and YES is selected" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("hasPermission" -> "true")
        submitWithAuthorisedAgent("", fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/mandate/agent/client-registered-previously")
        }
      }
    }

    "redirect agent to 'mandate summary' page" when {
      "valid form is submitted and NO" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("hasPermission" -> "false")
        submitWithAuthorisedAgent("", fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/summary?tabName=ATED"))
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("hasPermission" -> "")
        submitWithAuthorisedAgent("", fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the client permission question")
          document.getElementsByClass("error-notification").text() must include("You must answer the client permission question")
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessCustomerConnector = mock[BusinessCustomerFrontendConnector]
  val mockAtedSubscriptionConnector = mock[AtedSubscriptionFrontendConnector]
  val service = "ATED"
  val mockDataCacheService = mock[DataCacheService]

  object TestClientPermissionController extends ClientPermissionController {
    override val authConnector = mockAuthConnector
    override val businessCustomerConnector = mockBusinessCustomerConnector
    override val atedSubscriptionConnector = mockAtedSubscriptionConnector
    override val dataCacheService = mockDataCacheService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBusinessCustomerConnector)
    reset(mockAtedSubscriptionConnector)
  }

  def viewWithUnAuthenticatedAgent(callingPage: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestClientPermissionController.view(service, callingPage).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithUnAuthorisedAgent(callingPage: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createInvalidAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestClientPermissionController.view(service, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgent(serviceUsed: String = service, callingPage: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    when(mockBusinessCustomerConnector.clearCache(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn (Future.successful(HttpResponse(200)))
    when(mockAtedSubscriptionConnector.clearCache(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn (Future.successful(HttpResponse(200)))
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val result = TestClientPermissionController.view(serviceUsed, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgentWithSomeData(serviceUsed: String = service, callingPage: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    when(mockBusinessCustomerConnector.clearCache(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn (Future.successful(HttpResponse(200)))
    when(mockAtedSubscriptionConnector.clearCache(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn (Future.successful(HttpResponse(200)))
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientPermission](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ClientPermission(Some(true)))))
    val result = TestClientPermissionController.view(serviceUsed, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedAgent(callingPage: String, request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestClientPermissionController.submit(service, callingPage).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
