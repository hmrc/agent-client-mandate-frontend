/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.{AtedSubscriptionFrontendConnector, BusinessCustomerFrontendConnector}
import uk.gov.hmrc.agentclientmandate.controllers.agent.InformHmrcController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevRegistered
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InformHmrcControllerSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with MandateConstants  {

  "InformHmrcController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for inform HMRC view" in new Setup {
        viewWithUnAuthenticatedAgent(informHmrcController) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return inform HMRC view for AUTHORISED agent" when {

      "agent requests(GET) for inform HMRC view" in new Setup {
        viewWithAuthorisedAgent(informHmrcController) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("agent.inform-hmrc.title - GOV.UK")
          document.getElementById("header").text() must include("agent.inform-hmrc.header")
          document.getElementById("pre-header").text() must include("agent.add-a-client.sub-header")
          document.getElementById("paragraph1").text() must be("agent.inform-hmrc.p.1")
          document.getElementById("paragraph2").text() must be("agent.inform-hmrc.p.2")
          document.getElementById("paragraph3").text() must be("agent.inform-hmrc.p.3")
          document.getElementById("paragraph4").text() must be("agent.inform-hmrc.p.4")
          document.getElementById("submit").text() must be("continue-to-add-client-button")
        }
      }
    }

    "redirect Authorised agent to 'What is your client’s overseas registered business name and address?' page" when {
      "client submits" in new Setup {
        continueWithAuthorisedAgent(informHmrcController) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("http://localhost:9923/business-customer/agent/register/" +
            "non-uk-client/ated?backLinkUrl=http://localhost:9959/mandate/agent/inform-HMRC/callPage"))
        }
      }
    }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockBusinessCustomerConnector: BusinessCustomerFrontendConnector = mock[BusinessCustomerFrontendConnector]
  val mockAtedSubscriptionConnector: AtedSubscriptionFrontendConnector = mock[AtedSubscriptionFrontendConnector]
  val service: String = "ATED"
  val serviceAWRS: String = "AWRS"
  val callingPage: String = "callPage"


  class Setup {
    val informHmrcController = new InformHmrcController(
      stubbedMessagesControllerComponents,
      mockDataCacheService,
      mockBusinessCustomerConnector,
      mockAtedSubscriptionConnector,
      implicitly,
      mockAppConfig,
      mockAuthConnector
    )

    def viewWithUnAuthenticatedAgent(controller: InformHmrcController)(test: Future[Result] => Any) {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, callingPage).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewWithAuthorisedAgent(controller: InformHmrcController)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val prevReg: Option[PrevRegistered] = None

      when(mockBusinessCustomerConnector.clearCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "")))
      when(mockAtedSubscriptionConnector.clearCache(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "")))
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[PrevRegistered](ArgumentMatchers.any())(ArgumentMatchers.any(),
        ArgumentMatchers.any())).thenReturn(Future.successful(prevReg))
      val result = controller.view(service, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def continueWithAuthorisedAgent(controller: InformHmrcController)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockAppConfig.nonUkUri(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(
            "http://localhost:9923/business-customer/agent/register/non-uk-client/ated?backLinkUrl=http://localhost:9959/mandate/agent/inform-HMRC/callPage")
      val result = controller.continue(service, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBusinessCustomerConnector)
    reset(mockAtedSubscriptionConnector)
    reset(mockDataCacheService)
  }
}