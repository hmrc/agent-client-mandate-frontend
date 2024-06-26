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
import java.time.Instant
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.MandateDeclarationController
import uk.gov.hmrc.agentclientmandate.models.{MandateStatus, Service, Status, Subscription, _}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.client.mandateDeclaration
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MandateDeclarationControllerSpec extends PlaySpec with MockitoSugar with MockControllerSetup with GuiceOneServerPerSuite {

  val mandateId: String = "ABC123"

  val mandate: Mandate = Mandate(id = mandateId, createdBy = User("cerdId", "Joe Bloggs"),
    agentParty = Party("ated-ref-no", "name", `type` = PartyType.Organisation,
      contactDetails = ContactDetails("aa@aa.com", None)),
    clientParty = None,
    currentStatus = MandateStatus(status = Status.New, Instant.now(), updatedBy = ""),
    statusHistory = Nil,
    subscription = Subscription(referenceNumber = None,
      service = Service(id = "ated-ref-no", name = "ATED")),
    clientDisplayName = "client display name")

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val injectedViewInstanceMandateDeclaration: mandateDeclaration = app.injector.instanceOf[views.html.client.mandateDeclaration]

  class Setup {
    val controller = new MandateDeclarationController(
      mockDataCacheService,
      mockMandateService,
      mockAuthConnector,
      stubbedMessagesControllerComponents,
      implicitly,
      mockAppConfig,
      injectedViewInstanceMandateDeclaration
    )
  }

  val service: String = "ATED"

  def viewUnAuthenticatedClient(controller: MandateDeclarationController)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewAuthorisedClient(controller: MandateDeclarationController)(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(cachedData))
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(controller: MandateDeclarationController)(
    request: FakeRequest[AnyContentAsFormUrlEncoded],
    clientCache: Option[ClientCache] = None,
    mandate: Option[Mandate] = None)(test: Future[Result] => Any
                                ): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedClient(mockAuthConnector)

    when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(clientCache))

    when(mockMandateService.approveMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any())).thenReturn(Future.successful(mandate))

    val result = controller.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  "MandateDeclarationController" must {

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for review mandate view" in new Setup {
        viewUnAuthenticatedClient(controller) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return mandate declaration view for AUTHORISED client" when {

      "client requests(GET) for mandate declaration view" in new Setup {
        val cachedData: Some[ClientCache] = Some(ClientCache(email = Some(ClientEmail("bb@bb.com")), mandate = Some(mandate)))
        viewAuthorisedClient(controller)(cachedData) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Declaration and consent - service.name - GOV.UK")
          document.getElementsByTag("header").text() must include("client.agent-declaration.header")
          document.getElementsByTag("header").text() must include("ated.screen-reader.section client.agent-declaration.pre-heading")
          document.getElementById("declare-title").text() must be("client.agent-declaration.declare-header")
          document.getElementById("agent-name").text() must be("client.agent-declaration.agent-name")
          document.getElementById("dec-info").text() must be("client.agent-declaration.information")
          document.getElementById("submit").text() must be("agree-submit")
        }
      }
    }

    "redirect to mandate review page for AUTHORISED client" when {

      "client requests(GET) for mandate declaration view but mandate not found in cache" in new Setup {
        viewAuthorisedClient(controller)(None) { result =>
          status(result) must be(SEE_OTHER)
        }
      }
    }

    "redirect to mandate confirmation page for AUTHORISED client" when {

      "valid form is submitted, mandate is found in cache and updated with status=accepted" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody()
        val cacheReturn: Some[ClientCache] = Some(ClientCache(mandate = Some(mandate)))
        val mandateReturned: Some[Mandate] = Some(mandate)
        submitWithAuthorisedClient(controller)(fakeRequest, cacheReturn, mandateReturned) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/confirmation"))
        }
      }
    }

    "redirect to mandate confirmation page for AUTHORISED client" when {

      "valid form is submitted, mandate is found in cache but update in backend fails" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody()
        val cacheReturn: Some[ClientCache] = Some(ClientCache(mandate = Some(mandate)))
        submitWithAuthorisedClient(controller)(fakeRequest, cacheReturn) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
        }
      }
    }

    "redirect to review Mandate view" when {
      "mandate is not found in cache" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody()
        submitWithAuthorisedClient(controller)(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
        }
      }
    }
  }

}
