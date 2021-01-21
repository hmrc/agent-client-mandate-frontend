/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.uk.gov.hmrc.agentclientmandate.controllers.client

import java.util.UUID

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.ReviewMandateController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReviewMandateControllerSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  "ReviewMandateController" must {

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for search mandate view" in new Setup {
        viewWithUnAuthenticatedClient(reviewMandateController) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return review mandate view for AUTHORISED client" when {

      "client requests(GET) for review mandate view, and mandate has been cached on search mandate submit" in new Setup {
        val mandate = Mandate(id = "ABC123", createdBy = User("cerdId", "Joe Bloggs"),
          agentParty = Party("ated-ref-no", "name",
            `type` = PartyType.Organisation,
            contactDetails = ContactDetails("aa@aa.com", None)),
          clientParty = Some(Party("client-id", "client name",
            `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None))),
          currentStatus = MandateStatus(status = Status.New, DateTime.now(), updatedBy = ""),
          statusHistory = Nil, subscription = Subscription(referenceNumber = None, service = Service(id = "ated-ref-no", name = "")),
          clientDisplayName = "client display name")
        val returnData = ClientCache(mandate = Some(mandate))
        viewWithAuthorisedClient(reviewMandateController)(Some(returnData)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.review-agent.title - GOV.UK")
          document.getElementById("header").text() must include("client.review-agent.header")
          document.getElementById("pre-heading").text() must include("ated.screen-reader.section client.review-agent.preheader")
          document.getElementById("agent-ref-name-label").text() must be("client.review-agent.agent-reference")
          document.getElementById("your-email-label").text() must be("client.review-agent.own.email")
          document.getElementById("agent-disclaimer").text() must be("client.review-agent.disclaimer")
          document.getElementById("submit").text() must be("client.review-agent.submit")
        }
      }

    }


    "redirect to search mandate view for AUTHORISED client" when {

      "client requests(GET) for review mandate view, but mandate has not been cached on search mandate submit" in new Setup {
        viewWithAuthorisedClient(reviewMandateController)(Some(ClientCache(email = Some(ClientEmail(email = "aa@test.com"))))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/search"))
        }
      }

    }

    "redirect to collect eamil view for AUTHORISED client" when {

      "client requests(GET) for review mandate view, but there is no cache" in new Setup {
        viewWithAuthorisedClient(reviewMandateController)() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/email"))
        }
      }

    }

    "redirect Authorised Client to 'Mandate declaration' page" when {
      "client submits form" in new Setup {
        submitWithAuthorisedClient(reviewMandateController) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/declaration"))
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val injectedViewInstanceReviewMandate = app.injector.instanceOf[views.html.client.reviewMandate]

  class Setup {
    val reviewMandateController = new ReviewMandateController(
      mockDataCacheService,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig,
      injectedViewInstanceReviewMandate
    )
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  val service: String = "ATED"

  def viewWithUnAuthenticatedClient(controller: ReviewMandateController)(test: Future[Result] => Any) {

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithAuthorisedClient(controller: ReviewMandateController)(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"


    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(cachedData))
    when(mockDataCacheService.cacheFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId),
      ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(ClientCache()))
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(controller: ReviewMandateController)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"


    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    val result = controller.submit(service).apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
    test(result)
  }

}
