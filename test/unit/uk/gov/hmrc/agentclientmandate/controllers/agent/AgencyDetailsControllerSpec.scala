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

package unit.uk.gov.hmrc.agentclientmandate.controllers.agent

import java.util.UUID

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.agent.AgencyDetailsController
import uk.gov.hmrc.agentclientmandate.models.AgentDetails
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AgencyDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockControllerSetup {

   "AgencyDetailsController" should {

     "redirect to unathorised page" when {
       "the user is UNAUTHORISED" in new Setup {
         getWithUnAuthorisedUser(controller)("abc") { result =>
           status(result) must be(SEE_OTHER)
           redirectLocation(result).get must include("/gg/sign-in")
         }
       }
     }

     "return status OK" when {
       "user is AUTHORISED" in new Setup {
         getWithAuthorisedUser(controller)(agentDetails, "abc") { result =>
           status(result) must be(OK)
         }
       }
     }


   }

  val agentDetails: AgentDetails = AgentBuilder.buildAgentDetails

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]

  class Setup {
    val controller = new AgencyDetailsController(
      mockAgentClientMandateService,
      mockDataCacheService,
      app.injector.instanceOf[MessagesControllerComponents],
      mockAuthConnector,
      implicitly,
      mockAppConfig
    )
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  def getWithUnAuthorisedUser(controller: AgencyDetailsController)(service: String)(test: Future[Result] => Any): Any = {
    val userId = s"user-${UUID.randomUUID}"
    AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(controller: AgencyDetailsController)(agentDetails: AgentDetails, service: String)(test: Future[Result] => Any): Any = {
    val userId = s"user-${UUID.randomUUID}"
    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
    val cachedData = AgentBuilder.buildAgentDetails
    when(mockDataCacheService.cacheFormData[AgentDetails]
      (ArgumentMatchers.eq(controller.agentDetailsFormId), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(cachedData))
    when(mockAgentClientMandateService.fetchAgentDetails(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(agentDetails))
    val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
