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
import uk.gov.hmrc.agentclientmandate.controllers.agent.OverseasClientQuestionController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.OverseasClientQuestion
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, SessionBuilder}

import scala.concurrent.Future


class OverseasClientQuestionControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "OverseasClientQuestionController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'overseas client question' view" in {
        viewWithUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'overseas client question' view" in {
        viewWithUnAuthorisedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return 'overseas client question' view for AUTHORISED agent" when {

      "agent requests(GET) for 'overseas client question' view" in {
        viewWithAuthorisedAgent { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Does your client have an overseas company without a UK Unique Taxpayer Reference? - GOV.UK")
          document.getElementById("header").text() must include("Does your client have an overseas company without a UK Unique Taxpayer Reference?")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "agent requests(GET) for 'overseas client question' view with some saved data" in {
        viewWithAuthorisedAgentWithSomeData { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Does your client have an overseas company without a UK Unique Taxpayer Reference? - GOV.UK")
          document.getElementById("header").text() must include("Does your client have an overseas company without a UK Unique Taxpayer Reference?")
          document.getElementById("pre-header").text() must be("This section is: Add a client")
          document.getElementById("isOverseas-true").attr("checked") must be("checked")
          document.getElementById("submit").text() must be("Continue")
        }
      }

    }

    "redirect agent to 'nrl page' on business-customer-frontend" when {
      "valid form is submitted and overseas is answered as yes" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("isOverseas" -> "true")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/mandate/agent/nrl-question")
        }
      }
    }
    "redirect agent to 'mandate confirmation' page" when {
      "valid form is submitted and overseas is answered as no" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("isOverseas" -> "false")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/mandate/agent/details/overseas")
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("isOverseas" -> "")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the overseas client question")
          document.getElementsByClass("error-notification").text() must include("You must answer the overseas client question")
        }
      }
    }


  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val service: String = "ATED"
  val mockDataCacheService: DataCacheService = mock[DataCacheService]


  object TestOverseasClientQuestionController extends OverseasClientQuestionController {
    override val authConnector: AuthConnector = mockAuthConnector
    override val dataCacheService: DataCacheService = mockDataCacheService
    override val controllerId: String = "overseas"
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  def viewWithUnAuthenticatedAgent(test: Future[Result] => Any) {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestOverseasClientQuestionController.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithUnAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestOverseasClientQuestionController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val result = TestOverseasClientQuestionController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgentWithSomeData(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[OverseasClientQuestion](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(OverseasClientQuestion(Some(true)))))
    val result = TestOverseasClientQuestionController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
    val result = TestOverseasClientQuestionController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
