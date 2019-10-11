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
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.MandateConfirmationController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, SessionBuilder}

import scala.concurrent.Future

class MandateConfirmationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "MandateConfirmationController" must {

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for agent confirm view" in {
        viewUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED client" when {

      "client requests(GET) for agent confirm view" in {
        viewUnAuthorisedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return search mandate view for AUTHORISED client" when {

      "client requests(GET) for agent confirm view" in {
        val mandate = Mandate(id = "ABC123", createdBy = User("cerdId", "Joe Bloggs"),
          agentParty = Party("ated-ref-no", "name",
            `type` = PartyType.Organisation,
            contactDetails = ContactDetails("aa@aa.com", None)),
          clientParty = Some(Party("client-id", "client name",
            `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None))),
          currentStatus = MandateStatus(status = Status.New, DateTime.now(), updatedBy = ""),
          statusHistory = Nil, subscription = Subscription(referenceNumber = None, service = Service(id = "ated-ref-no", name = service)),
          clientDisplayName = "client display name")
        viewAuthorisedClient(Some(mandate)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Your request to appoint an agent has been successfully submitted - GOV.UK")
          document.getElementById("banner-text").text() must include("Your request to appoint name has been successfully submitted")
          document.getElementById("notification").text() must be("Your agent will receive an email notification.")
          document.getElementById("heading").text() must be("What happens next")
          document.getElementById("finish_btn").text() must be("Sign out")
        }
      }

    }

    "redirect client to review page" when {
      "approved mandate is not returned in response" in {
        viewAuthorisedClient(None) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val service: String = "ATED"

  object TestMandateConfirmationController extends MandateConfirmationController {
    override val authConnector: AuthConnector = mockAuthConnector
    override val dataCacheService: DataCacheService = mockDataCacheService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  def viewUnAuthenticatedClient(test: Future[Result] => Any) {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestMandateConfirmationController.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }


  def viewUnAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestMandateConfirmationController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewAuthorisedClient(cachedData: Option[Mandate] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[Mandate]
      (Matchers.eq(TestMandateConfirmationController.clientApprovedMandateId))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(cachedData))
    val result = TestMandateConfirmationController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


}
