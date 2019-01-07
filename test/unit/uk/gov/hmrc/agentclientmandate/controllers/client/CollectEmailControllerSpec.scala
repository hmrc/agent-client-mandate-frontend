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

import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.CollectEmailController
import uk.gov.hmrc.agentclientmandate.service.{DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.utils.{FeatureSwitch, MandateFeatureSwitches}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class CollectEmailControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "CollectEmailController" must {

    "not return NOT_FOUND at route " when {
      "GET /mandate/client/email" in {
        val result = route(FakeRequest(GET, "/mandate/client/email")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for collect email view" in {
        viewWithUnAuthenticatedClient() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return url is invalid format" in {
      viewWithAuthorisedClient(None, Some(ContinueUrl("http://website.com"))) { result =>
        status(result) must be(BAD_REQUEST)
      }
    }

    "return search mandate view for AUTHORISED client" when {

      "client requests(GET) for collect email view and the data hasn't been cached" in {
        viewWithAuthorisedClient() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address? - GOV.UK")
          document.getElementById("header").text() must include("What is your email address?")
          document.getElementById("pre-heading").text() must include("Appoint an agent")
          document.getElementById("email_field").text() must be("What is your email address?")
          document.getElementById("email").`val`() must be("")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "client requests(GET) for collect email view pre-populated and the data has been cached" in {
        val cached = ClientCache(email = Some(ClientEmail("aa@mail.com")))
        viewWithAuthorisedClient(Some(cached)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address? - GOV.UK")
          document.getElementById("email").`val`() must be("aa@mail.com")
        }
      }

    }

    "return search mandate edit view for AUTHORISED client" when {

      "client requests(GET) for collect email view and the data hasn't been cached" in {
        editWithAuthorisedClient() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address? - GOV.UK")
          document.getElementById("header").text() must include("What is your email address?")
          document.getElementById("pre-heading").text() must include("Appoint an agent")
          document.getElementById("email_field").text() must be("What is your email address?")
          document.getElementById("email").`val`() must be("")
          document.getElementById("submit").text() must be("Continue")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/mandate/client/review")
        }

      }

      "client requests(GET) for collect email view pre-populated and the data and redirect have been cached" in {

        val cached = ClientCache(email = Some(ClientEmail("aa@mail.com")))
        viewWithAuthorisedClient(Some(cached), Some(ContinueUrl("/api/anywhere"))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address? - GOV.UK")
          document.getElementById("email").`val`() must be("aa@mail.com")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/api/anywhere")
        }
      }

      "client requests(GET) for collect email view pre-populated and the data has been cached, but no redirect" in {

        val cached = ClientCache(email = Some(ClientEmail("aa@mail.com")))
        viewWithAuthorisedClient(Some(cached), None) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address? - GOV.UK")
          document.getElementById("email").`val`() must be("aa@mail.com")

          document.getElementById("backLinkHref") must be(null)
        }
      }
    }

    "back shows cached backlink for AUTHORISED client" when {

      "client requests(GET) for collect email view and the data hasn't been cached" in {

        val cached = ClientCache(email = Some(ClientEmail("aa@mail.com")))
        backWithAuthorisedClient(Some(cached), Some(ContinueUrl("http://backlink"))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your email address? - GOV.UK")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("http://backlink")
        }
      }
    }

    "redirect to respective page " when {

      "valid form is submitted, while updating existing client cache object" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        val cachedData = ClientCache()
        val returnData = ClientCache(email = Some(ClientEmail("aa@aa.com")))
        submitWithAuthorisedClient(fakeRequest, isValidEmail = true, cachedData = Some(cachedData), returnCache = returnData, mode = Some("edit")) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "valid form is submitted, while creating new client cache object" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aa@aa.com")
        val returnData = ClientCache(email = Some(ClientEmail("aa@aa.com")))
        submitWithAuthorisedClient(fakeRequest, isValidEmail = true, cachedData = None, returnCache = returnData) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/search"))
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(1)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
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
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestCollectEmailController.backLinkId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId))(Matchers.any(), Matchers.any())
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
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestCollectEmailController.backLinkId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }


      "invalid email id is passed" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("email" -> "aainvalid.com")
        submitWithAuthorisedClient(fakeRequest, isValidEmail = false) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("This email is invalid")
          verify(mockDataCacheService, times(1)).fetchAndGetFormData[String](Matchers.eq(TestCollectEmailController.backLinkId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId))(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  object TestCollectEmailController extends CollectEmailController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  val service = "ATED"


  def viewWithUnAuthenticatedClient(redirectUrl: Option[ContinueUrl] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestCollectEmailController.view(service, redirectUrl).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def editWithAuthorisedClient(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestCollectEmailController.backLinkId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("/api/anywhere")))
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestCollectEmailController.edit(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def backWithAuthorisedClient(cachedData: Option[ClientCache] = None, backLink: Option[ContinueUrl])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.cacheFormData[ContinueUrl](Matchers.eq(TestCollectEmailController.backLinkId),
      Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(backLink.get))
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestCollectEmailController.backLinkId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(backLink.get.url)))
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestCollectEmailController.back(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedClient(cachedData: Option[ClientCache] = None, redirectUrl: Option[ContinueUrl] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.cacheFormData[String](Matchers.eq(TestCollectEmailController.backLinkId),
      Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("/test/test"))
    redirectUrl match {
      case Some(x) => when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestCollectEmailController.backLinkId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(x.url)))
      case _ => when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestCollectEmailController.backLinkId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("")))
    }
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestCollectEmailController.view(service, redirectUrl).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                 cachedData: Option[ClientCache] = None,
                                 isValidEmail: Boolean = false,
                                 returnCache: ClientCache = ClientCache(),
                                 mode: Option[String] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[String](Matchers.eq(TestCollectEmailController.backLinkId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("/api/anywhere")))
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    when(mockDataCacheService.cacheFormData[ClientCache](Matchers.eq(TestCollectEmailController.clientFormId), Matchers.eq(returnCache))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnCache))
    val result = TestCollectEmailController.submit(service, mode).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }


}
