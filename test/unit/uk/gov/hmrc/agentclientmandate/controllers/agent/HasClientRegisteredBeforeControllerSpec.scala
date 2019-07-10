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
import uk.gov.hmrc.agentclientmandate.connectors.{AtedSubscriptionFrontendConnector, BusinessCustomerFrontendConnector}
import uk.gov.hmrc.agentclientmandate.controllers.agent.{HasClientRegisteredBeforeController, PaySAQuestionController}
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevRegistered
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, SessionBuilder}

import scala.concurrent.Future

class HasClientRegisteredBeforeControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach with MockitoSugar {

  "HasClientRegisteredBeforeController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {
      "agent requests(GET) for 'has previously registered question page' view" in {
        viewWithUnAuthenticatedAgent("") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {
      "agent requests(GET) for 'has previously registered question page' view" in {
        viewWithUnAuthorisedAgent("paySa") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }
    "redirect to 'has client registered page'" when {
      "agent requests(GET) for 'has client registered page', with service = ATED" in {
        viewWithAuthorisedAgent(service, PaySAQuestionController.controllerId, Some(PrevRegistered(Some(true)))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include("Has your client previously had an agent who used the ATED online service to submit returns on their behalf?")
        }
      }

      "agent requests(GET) for 'has client registered page', with service = ATED but no prev reg info" in {
        viewWithAuthorisedAgent(service, PaySAQuestionController.controllerId) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include("Has your client previously had an agent who used the ATED online service to submit returns on their behalf?")
        }
      }
    }

    "redirect to ''" when {
      "agent requests(GET) for 'has client registered page', with service = any service" in {
        viewWithAuthorisedAgent("any", PaySAQuestionController.controllerId, Some(PrevRegistered(Some(true)))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must include("Has your client previously had an agent who used the ATED online service to submit returns on their behalf?")
        }
      }
    }

    "redirect agent to previous mandate ref page" when {
      "valid form is submitted and YES is selected" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("prevRegistered" -> "true")
        submitWithAuthorisedAgent("callPage", fakeRequest, Some(PrevRegistered(Some(true)))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/mandate/agent/search-previous/callPage")
        }
      }
    }


    "redirect agent to business-customer enter business details page" when {
      "valid form is submitted and NO" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("prevRegistered" -> "false")
        submitWithAuthorisedAgent("callPage", fakeRequest, Some(PrevRegistered(Some(true)))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"http://localhost:9923/business-customer/agent/register/non-uk-client/ated?backLinkUrl=http://localhost:9959/mandate/agent/client-registered-previously/callPage"))
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("hasPermission" -> "")
        submitWithAuthorisedAgent("callPage", fakeRequest, Some(PrevRegistered(Some(true)))) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the previously had an agent question")
          document.getElementsByClass("error-notification").text() must include("You must answer the previously had an agent question")
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockBusinessCustomerConnector: BusinessCustomerFrontendConnector = mock[BusinessCustomerFrontendConnector]
  val mockAtedSubscriptionConnector: AtedSubscriptionFrontendConnector = mock[AtedSubscriptionFrontendConnector]
  val service: String = "ATED"
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  object TestHasClientRegisteredBeforeController extends HasClientRegisteredBeforeController {
    override val authConnector: AuthConnector = mockAuthConnector
    override val businessCustomerConnector: BusinessCustomerFrontendConnector = mockBusinessCustomerConnector
    override val atedSubscriptionConnector: AtedSubscriptionFrontendConnector = mockAtedSubscriptionConnector
    override val dataCacheService: DataCacheService = mockDataCacheService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBusinessCustomerConnector)
    reset(mockAtedSubscriptionConnector)
  }

  def viewWithUnAuthenticatedAgent(callingPage: String)(test: Future[Result] => Any) {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestHasClientRegisteredBeforeController.view(service, callingPage).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithUnAuthorisedAgent(callingPage: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestHasClientRegisteredBeforeController.view(service, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgent
  (serviceUsed: String = service, callingPage: String, prevReg: Option[PrevRegistered] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockBusinessCustomerConnector.clearCache(Matchers.any())(Matchers.any()))
      .thenReturn (Future.successful(HttpResponse(OK)))
    when(mockAtedSubscriptionConnector.clearCache(Matchers.any())(Matchers.any()))
      .thenReturn (Future.successful(HttpResponse(OK)))
    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[PrevRegistered](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(prevReg))
    val result = TestHasClientRegisteredBeforeController.view(serviceUsed, callingPage).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedAgent
  (callingPage: String, request: FakeRequest[AnyContentAsFormUrlEncoded], prevReg: Option[PrevRegistered] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[PrevRegistered](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(prevReg))
    val result = TestHasClientRegisteredBeforeController.submit(service, callingPage).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
