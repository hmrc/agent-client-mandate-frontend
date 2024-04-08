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

package unit.uk.gov.hmrc.agentclientmandate.controllers.client

import java.util.UUID
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
import uk.gov.hmrc.agentclientmandate.controllers.client.CollectEmailController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.client.collectEmail
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CollectEmailControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val injectedViewInstanceCollectEmail: collectEmail = app.injector.instanceOf[views.html.client.collectEmail]

  class Setup {
    val controller = new CollectEmailController(
      mockDataCacheService,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig,
      injectedViewInstanceCollectEmail
    )

    def viewWithUnAuthenticatedClient(redirectUrl: Option[RedirectUrl] = None)(test: Future[Result] => Any): Unit = {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, redirectUrl).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def editWithAuthorisedClient(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.eq(controller.backLinkId))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("/api/anywhere")))
      when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(cachedData))
      val result = controller.edit(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def backWithAuthorisedClient(cachedData: Option[ClientCache] = None, backLink: Option[String])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
      when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.eq(controller.backLinkId),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(backLink.get))
      when(mockDataCacheService.fetchAndGetFormData[String](ArgumentMatchers.eq(controller.backLinkId))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(backLink.get)))
      when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(cachedData))
      val result = controller.back(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedClient(cachedData: Option[ClientCache] = None, redirectUrl: Option[RedirectUrl] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
      when(mockDataCacheService.cacheFormData[String](ArgumentMatchers.eq(controller.backLinkId),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful("/test/test"))
      redirectUrl match {
        case Some(x) => when(mockDataCacheService.fetchAndGetFormData[String]
          (ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(x.toString)))
        case _ => when(mockDataCacheService.fetchAndGetFormData[String]
          (ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some("")))
      }
      when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(cachedData))
      val result = controller.view(service, redirectUrl).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                   cachedData: Option[ClientCache] = None,
                                   isValidEmail: Boolean = false,
                                   returnCache: ClientCache = ClientCache(),
                                   mode: Option[String] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[String]
        (ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("/api/anywhere")))
      when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(cachedData))
      when(mockDataCacheService.cacheFormData[ClientCache]
        (ArgumentMatchers.eq(controller.clientFormId), ArgumentMatchers.eq(returnCache))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(returnCache))
      val result = controller.submit(service, mode).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  val service = "ATED"

  "CollectEmailController" must {

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for collect email view" in new Setup {
        viewWithUnAuthenticatedClient() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return url is invalid format" in new Setup {
      viewWithAuthorisedClient(None, Some(RedirectUrl("http://www.tax.service.gov.uk/ated-subscription/review-business-details"))) { result =>
        status(result) must be(BAD_REQUEST)
      }
    }

    "return search mandate view for AUTHORISED client" when {

      "client requests(GET) for collect email view and the data hasn't been cached" in new Setup {
        viewWithAuthorisedClient() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.collect-email.title - service.name - GOV.UK")
          document.getElementsByTag("header").text() must include("client.collect-email.header")
          document.getElementsByTag("header").text() must include("ated.screen-reader.section client.collect-email.preheader")
          document.getElementsByClass("govuk-label").text() must be("client.collect-email.email.label")
          document.getElementById("email").`val`() must be("")
          document.getElementById("submit").text() must be("continue-button")
        }
      }

      "client requests(GET) for collect email view pre-populated and the data has been cached" in new Setup {
        val cached: ClientCache = ClientCache(email = Some(ClientEmail("aa@mail.com")))
        viewWithAuthorisedClient(Some(cached)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.collect-email.title - service.name - GOV.UK")
          document.getElementById("email").`val`() must be("aa@mail.com")
        }
      }
    }

    "return search mandate edit view for AUTHORISED client" when {

      "client requests(GET) for collect email view and the data hasn't been cached" in new Setup {
        editWithAuthorisedClient() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.collect-email.title - service.name - GOV.UK")
          document.getElementsByTag("header").text() must include("client.collect-email.header")
          document.getElementsByTag("header").text() must include("ated.screen-reader.section client.collect-email.preheader")
          document.getElementsByClass("govuk-label").text() must be("client.collect-email.email.label")
          document.getElementById("email").`val`() must be("")
          document.getElementById("submit").text() must be("continue-button")

          document.getElementsByClass("govuk-back-link").text() must be("Back")
          document.getElementsByClass("govuk-back-link").attr("href") must be("/mandate/client/review")
        }
      }

      "client requests(GET) for collect email view pre-populated and the data and redirect have been cached" in new Setup {

        val cached: ClientCache = ClientCache(email = Some(ClientEmail("aa@mail.com")))
        viewWithAuthorisedClient(Some(cached), Some(RedirectUrl("/api/anywhere"))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.collect-email.title - service.name - GOV.UK")
          document.getElementById("email").`val`() must be("aa@mail.com")

          document.getElementsByClass("govuk-back-link").text() must be("Back")
          document.getElementsByClass("govuk-back-link").attr("href") must be("RedirectUrl(/api/anywhere)")
        }
      }

      "client requests(GET) for collect email view pre-populated and the data has been cached, but no redirect" in new Setup {

        val cached: ClientCache = ClientCache(email = Some(ClientEmail("aa@mail.com")))
        viewWithAuthorisedClient(Some(cached), None) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.collect-email.title - service.name - GOV.UK")
          document.getElementById("email").`val`() must be("aa@mail.com")

          document.getElementById("backLinkHref") must be(null)
        }
      }
    }

    "back shows cached backlink for AUTHORISED client" when {

      "client requests(GET) for collect email view and the data hasn't been cached" in new Setup {

        val cached: ClientCache = ClientCache(email = Some(ClientEmail("aa@mail.com")))
        backWithAuthorisedClient(Some(cached), Some("http://backlink")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.collect-email.title - service.name - GOV.UK")

          document.getElementsByClass("govuk-back-link").text() must be("Back")
          document.getElementsByClass("govuk-back-link").attr("href") must be("http://backlink")
        }
      }
    }

    "redirect to respective page " when {

      "valid form is submitted, while updating existing client cache object" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("email" -> "aa@aa.com")
        val cachedData: ClientCache = ClientCache()
        val returnData: ClientCache = ClientCache(email = Some(ClientEmail("aa@aa.com")))
        submitWithAuthorisedClient(fakeRequest, isValidEmail = true, cachedData = Some(cachedData), returnCache = returnData, mode = Some("edit")) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
          verify(mockDataCacheService, times(1))
            .fetchAndGetFormData[ClientCache](ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[ClientCache](
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "valid form is submitted, while creating new client cache object" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("email" -> "aa@aa.com")
        val returnData: ClientCache = ClientCache(email = Some(ClientEmail("aa@aa.com")))
        submitWithAuthorisedClient(fakeRequest, isValidEmail = true, cachedData = None, returnCache = returnData) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/search"))
          verify(mockDataCacheService, times(1))
            .fetchAndGetFormData[ClientCache](ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[ClientCache](
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("email" -> "")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-error-summary__body").text() mustBe "client.email.error.email.empty"
          document.getElementsByClass("govuk-error-message").text() mustBe "govukErrorMessage.visuallyHiddenText: client.email.error.email.empty"
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](
            ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "email field and confirmEmail field has more than expected length" in new Setup {
        val tooLongEmail: String = "aaa@" + "a"*237 + ".com"
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("email" -> tooLongEmail)
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-error-summary__body").text() mustBe "client.email.error.email.too.long"
          document.getElementsByClass("govuk-error-message").text() mustBe "govukErrorMessage.visuallyHiddenText: client.email.error.email.too.long"
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](
            ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "invalid email id is passed - missing '@'" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("email" -> "gandalf.test.com")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-error-summary__body").text() mustBe "agent.edit-client.error.general.agent-enter-email-form"
          document.getElementsByClass("govuk-error-message")
            .text() mustBe "govukErrorMessage.visuallyHiddenText: agent.edit-client.error.general.agent-enter-email-form"
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](
            ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "invalid email id is passed - missing '.com'" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("email" -> "gandalf@mordor")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-error-summary__body").text() mustBe "agent.edit-client.error.general.agent-enter-email-form"
          document.getElementsByClass("govuk-error-message")
            .text() mustBe "govukErrorMessage.visuallyHiddenText: agent.edit-client.error.general.agent-enter-email-form"
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](
            ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "invalid email id is passed - incomplete" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("email" -> "gandalf@.com")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-error-summary__body").text() mustBe "agent.edit-client.error.general.agent-enter-email-form"
          document.getElementsByClass("govuk-error-message")
            .text() mustBe "govukErrorMessage.visuallyHiddenText: agent.edit-client.error.general.agent-enter-email-form"
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](
            ArgumentMatchers.eq(controller.backLinkId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0))
            .fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](
            ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }
  }

}
