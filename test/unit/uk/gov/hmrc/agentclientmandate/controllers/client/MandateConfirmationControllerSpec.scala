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
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.MandateConfirmationController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MandateConfirmationControllerSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  "MandateConfirmationController" must {

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for agent confirm view" in new Setup {
        viewUnAuthenticatedClient(controller) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED client" when {

      "client requests(GET) for agent confirm view" in new Setup {
        viewUnAuthorisedClient(controller) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return search mandate view for AUTHORISED client" when {

      "client requests(GET) for agent confirm view" in new Setup {
        val mandate = Mandate(id = "ABC123", createdBy = User("cerdId", "Joe Bloggs"),
          agentParty = Party("ated-ref-no", "name",
            `type` = PartyType.Organisation,
            contactDetails = ContactDetails("aa@aa.com", None)),
          clientParty = Some(Party("client-id", "client name",
            `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None))),
          currentStatus = MandateStatus(status = Status.New, DateTime.now(), updatedBy = ""),
          statusHistory = Nil, subscription = Subscription(referenceNumber = None, service = Service(id = "ated-ref-no", name = service)),
          clientDisplayName = "client display name")
        viewAuthorisedClient(controller)(Some(mandate)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.agent-confirmation.title - GOV.UK")
          document.getElementById("banner-text").text() must include("client.agent-confirmation.banner-text")
          document.getElementById("notification").text() must be("client.agent-confirmation.notification")
          document.getElementById("heading").text() must be("client.agent-confirmation.header")
          document.getElementById("finish_btn").text() must be("client.agent-confirmation.finish-signout")
        }
      }

    }

    "redirect client to review page" when {
      "approved mandate is not returned in response" in new Setup {
        viewAuthorisedClient(controller)(None) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val service: String = "ATED"
  val injectedViewInstanceMandateConfirmation = app.injector.instanceOf[views.html.client.mandateConfirmation]

  class Setup {
    val controller = new MandateConfirmationController(
      stubbedMessagesControllerComponents,
      implicitly,
      mockAppConfig,
      mockDataCacheService,
      mockAuthConnector,
      injectedViewInstanceMandateConfirmation
    )
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  def viewUnAuthenticatedClient(controller: MandateConfirmationController)(test: Future[Result] => Any) {

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }


  def viewUnAuthorisedClient(controller: MandateConfirmationController)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewAuthorisedClient(controller: MandateConfirmationController)(cachedData: Option[Mandate] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"


    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[Mandate]
      (ArgumentMatchers.eq(controller.clientApprovedMandateId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(cachedData))
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


}
