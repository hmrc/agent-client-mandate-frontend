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
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.EditEmailController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientCache
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.binders.ContinueUrl
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, SessionBuilder}

import scala.concurrent.Future

class EditEmailControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "EditEmailController" must {

    "redirect to login page for UNAUTHENTICATED client" when {
      "client requests(GET) for collect email view" in {
        viewWithUnAuthenticatedClient(ContinueUrl("/api/anywhere")) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "client requests(GET) for updating email" in {
      viewWithAuthorisedClient(ContinueUrl("/api/anywhere")) { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title() must be("Edit your email address - GOV.UK")
        document.getElementById("email").`val`() must be("client@client.com")

        document.getElementById("backLinkHref").text() must be("Back")
        document.getElementById("backLinkHref").attr("href") must be("/api/anywhere")
      }
    }

    "bad request if continue url is not correctly formatted" in {
      viewWithAuthorisedClient(ContinueUrl("http://website.com")) { result =>
        status(result) must be(BAD_REQUEST)
      }
    }

    "get clients mandate details" when {
      "find the mandate" in {
        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
          agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
          clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
          currentStatus = MandateStatus(Status.Active, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")),
          clientDisplayName = "client display name")
        getDetailsWithAuthorisedClient(Some(mandate), ContinueUrl("/api/anywhere")) { result =>
          status(result) must be(OK)
        }
      }

      "cant find the mandate" in {
        getDetailsWithAuthorisedClient(None, ContinueUrl("/api/anywhere")) { result =>
          status(result) must be(NOT_FOUND)
        }
      }

      "mandate is not active" in {
        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
          agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
          clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
          currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")),
          clientDisplayName = "client display name")
        getDetailsWithAuthorisedClient(Some(mandate), ContinueUrl("/api/anywhere")) { result =>
          status(result) must be(NOT_FOUND)
        }
      }

      "bad request if continue url is not correctly formatted" in {
        getDetailsWithAuthorisedClient(None, ContinueUrl("http://website.com")) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must include("Enter the email address you want to use for this client")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](Matchers.eq(TestEditEmailController.clientFormId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "email field and confirmEmail field has more than expected length" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aaa@aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.com")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must
            include("The email address you want to use for this client must be 241 characters or less")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](Matchers.eq(TestEditEmailController.clientFormId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "invalid email id is passed" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aainvalid.com")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must include("Enter an email address in the correct format, like name@example.com")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](Matchers.eq(TestEditEmailController.clientFormId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }

    "redirect to respective page " when {

      "valid form is submitted, while updating existing client cache object" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        submitWithAuthorisedClient(fakeRequest, isValidEmail = true, redirectUrl = Some("/api/anywhere")) { result =>
          status(result) must be(SEE_OTHER)
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())
        }
      }
    }

    "back link has not been cached" in {
      val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "")
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))
      val result = TestEditEmailController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))
      status(result) must be(BAD_REQUEST)
      verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())
      verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.eq(TestEditEmailController.clientFormId))(Matchers.any(), Matchers.any())
      verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockMandateService: AgentClientMandateService = mock[AgentClientMandateService]

  object TestEditEmailController extends EditEmailController {
    override val mandateService: AgentClientMandateService = mockMandateService
    override val authConnector: AuthConnector = mockAuthConnector
    override val dataCacheService: DataCacheService = mockDataCacheService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
    reset(mockMandateService)
  }

  val service = "ATED"

  val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
    currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name")

  def viewWithUnAuthenticatedClient(continueUrl: ContinueUrl)(test: Future[Result] => Any): Unit = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = TestEditEmailController.view("mandateId", "service", continueUrl).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getDetailsWithAuthorisedClient(mandate: Option[Mandate], continueUrl: ContinueUrl)(test: Future[Result] => Any): Any = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockMandateService.fetchClientMandateByClient(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())) thenReturn Future.successful(mandate)
    val result = TestEditEmailController.getClientMandateDetails("mandateId", service, continueUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedClient(continueUrl: ContinueUrl)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockDataCacheService.cacheFormData[String](Matchers.eq(TestEditEmailController.backLinkId),
      Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("/api/anywhere"))
    when(mockDataCacheService.cacheFormData[String](Matchers.eq("MANDATE_ID"),
      Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("mandateId"))
    when(mockMandateService.fetchClientMandate(Matchers.any(), Matchers.any())(Matchers.any())) thenReturn Future.successful(Some(mandate))
    val result = TestEditEmailController.view("mandateId", service, continueUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                 isValidEmail: Boolean = false,
                                 redirectUrl: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some("/api/anywhere")))
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq("MANDATE_ID"))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some("mandateId")))
    val result = TestEditEmailController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }
}
