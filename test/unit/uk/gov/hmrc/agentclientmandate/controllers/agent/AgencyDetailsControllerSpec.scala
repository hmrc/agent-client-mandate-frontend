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

package unit.uk.gov.hmrc.agentclientmandate.controllers.agent

import java.util.UUID

import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.agentclientmandate.controllers.agent.AgencyDetailsController
import uk.gov.hmrc.agentclientmandate.models.AgentDetails
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AuthBuilder, SessionBuilder}

import scala.concurrent.Future

class AgencyDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

   "AgencyDetailsController" should {

     "not respond with NOT_FOUND status" when {
       "GET /mandate/agent/details/edit/abc is invoked" in {
         val result = route(FakeRequest(GET, "/mandate/agent/edit")).get
         status(result) mustNot be(NOT_FOUND)
       }
     }

     "redirect to unathorised page" when {
       "the user is UNAUTHORISED" in {
         getWithUnAuthorisedUser("abc") { result =>
           status(result) must be(SEE_OTHER)
           redirectLocation(result).get must include("/gg/sign-in")
         }
       }
     }

     "return status OK" when {
       "user is AUTHORISED" in {
         getWithAuthorisedUser(agentDetails, "abc") { result =>
           status(result) must be(OK)
         }
       }
     }


   }

  val agentDetails = AgentBuilder.buildAgentDetails

  val mockAuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService = mock[AgentClientMandateService]
  val mockDataCacheService = mock[DataCacheService]

  object TestAgencyDetailsController extends AgencyDetailsController {
    override val authConnector = mockAuthConnector
    override val dataCacheService: DataCacheService = mockDataCacheService
    override val agentClientMandateService: AgentClientMandateService = mockAgentClientMandateService
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
  }

  def getWithUnAuthorisedUser(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestAgencyDetailsController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(agentDetails: AgentDetails, service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val cachedData = AgentBuilder.buildAgentDetails
    when(mockDataCacheService.cacheFormData[AgentDetails](Matchers.eq(TestAgencyDetailsController.agentDetailsFormId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    when(mockAgentClientMandateService.fetchAgentDetails()(Matchers.any(), Matchers.any())) thenReturn(Future.successful(agentDetails))
    val result = TestAgencyDetailsController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
