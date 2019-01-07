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

package unit.uk.gov.hmrc.agentclientmandate.controllers.client

import java.util.UUID

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.ReviewMandateController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ReviewMandateControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "ReviewMandateController" must {

    "not return NOT_FOUND at route " when {
      "GET /mandate/client/review" in {
        val result = route(FakeRequest(GET, "/mandate/client/review")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for search mandate view" in {
        viewWithUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return review mandate view for AUTHORISED client" when {

      "client requests(GET) for review mandate view, and mandate has been cached on search mandate submit" in {
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
        viewWithAuthorisedClient(Some(returnData)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Check that this is the agency you want to appoint - GOV.UK")
          document.getElementById("header").text() must include("Check that this is the agency you want to appoint")
          document.getElementById("pre-heading").text() must include("Appoint an agent")
          document.getElementById("agent-ref-name-label").text() must be("Unique authorisation number")
          document.getElementById("your-email-label").text() must be("Your email address")
          document.getElementById("agent-disclaimer").text() must be("Appointing name will let them see all the details in your old returns.")
          document.getElementById("submit").text() must be("Confirm and appoint agent")
        }
      }

    }


    "redirect to search mandate view for AUTHORISED client" when {

      "client requests(GET) for review mandate view, but mandate has not been cached on search mandate submit" in {
        viewWithAuthorisedClient(Some(ClientCache(email = Some(ClientEmail(email = "aa@test.com"))))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/search"))
        }
      }

    }

    "redirect to collect eamil view for AUTHORISED client" when {

      "client requests(GET) for review mandate view, but there is no cache" in {
        viewWithAuthorisedClient() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/email"))
        }
      }

    }

    "redirect Authorised Client to 'Mandate declaration' page" when {
      "client submits form" in {
        submitWithAuthorisedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/declaration"))
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService = mock[DataCacheService]

  object TestReviewMandateController extends ReviewMandateController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  val service = "ATED"

  def viewWithUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestReviewMandateController.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithAuthorisedClient(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestReviewMandateController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    when(mockDataCacheService.cacheFormData[ClientCache](Matchers.eq(TestReviewMandateController.clientFormId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(ClientCache()))
    val result = TestReviewMandateController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestReviewMandateController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))
    test(result)
  }

}
