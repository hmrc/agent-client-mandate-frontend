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
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.MandateDetailsController
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.ControllerPageIdConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayName}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.RunMode
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MandateDetailsControllerSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach with MockControllerSetup {

  "MandateDetailsController" must {

    "return 'mandate details' view for AUTHORISED agent" when {

      "agent requests(GET) for check client details view and email has been cached previously and it's from PaySA" in new Setup {
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](ArgumentMatchers.eq(controller.agentEmailFormId))
          (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(AgentEmail(""))))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName]
          (ArgumentMatchers.eq(controller.clientDisplayNameFormId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(ClientDisplayName("client display name"))))
        when(mockDataCacheService.cacheFormData[String]
          (ArgumentMatchers.eq(controller.callingPageCacheId), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("callingPage"))
        viewWithAuthorisedAgent(ControllerPageIdConstants.paySAQuestionControllerId) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("agent.check-client-details.header - GOV.UK")
          document.getElementById("pre-header").text must be("ated.screen-reader.section agent.add-a-client.sub-header")
          document.getElementById("header").text must be("agent.check-client-details.header")
          document.getElementById("email-address-label").text must be("agent.check-client-details.your-email")
          document.getElementById("submit").text must be("agent.check-client-details.confirm")

          document.getElementById("backLinkHref").text() must be("mandate.back")
          document.getElementById("backLinkHref").attr("href") must be("/agent/paySA-question")
        }
      }

      "agent requests(GET) for check client details view and email has been cached previously and it's from Overseas" in new Setup {
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail]
          (ArgumentMatchers.eq(controller.agentEmailFormId))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(AgentEmail(""))))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName]
          (ArgumentMatchers.eq(controller.clientDisplayNameFormId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(ClientDisplayName("client display name"))))
        when(mockDataCacheService.cacheFormData[String]
          (ArgumentMatchers.eq(controller.callingPageCacheId), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("callingPage"))
        viewWithAuthorisedAgent(ControllerPageIdConstants.overseasClientQuestionControllerId) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("agent.check-client-details.header - GOV.UK")
          document.getElementById("pre-header").text must be("ated.screen-reader.section agent.add-a-client.sub-header")
          document.getElementById("header").text must be("agent.check-client-details.header")
          document.getElementById("email-address-label").text must be("agent.check-client-details.your-email")
          document.getElementById("submit").text must be("agent.check-client-details.confirm")

          document.getElementById("backLinkHref").text() must be("mandate.back")
          document.getElementById("backLinkHref").attr("href") must be("/agent/overseas-client-question")
        }
      }
    }

    "redirect to 'collect agent email' view for AUTHORISED agent" when {

      "agent requests(GET) for check client details view and email has NOT been cached previously" in new Setup {
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail]
          (ArgumentMatchers.eq(controller.agentEmailFormId))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName]
          (ArgumentMatchers.eq(controller.clientDisplayNameFormId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(ClientDisplayName("client display name"))))
        when(mockDataCacheService.cacheFormData[String]
          (ArgumentMatchers.eq(controller.callingPageCacheId), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("callingPage"))
        viewWithAuthorisedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/agent/add-client"))
        }
      }
    }

    "redirect to 'client display name' view for AUTHORISED agent" when {

      "agent requests(GET) for check client details view and display name has NOT been cached previously" in new Setup {
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail]
          (ArgumentMatchers.eq(controller.agentEmailFormId))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(AgentEmail(""))))
        when(mockDataCacheService.fetchAndGetFormData[ClientDisplayName]
          (ArgumentMatchers.eq(controller.clientDisplayNameFormId))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockDataCacheService.cacheFormData[String]
          (ArgumentMatchers.eq(controller.callingPageCacheId), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful("callingPage"))
        viewWithAuthorisedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/agent/client-display-name"))
        }
      }
    }

    "redirect client details view for UNAUTHORISED agent" when {

      "agent requests(GET) for 'what is your email address' view" in new Setup {
        viewWithUnAuthorisedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect Authorised Agent to 'unique agent reference' view" when {
      "form is submitted" in new Setup {
        submitWithAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/agent/unique-reference"))
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockMandateService: AgentClientMandateService = mock[AgentClientMandateService]



  class Setup {
    val controller = new MandateDetailsController(
      stubbedMessagesControllerComponents,
      mockDataCacheService,
      mockMandateService,
      implicitly,
      mockAppConfig,
      mockAuthConnector
    )

    def viewWithUnAuthorisedAgent(callingPage: String)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedAgent(callingPage: String)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result = controller.view(service, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedAgent(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockMandateService.createMandate(ArgumentMatchers.eq(service), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(mandateId))
      val fakeRequest = FakeRequest().withFormUrlEncodedBody()
      val result = controller.submit(service).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
    reset(mockMandateService)
  }

  val service: String = "ated"
  val agentEmail: AgentEmail = AgentEmail("aa@mail.com")
  val mandateId: String = "AS12345678"
}
