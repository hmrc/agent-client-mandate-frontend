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
import uk.gov.hmrc.agentclientmandate.controllers.client.SearchMandateController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SearchMandateControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach  with MockControllerSetup with GuiceOneServerPerSuite {

  "SearchMandateController" must {

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for search mandate view" in new Setup {
        viewUnAuthenticatedClient(searchMandateController) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED client" when {

      "client requests for search mandate view" in new Setup {
        viewUnAuthenticatedClient(searchMandateController) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return search mandate view for AUTHORISED client" when {

      "client requests(GET) for search mandate view" in new Setup {
        viewWithAuthorisedClient(searchMandateController)() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.search-mandate.title - GOV.UK")
          document.getElementById("header").text() must include("client.search-mandate.header")
          document.getElementById("mandateRef").`val`() must be("")
          document.getElementById("submit").text() must be("continue-button")
        }
      }

      "client requests(GET) for search mandate view pre-populated and the data has been cached" in new Setup {
        val cached = ClientCache(mandate = Some(mandate1))
        viewWithAuthorisedClient(searchMandateController)(Some(cached)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("client.search-mandate.title - GOV.UK")
          document.getElementById("header").text() must include("client.search-mandate.header")
          document.getElementById("mandateRef").`val`() must be("ABC123")
          document.getElementById("submit").text() must be("continue-button")
        }
      }

    }

    "redirect to 'Review Mandate view' view for Authorised Client" when {

      "valid form is submitted, mandate is found from backend, cache object exists and update of cache with mandate is successful" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> s"$mandateId")
        val clientParty = Some(Party("client-id", "client name",
          `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None)))
        val cachedData = ClientCache(email = Some(ClientEmail("bb@bb.com")))
        val mandate1 = mandate.copy(clientParty = clientParty)
        val returnCache = cachedData.copy(mandate = Some(mandate1))
        submitWithAuthorisedClient(searchMandateController)(request = fakeRequest, cachedData = Some(cachedData),
          mandate = Some(mandate1), returnCache = returnCache) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
        }
      }

      "valid form is submitted but with mandate having spaces, mandate is found from backend," +
        "cache object exists and update of cache with mandate is successful" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> s"   $mandateId   ")
        val clientParty = Some(Party("client-id", "client name",
          `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None)))
        val cachedData = ClientCache(email = Some(ClientEmail("bb@bb.com")))
        val mandate1 = mandate.copy(clientParty = clientParty)
        val returnCache = cachedData.copy(mandate = Some(mandate1))
        submitWithAuthorisedClient(searchMandateController)(request = fakeRequest, cachedData = Some(cachedData),
          mandate = Some(mandate1), returnCache = returnCache) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
        }
      }

      "throw an exception when cached email not found from cache" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> s"$mandateId")
        val clientParty = Some(Party("client-id", "client name",
          `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None)))
        val cachedData = ClientCache(email = None)
        val mandate1 = mandate.copy(clientParty = clientParty)
        val returnCache = cachedData.copy(mandate = Some(mandate1))
        submitWithAuthorisedClient(searchMandateController)(request = fakeRequest, cachedData = Some(cachedData),
          mandate = Some(mandate1), returnCache = returnCache) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("email not cached")
        }
      }
    }

    "redirect to 'collect email' view for authorised client" when {
      "valid form is submitted, mandate is found from backend, but cache object doesn't exist" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> s"$mandateId")
        val returnCache = ClientCache(mandate = Some(mandate))
        submitWithAuthorisedClient(searchMandateController)(request = fakeRequest, cachedData = None,
          mandate = Some(mandate), returnCache = returnCache) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/email"))
        }
      }
    }


    "returns BAD_REQUEST" when {
      "empty form is submitted" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "")
        submitWithAuthorisedClient(searchMandateController)(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("client.search-mandate.error.general.mandateRef")
          document.getElementsByClass("govuk-error-message").text() must include("client.search-mandate.error.mandateRef")
          verify(mockMandateService, times(0)).fetchClientMandate(ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "mandateRef field has more than expected length" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "a" * 11)
        submitWithAuthorisedClient(searchMandateController)(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("client.search-mandate.error.general.mandateRef")
          document.getElementsByClass("govuk-error-message").text() must include("client.search-mandate.error.mandateRef.length")
          verify(mockMandateService, times(0)).fetchClientMandate(ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "invalid agent reference is passed" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "A1B2C3D4")
        submitWithAuthorisedClient(searchMandateController)(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("client.search-mandate.error.general.mandateRef")
          document.getElementsByClass("govuk-error-message").text() must
            include("lient.search-mandate.error.mandateRef.not-found-by-mandate-service")
          verify(mockMandateService, times(1)).fetchClientMandate(ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "agent reference is already used" in new Setup {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "A1B2C3D4")
        val returnCache = ClientCache(mandate = Some(mandate1))
        submitWithAuthorisedClient(searchMandateController)(request = fakeRequest, cachedData = None,
          mandate = Some(mandate1), returnCache = returnCache) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("client.search-mandate.error.general.mandateRef")
          document.getElementsByClass("govuk-error-message").text() must
            include("client.search-mandate.error.mandateRef.already-used-by-mandate-service")
          verify(mockMandateService, times(1)).fetchClientMandate(ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

    }

  }

  val mandateId: String = "ABC123"

  val mandate = Mandate(id = mandateId, createdBy = User("cerdId", "Joe Bloggs"),
    agentParty = Party("ated-ref-no", "name", `type` = PartyType.Organisation,
      contactDetails = ContactDetails("aa@aa.com", None)),
    clientParty = None,
    currentStatus = MandateStatus(status = Status.New, DateTime.now(), updatedBy = ""),
    statusHistory = Nil, subscription = Subscription(referenceNumber = None,
      service = Service(id = "ated-ref-no", name = "")),
    clientDisplayName = "client display name")

  val mandate1 = Mandate(id = mandateId, createdBy = User("cerdId", "Joe Bloggs"),
    agentParty = Party("ated-ref-no", "name", `type` = PartyType.Organisation,
      contactDetails = ContactDetails("aa@aa.com", None)),
    clientParty = None,
    currentStatus = MandateStatus(status = Status.Approved, DateTime.now(), updatedBy = ""),
    statusHistory = Nil, subscription = Subscription(referenceNumber = None,
      service = Service(id = "ated-ref-no", name = "")),
    clientDisplayName = "client display name")

  val service: String = "ATED"

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val injectedViewInstanceSearchMandate = app.injector.instanceOf[views.html.client.searchMandate]

  class Setup {
    val searchMandateController = new SearchMandateController(
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      mockDataCacheService,
      mockMandateService,
      implicitly,
      mockAppConfig,
      injectedViewInstanceSearchMandate
    )
  }

  override def beforeEach(): Unit = {
    reset(mockMandateService)
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }

  def viewUnAuthenticatedClient(controller: SearchMandateController)(test: Future[Result] => Any) {

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithAuthorisedClient(controller: SearchMandateController)(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"


    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(cachedData))
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(controller: SearchMandateController)(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                 cachedData: Option[ClientCache] = None,
                                 mandate: Option[Mandate] = None,
                                 returnCache: ClientCache = ClientCache())(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(cachedData))
    when(mockMandateService.fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any())).thenReturn(Future.successful(mandate))
    when(mockDataCacheService.cacheFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId),
      ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(returnCache))
    val result = controller.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
