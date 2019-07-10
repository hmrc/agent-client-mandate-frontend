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
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.RemoveAgentController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.binders.ContinueUrl
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, SessionBuilder}

import scala.concurrent.Future

class RemoveAgentControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "RemoveAgentController" must {

    "redirect to login page for UNAUTHENTICATED client" when {
      "client requests(GET) for agent removal view" in {
        viewUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED client" when {
      "client requests(GET) for agent removal view" in {
        viewUnAuthorisedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }


    "return 'remove agent question' view for AUTHORISED agent" when {
      "client requests(GET) for 'remove agent question' view" in {

        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any(), Matchers.any())(Matchers.any())) thenReturn Future.successful(Some(mandate))
        when(mockDataCacheService.cacheFormData[String](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful("AS12345678"))
        val request = FakeRequest(GET, "/client/remove-agent/1?returnUrl=/app/return").withJsonBody(Json.toJson("""{}"""))
        viewAuthorisedClient(request, ContinueUrl("/app/return")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Confirm Agent Removal - GOV.UK")
          document.getElementById("header").text() must include("Are you sure you want to cancel the authority for Agent Ltd to act on your behalf for ATED?")
          document.getElementById("pre-header").text() must be("This section is: Manage your ATED service")
          document.getElementById("yesNo_legend").text() must be("Are you sure you want to cancel the authority for Agent Ltd to act on your behalf for ATED?")
          document.getElementById("submit").text() must be("Confirm")
        }
      }

      "can't find mandate, throw exception" in {
        when(mockAgentClientMandateService.fetchClientMandate(Matchers.any(), Matchers.any())(Matchers.any())) thenReturn Future.successful(None)
        when(mockDataCacheService.cacheFormData[String](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful("AS12345678"))
        val userId = s"user-${UUID.randomUUID}"
        AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
        val request = FakeRequest(GET, "/client/remove-agent/1?returnUrl=/app/return").withJsonBody(Json.toJson("""{}"""))
        val thrown = the[RuntimeException] thrownBy await(TestRemoveAgentController.view(service, "1", ContinueUrl("/api/anywhere"))
          .apply(SessionBuilder.updateRequestWithSession(request, userId)))

        thrown.getMessage must be("No Mandate returned")
      }

      "return url is invalid format" in {
        val request = FakeRequest(GET, "/client/remove-agent/1?returnUrl=http://website.com").withJsonBody(Json.toJson("""{}"""))
        viewAuthorisedClient(request, ContinueUrl("http://website.com")) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "submitting form" when {
      "invalid form is submitted" in {
        when(mockAgentClientMandateService.fetchClientMandateAgentName(Matchers.any(), Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful("Agent Limited"))
        when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(Some("/api/anywhere")))

        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with remove agent question")
          document.getElementsByClass("error-notification").text() must include("The remove agent question must be answered")
        }
      }

      "submitted with true will redirect to change agent" in {
        when(mockAgentClientMandateService.removeAgent(Matchers.any(), Matchers.any())(Matchers.any())) thenReturn Future.successful(true)
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "true")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/mandate/client/change")
        }
      }

      "submitted with true but agent removal fails" in {

        when(mockAgentClientMandateService.removeAgent(Matchers.any(), Matchers.any())(Matchers.any())) thenReturn Future.successful(false)
        val userId = s"user-${UUID.randomUUID}"
        AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "true")
        val thrown = the[RuntimeException] thrownBy await(TestRemoveAgentController.submit(service, "1")
          .apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId)))

        thrown.getMessage must be("Agent Removal Failed")
      }

      "submitted with false will redirect to cached return url" in {
        when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(Some("/api/anywhere")))
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "false")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must be("/api/anywhere")
        }
      }

      "submitted with false but retreival of returnUrl from cache fails" in {
        when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        val userId = s"user-${UUID.randomUUID}"
        AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("yesNo" -> "false")
        val thrown = the[RuntimeException] thrownBy await(TestRemoveAgentController.submit(service, "1")
          .apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId)))

        thrown.getMessage must be("Cache Retrieval Failed with id 1")
      }
    }

    "returnToService" when {
      "redirects to cached service" in {
        when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(Some("/api/anywhere"))
        }
        returnToServiceWithAuthorisedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must be("/api/anywhere")
        }
      }

      "fails when cache fails" in {
        when(mockDataCacheService.fetchAndGetFormData[String](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        val userId = s"user-${UUID.randomUUID}"
        AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
        val thrown = the[RuntimeException] thrownBy await(TestRemoveAgentController.returnToService()
          .apply(SessionBuilder.buildRequestWithSession(userId)))

        thrown.getMessage must be("Cache Retrieval Failed")
      }
    }

    "showConfirmation" when {
      "agent has been removed show confirmation page" in {
        when(mockAgentClientMandateService.fetchClientMandateAgentName(Matchers.any(), Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful("Agent Limited"))

        confirmationWithAuthorisedClient { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("You have successfully removed your agent - GOV.UK")
          document.getElementById("banner-text").text() must include("You have removed Agent Limited as your agent")
          document.getElementById("notification").text() must be("Your agent will receive an email notification.")
          document.getElementById("heading-1").text() must be("You can")
                    document.getElementById("return_to_service_button").text() must be("Your ATED online service")
        }
      }
    }

  }

  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  object TestRemoveAgentController extends RemoveAgentController {
    override val authConnector: AuthConnector = mockAuthConnector
    override val acmService: AgentClientMandateService = mockAgentClientMandateService
    override val dataCacheService: DataCacheService = mockDataCacheService
  }

  override def beforeEach: Unit = {
    reset(mockAgentClientMandateService)
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
    currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name")

  val service: String = "ATED"

  def viewUnAuthenticatedClient(test: Future[Result] => Any) {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestRemoveAgentController.view(service, "1", ContinueUrl("/api/anywhere")).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }


  def viewUnAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestRemoveAgentController.view(service, "1", ContinueUrl("/api/anywhere")).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewAuthorisedClient(request: FakeRequest[AnyContentAsJson], continueUrl: ContinueUrl)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    val result = TestRemoveAgentController.view(service, "1", continueUrl).apply(SessionBuilder.updateRequestWithSession(request, userId))
    test(result)
  }

  def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)

    val result = TestRemoveAgentController.submit(service, "1").apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  def returnToServiceWithAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    val result = TestRemoveAgentController.returnToService().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def confirmationWithAuthorisedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    val result = TestRemoveAgentController.confirmation(service, "1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
