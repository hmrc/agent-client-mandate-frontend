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

package unit.uk.gov.hmrc.agentclientmandate.controllers.agent

import java.util.UUID
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.UniqueAgentReferenceController
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientMandateDisplayDetails
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.uniqueAgentReference
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UniqueAgentReferenceControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  val injectedViewInstanceUniqueAgentReference: uniqueAgentReference = app.injector.instanceOf[views.html.agent.uniqueAgentReference]

  class Setup {
    val controller = new UniqueAgentReferenceController(
      mockAuthConnector,
      mockDataCacheService,
      stubbedMessagesControllerComponents,
      implicitly,
      mockAppConfig,
      injectedViewInstanceUniqueAgentReference
    )
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val service: String = "ated"
  val mandateId: String = "ABC123"
  val agentLastUsedEmail: String = "a.b@mail.com"

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  def viewWithUnAuthenticatedAgent(controller: UniqueAgentReferenceController)(test: Future[Result] => Any): Unit = {

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithUnAuthorisedAgent(controller: UniqueAgentReferenceController)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedAgent(controller: UniqueAgentReferenceController)(
    clientDisplayDetails: Option[ClientMandateDisplayDetails] = None)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)

    when(mockDataCacheService.fetchAndGetFormData[ClientMandateDisplayDetails](ArgumentMatchers.eq(controller.agentRefCacheId))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(clientDisplayDetails))

    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  "UniqueAgentReferenceController" must {

    "redirect to login page for UNAUTHENTICATED agent" when {

      "agent requests(GET) for 'Your unique agent reference' view" in new Setup {
        viewWithUnAuthenticatedAgent(controller) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "agent requests(GET) for 'Your unique agent reference' view" in new Setup {
        viewWithUnAuthorisedAgent(controller) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return 'what is your email address' for AUTHORISED agent" when {

      "agent requests(GET) for 'Your unique agent reference' view" in new Setup {
        viewWithAuthorisedAgent(controller)(Some(ClientMandateDisplayDetails("test name", mandateId, agentLastUsedEmail))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("agent.unique-reference.title - GOV.UK - service.name - site.govuk")
        }
      }
    }

    "redirect agent to select service page" when {
      "mandate ID is not found in cache" in new Setup {
        viewWithAuthorisedAgent(controller)() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/agent/service"))
        }
      }
    }
  }

}
