/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.EditEmailController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class EditEmailControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "EditEmailController" must {

    "not return NOT_FOUND at route " when {
      "GET /mandate/client/details/:clientId/:service" in {
        val result = route(FakeRequest(GET, "/mandate/client/details/clientId/service")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

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
        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.Active, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")
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
        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")
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
          document.getElementsByClass("error-notification").text() must include("You must answer the email address question.")
          verify(mockEmailService, times(0)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.eq(TestEditEmailController.clientFormId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "email field and confirmEmail field has more than expected length" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "a" * 242)
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the email address question")
          document.getElementsByClass("error-notification").text() must include("The email address cannot be more than 241 characters.")
          verify(mockEmailService, times(0)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.eq(TestEditEmailController.clientFormId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "invalid email id is passed" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@invalid.com")
        submitWithAuthorisedClient(fakeRequest, isValidEmail = false) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("This email is invalid")
          verify(mockEmailService, times(1)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.eq(TestEditEmailController.clientFormId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }

    "redirect to respective page " when {

      "valid form is submitted, while updating existing client cache object" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        submitWithAuthorisedClient(fakeRequest, isValidEmail = true, redirectUrl = Some("/api/anywhere")) { result =>
          status(result) must be(SEE_OTHER)
          verify(mockEmailService, times(1)).validate(Matchers.any())(Matchers.any())
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())
        }
      }
    }

    "back link has not been cached" in {
      val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "")
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      val result = TestEditEmailController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))
      status(result) must be(BAD_REQUEST)
      verify(mockEmailService, times(0)).validate(Matchers.any())(Matchers.any())
      verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())
      verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.eq(TestEditEmailController.clientFormId))(Matchers.any(), Matchers.any())
      verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockEmailService: EmailService = mock[EmailService]
  val mockMandateService = mock[AgentClientMandateService]

  object TestEditEmailController extends EditEmailController {
    override val mandateService = mockMandateService
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
    override val emailService = mockEmailService
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
    reset(mockEmailService)
    reset(mockMandateService)
  }

  val service = "ATED"

  val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))), currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  def viewWithUnAuthenticatedClient(continueUrl: ContinueUrl)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestEditEmailController.view("mandateId", "service", continueUrl).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getDetailsWithAuthorisedClient(mandate: Option[Mandate], continueUrl: ContinueUrl)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockMandateService.fetchClientMandateByClient(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(mandate)
    val result = TestEditEmailController.getClientMandateDetails("mandateId", service, continueUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedClient(continueUrl: ContinueUrl)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.cacheFormData[String](Matchers.eq(TestEditEmailController.backLinkId),
      Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("/api/anywhere"))
    when(mockDataCacheService.cacheFormData[String](Matchers.eq("MANDATE_ID"),
      Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("mandateId"))
    when(mockMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(mandate))
    val result = TestEditEmailController.view("mandateId", service, continueUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                 isValidEmail: Boolean = false,
                                 redirectUrl: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestEditEmailController.backLinkId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("/api/anywhere")))
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq("MANDATE_ID"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("mandateId")))
    when(mockEmailService.validate(Matchers.any())(Matchers.any())).thenReturn(Future.successful(isValidEmail))
    val result = TestEditEmailController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }
}
