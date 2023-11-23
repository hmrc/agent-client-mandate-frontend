/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.uk.gov.hmrc.agentclientmandate.controllers.internal

import java.util.UUID
import java.time.Instant
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.internal.ClientBannerPartialInternalController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientCache
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientBannerPartialInternalControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockControllerSetup {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockMandateService: AgentClientMandateService = mock[AgentClientMandateService]

  class Setup {
    val controller = new ClientBannerPartialInternalController(
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      mockMandateService,
      implicitly,
      mockAppConfig
    )

    def viewWithUnAuthenticatedClient(test: Future[Result] => Any): Unit = {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.getClientBannerPartial("clientId", "service", RedirectUrl("/api/anywhere"))
        .apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewWithAuthorisedClient(cachedData: Option[ClientCache] = None, continueUrl: String = "/api/anywhere")(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
      val result = controller.getClientBannerPartial("clientId", "ated", RedirectUrl(continueUrl)).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockMandateService)
  }

  val service = "ATED"
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val approvedMandate: Mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
    currentStatus = MandateStatus(Status.Approved, Instant.now(), "credId"), statusHistory = Nil,
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")
  val activeMandate: Mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
    currentStatus = MandateStatus(Status.Active, Instant.now(), "credId"), statusHistory = Nil,
    Subscription(None, Service("ated", "ATED")),clientDisplayName = "client display name")
  val cancelledMandate: Mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
    currentStatus = MandateStatus(Status.Cancelled, Instant.now(), "credId"), statusHistory = Nil,
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")
  val rejectedMandate: Mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
    currentStatus = MandateStatus(Status.Rejected, Instant.now(), "credId"), statusHistory = Nil,
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  "ClientBannerPartialController" must {

    "return NOT_FOUND if can't find mandate" in new Setup {
      when(mockMandateService.fetchClientMandateByClient(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(None))
      viewWithAuthorisedClient() { result =>
        status(result) must be(NOT_FOUND)
      }
    }

    "return partial if mandate is found and approved" in new Setup {
      when(mockMandateService.fetchClientMandateByClient(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(Some(approvedMandate)))
      viewWithAuthorisedClient() { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("client-banner-text").text() must include("client.banner.text.approved")
        document.getElementById("client-banner-text-link").attr("href") must include("/client/remove/1")
      }
    }

    "return partial if mandate is found and active" in new Setup {
      when(mockMandateService.fetchClientMandateByClient(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(Some(activeMandate)))
      viewWithAuthorisedClient() { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("client-banner-text").text() must include("client.banner.text.active")
        document.getElementById("client-banner-text-link").attr("href") must include("/client/remove/1")
      }
    }

    "return partial if mandate is found and cancelled" in new Setup {
      when(mockMandateService.fetchClientMandateByClient(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(Some(cancelledMandate)))
      viewWithAuthorisedClient() { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("client-banner-text").text() must include("client.banner.text.cancelled")
        document.getElementById("client-banner-text-link").attr("href") must include("/client/email")
      }
    }

    "return partial if mandate is found and rejected" in new Setup {
      when(mockMandateService.fetchClientMandateByClient(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(Some(rejectedMandate)))
      viewWithAuthorisedClient() { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("client-banner-text").text() must include("client.banner.text.rejected")
        document.getElementById("client-banner-text-link").attr("href") must include("/client/email")
      }
    }
  }

}
