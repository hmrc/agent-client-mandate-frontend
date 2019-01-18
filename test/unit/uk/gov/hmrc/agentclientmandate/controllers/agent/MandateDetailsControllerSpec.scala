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
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.{MandateDetailsController, OverseasClientQuestionController, PaySAQuestionController}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayName}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


class MandateDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "MandateDetailsController" must {

    "not return NOT_FOUND at route " when {
      "GET /mandate/agent/details/:service" in {
        val result = route(FakeRequest(GET, s"/mandate/agent/details/overseas")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "return 'mandate details' view for AUTHORISED agent" when {

      "agent requests(GET) for check client details view and email has been cached previously and it's from PaySA" in {
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestMandateDetailsController.agentEmailFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(AgentEmail(""))))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestMandateDetailsController.clientDisplayNameFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ClientDisplayName("client display name"))))
        when(mockDataCacheService.cacheFormData[String](Matchers.eq(TestMandateDetailsController.callingPageCacheId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("callingPage"))
        viewWithAuthorisedAgent(PaySAQuestionController.controllerId) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Check your client’s details are correct - GOV.UK")
          document.getElementById("pre-header").text must be("This section is: Add a client")
          document.getElementById("header").text must be("Check your client’s details are correct")
          document.getElementById("email-address-label").text must be("Your email address")
          document.getElementById("submit").text must be("Confirm and add client")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/mandate/agent/paySA-question")
        }
      }

      "agent requests(GET) for check client details view and email has been cached previously and it's from Overseas" in {
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestMandateDetailsController.agentEmailFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(AgentEmail(""))))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestMandateDetailsController.clientDisplayNameFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ClientDisplayName("client display name"))))
        when(mockDataCacheService.cacheFormData[String](Matchers.eq(TestMandateDetailsController.callingPageCacheId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("callingPage"))
        viewWithAuthorisedAgent(OverseasClientQuestionController.controllerId) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Check your client’s details are correct - GOV.UK")
          document.getElementById("pre-header").text must be("This section is: Add a client")
          document.getElementById("header").text must be("Check your client’s details are correct")
          document.getElementById("email-address-label").text must be("Your email address")
          document.getElementById("submit").text must be("Confirm and add client")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/mandate/agent/overseas-client-question")
        }
      }
    }

    "redirect to 'collect agent email' view for AUTHORISED agent" when {

      "agent requests(GET) for check client details view and email has NOT been cached previously" in {
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestMandateDetailsController.agentEmailFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestMandateDetailsController.clientDisplayNameFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ClientDisplayName("client display name"))))
        when(mockDataCacheService.cacheFormData[String](Matchers.eq(TestMandateDetailsController.callingPageCacheId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("callingPage"))
        viewWithAuthorisedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/add-client"))
        }
      }
    }

    "redirect to 'client display name' view for AUTHORISED agent" when {

      "agent requests(GET) for check client details view and display name has NOT been cached previously" in {
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestMandateDetailsController.agentEmailFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(AgentEmail(""))))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName](Matchers.eq(TestMandateDetailsController.clientDisplayNameFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(mockDataCacheService.cacheFormData[String](Matchers.eq(TestMandateDetailsController.callingPageCacheId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("callingPage"))
        viewWithAuthorisedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/client-display-name"))
        }
      }
    }

    "redirect client details view for UNAUTHORISED agent" when {

      "agent requests(GET) for 'what is your email address' view" in {
        viewWithUnAuthorisedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect Authorised Agent to 'unique agent reference' view" when {
      "form is submitted" in {
        submitWithAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/unique-reference"))
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService = mock[DataCacheService]
  val mockMandateService = mock[AgentClientMandateService]

  object TestMandateDetailsController extends MandateDetailsController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
    override val mandateService = mockMandateService
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
    reset(mockMandateService)
  }

  val service = "ated"
  val agentEmail = AgentEmail("aa@mail.com")
  val mandateId = "AS12345678"

  def viewWithUnAuthorisedAgent(callingPage: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestMandateDetailsController.view(service, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgent(callingPage: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestMandateDetailsController.view(service, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockMandateService.createMandate(Matchers.eq(service))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mandateId))
    val fakeRequest = FakeRequest().withFormUrlEncodedBody()
    val result = TestMandateDetailsController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))
    test(result)
  }

}
