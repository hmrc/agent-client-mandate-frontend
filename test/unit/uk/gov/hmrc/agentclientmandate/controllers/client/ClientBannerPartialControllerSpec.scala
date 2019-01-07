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
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.ClientBannerPartialController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientCache
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ClientBannerPartialControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "ClientBannerPartialController" must {

    "not return NOT_FOUND at route " when {
      "GET /mandate/client/partial-banner/clientId/service" in {
        val result = route(FakeRequest(GET, "/mandate/client/partial-banner/clientId/service")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for collect email view" in {
        viewWithUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return NOT_FOUND if can't find mandate" in {
      when(mockMandateService.fetchClientMandateByClient(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)
      viewWithAuthorisedClient() { result =>
        status(result) must be(NOT_FOUND)
      }
    }

    "return partial if mandate is found and approved" in {
      when(mockMandateService.fetchClientMandateByClient(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(approvedMandate))
      viewWithAuthorisedClient() { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("client-banner-text").text() must include("You have requested Agent Ltd to act as your agent")
        document.getElementById("client-banner-text-link").attr("href") must include("/client/remove/1")
      }
    }

    "return partial if mandate is found and active" in {
      when(mockMandateService.fetchClientMandateByClient(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(activeMandate))
      viewWithAuthorisedClient() { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("client-banner-text").text() must include("Agent Ltd is now your appointed agent")
        document.getElementById("client-banner-text-link").attr("href") must include("/client/remove/1")
      }
    }

    "return partial if mandate is found and cancelled" in {
      when(mockMandateService.fetchClientMandateByClient(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cancelledMandate))
      viewWithAuthorisedClient() { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("client-banner-text").text() must include("The ATED agent can no longer act for the client")
        document.getElementById("client-banner-text-link").attr("href") must include("/client/email")
      }
    }

    "return partial if mandate is found and rejected" in {
      when(mockMandateService.fetchClientMandateByClient(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(rejectedMandate))
      viewWithAuthorisedClient() { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("client-banner-text").text() must include("Agent Ltd has rejected your request to act as your agent")
        document.getElementById("client-banner-text-link").attr("href") must include("/client/email")
      }
    }

    "return url is invalid format" in {
      viewWithAuthorisedClient(continueUrl = ContinueUrl("http://website.com")) { result =>
        status(result) must be(BAD_REQUEST)
      }
    }
  }


  val mockAuthConnector = mock[AuthConnector]
  val mockMandateService = mock[AgentClientMandateService]

  object TestClientBannerPartialController extends ClientBannerPartialController {
    override val authConnector = mockAuthConnector
    override val mandateService = mockMandateService
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(mockMandateService)
  }

  val service = "ATED"
  implicit val hc = new HeaderCarrier()
  val approvedMandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.Approved, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")
  val activeMandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.Active, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")
  val cancelledMandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.Cancelled, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")
  val rejectedMandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.Rejected, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  def viewWithUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestClientBannerPartialController.getBanner("clientId", "service", ContinueUrl("/api/anywhere")).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithAuthorisedClient(cachedData: Option[ClientCache] = None, continueUrl: ContinueUrl = ContinueUrl("/api/anywhere"))(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    val result = TestClientBannerPartialController.getBanner("clientId", "ated", continueUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
