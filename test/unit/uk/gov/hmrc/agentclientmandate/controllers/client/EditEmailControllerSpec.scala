/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.EditEmailController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientCache
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EditEmailControllerSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  "EditEmailController" must {

    "redirect to login page for UNAUTHENTICATED client" when {
      "client requests(GET) for collect email view" in new Setup {
        viewWithUnAuthenticatedClient(controller)("/api/anywhere") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "client requests(GET) for updating email" in new Setup {
      viewWithAuthorisedClient(controller)("/api/anywhere") { result =>
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title() must be("client.edit-email.title - GOV.UK")
        document.getElementById("email").`val`() must be("client@client.com")

        document.getElementsByClass("govuk-back-link").text() must be("Back")
        document.getElementsByClass("govuk-back-link").attr("href") must be("/api/anywhere")
      }
    }

    "bad request if continue url is not correctly formatted" in new Setup {
      viewWithAuthorisedClient(controller)("http://website.com") { result =>
        status(result) must be(BAD_REQUEST)
      }
    }

    "get clients mandate details" when {
      "find the mandate" in new Setup {
        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
          agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
          clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
          currentStatus = MandateStatus(Status.Active, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")),
          clientDisplayName = "client display name")
        getDetailsWithAuthorisedClient(controller)(Some(mandate), "/api/anywhere") { result =>
          status(result) must be(OK)
        }
      }

      "cant find the mandate" in new Setup {
        getDetailsWithAuthorisedClient(controller)(None, "/api/anywhere") { result =>
          status(result) must be(NOT_FOUND)
        }
      }

      "mandate is not active" in new Setup {
        val mandate = Mandate(id = "1", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
          agentParty = Party("JARN123456", "Agent Ltd", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
          clientParty = Some(Party("JARN123456", "ACME Limited", PartyType.Organisation, ContactDetails("client@client.com", None))),
          currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")),
          clientDisplayName = "client display name")
        getDetailsWithAuthorisedClient(controller)(Some(mandate), "/api/anywhere") { result =>
          status(result) must be(NOT_FOUND)
        }
      }

      "bad request if continue url is not correctly formatted" in new Setup {
        getDetailsWithAuthorisedClient(controller)(None, "http://website.com") { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "")
        submitWithAuthorisedClient(controller)(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("client.email.error.email.empty")
          document.getElementsByClass("govuk-error-message").text() must include("client.email.error.email.empty")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](
            ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "email field and confirmEmail field has more than expected length" in new Setup {
        val tooLongEmail: String = "aaa@" + "a"*237 + ".com"
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> tooLongEmail)
        submitWithAuthorisedClient(controller)(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("client.email.error.email.too.long")
          document.getElementsByClass("govuk-error-message").text() must
            include("client.email.error.email.too.long")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](
            ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "invalid email id is passed" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aainvalid.com")
        submitWithAuthorisedClient(controller)(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("client.email.error.email.invalid")
          document.getElementsByClass("govuk-error-message").text() must include("client.email.error.email.invalid")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](
            ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }

    "redirect to respective page " when {

      "valid form is submitted, while updating existing client cache object" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        submitWithAuthorisedClient(controller)(fakeRequest, isValidEmail = true, redirectUrl = Some("/api/anywhere")) { result =>
          status(result) must be(SEE_OTHER)
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](
            ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }

    "back link has not been cached" in new Setup {
      val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "")
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))
      val result = controller.submit(service).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))
      status(result) must be(BAD_REQUEST)
      verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](
        ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any())
      verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](
        ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any())
      verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](
        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val injectedViewInstanceEditEmail = app.injector.instanceOf[views.html.client.editEmail]


  class Setup {
    val controller = new EditEmailController(
      mockDataCacheService,
      mockMandateService,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig,
      injectedViewInstanceEditEmail
    )
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

  def viewWithUnAuthenticatedClient(controller: EditEmailController)(continueUrl: String)(test: Future[Result] => Any): Unit = {

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view("mandateId", "service", continueUrl).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getDetailsWithAuthorisedClient(controller: EditEmailController)(mandate: Option[Mandate], continueUrl: String)(test: Future[Result] => Any): Any = {
    val userId = s"user-${UUID.randomUUID}"


    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockMandateService.fetchClientMandateByClient(ArgumentMatchers.any(), ArgumentMatchers.any())
    (ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(mandate)
    val result = controller.getClientMandateDetails("mandateId", service, continueUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedClient(controller: EditEmailController)(continueUrl: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"


    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.eq(controller.backLinkId),
      ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful("/api/anywhere"))
    when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.eq("MANDATE_ID"),
      ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful("mandateId"))
    when(mockMandateService.fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any())) thenReturn Future.successful(Some(mandate))
    val result = controller.view("mandateId", service, continueUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(controller: EditEmailController)(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                 isValidEmail: Boolean = false,
                                 redirectUrl: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some("/api/anywhere")))
    when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.eq("MANDATE_ID"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some("mandateId")))
    val result = controller.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }
}
