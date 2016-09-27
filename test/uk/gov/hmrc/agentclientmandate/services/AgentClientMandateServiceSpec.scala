/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.services

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.builders.AuthBuilder
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmail
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future


class AgentClientMandateServiceSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "AgentClientMandateService" should {

    "not create a mandate" when {

      "no agent email is found in the keystore" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(None)

        val response = TestAgentClientMandateService.createMandate(service)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Email not found in cache")

      }

      "there is a problem while creating the mandate" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val cachedEmail = AgentEmail("aa@aa.com", "aa@aa.com")

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cachedEmail))
        when(mockAgentClientMandateConnector.createMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = TestAgentClientMandateService.createMandate(service)
        val thrown = the[RuntimeException] thrownBy await(response)
        thrown.getMessage must include("Mandate not created")

      }
    }

    "create a mandate" when {

      "agent email is found in the keystore" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val cachedEmail = AgentEmail("aa@aa.com", "aa@aa.com")
        val respJson = Json.parse("""{"mandateId": "AS12345678"}""")

        when(mockDataCacheService.fetchAndGetFormData[AgentEmail](Matchers.eq(TestAgentClientMandateService.agentEmailFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(Some(cachedEmail))
        when(mockAgentClientMandateConnector.createMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(CREATED, Some(respJson)))
        when(mockDataCacheService.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        when(mockDataCacheService.cacheFormData[String](Matchers.eq(TestAgentClientMandateService.agentRefCacheId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful("AS12345678"))

        val response = TestAgentClientMandateService.createMandate(service)
        await(response) must be("AS12345678")

      }

    }

    "not fetch any mandate" when {

      "incorrect mandate id is passed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        when(mockAgentClientMandateConnector.fetchMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None))

        val response = TestAgentClientMandateService.fetchClientMandate(mandateId)
        await(response) must be(None)
      }

    }

    "fetch correct mandate" when {

      "correct mandate id is passed" in {
        implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "agent")
        val respJson = Json.toJson(mandateNew)
        when(mockAgentClientMandateConnector.fetchMandate(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn Future.successful(HttpResponse(OK, Some(respJson)))

        val response = TestAgentClientMandateService.fetchClientMandate(mandateId)
        await(response) must be(Some(mandateNew))
      }

    }
  }


  val mandateDto: CreateMandateDto = CreateMandateDto("test@test.com", "ATED")
  val mandateId = "12345678"
  val service = "ATED"

  val time1 = DateTime.now()

  val mandateNew: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)), clientParty = None, currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Some(Seq(MandateStatus(Status.New, time1, "credId"))), Subscription(None, Service("ated", "ATED")))

  val mockAgentClientMandateConnector = mock[AgentClientMandateConnector]
  val mockDataCacheService = mock[DataCacheService]

  object TestAgentClientMandateService extends AgentClientMandateService {
    override val dataCacheService = mockDataCacheService
    override val agentClientMandateConnector = mockAgentClientMandateConnector
  }

  override def beforeEach = {
    reset(mockDataCacheService)
    reset(mockAgentClientMandateConnector)
  }

}