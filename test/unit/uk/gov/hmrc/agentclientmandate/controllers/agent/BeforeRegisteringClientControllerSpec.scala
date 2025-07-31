/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.OK
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.BeforeRegisteringClientController
import uk.gov.hmrc.agentclientmandate.utils.{FeatureSwitch, MandateFeatureSwitches}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import java.util.UUID

class BeforeRegisteringClientControllerSpec extends PlaySpec with MockitoSugar with GuiceOneServerPerSuite with MockControllerSetup with BeforeAndAfterEach{

  implicit val implicitMockServicesConfig: ServicesConfig = mockServicesConfig
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val service: String = "ATED"
  val injectedViewInstanceBeforeRegisteringClient: uk.gov.hmrc.agentclientmandate.views.html.agent.beforeRegisteringClient = app.injector.instanceOf[uk.gov.hmrc.agentclientmandate.views.html.agent.beforeRegisteringClient]

  val mockBeforeRegisteringClientController: BeforeRegisteringClientController = new BeforeRegisteringClientController(
    stubbedMessagesControllerComponents,
    mockAuthConnector,
    mockAppConfig,
    mockServicesConfig,
    injectedViewInstanceBeforeRegisteringClient
  )

  lazy val userId = s"user-${UUID.randomUUID}"

  def setUpMocks(): Unit = {
    AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    FeatureSwitch.disable(MandateFeatureSwitches.registeringClientContentUpdate)
  }

  "BeforeRegisteringClientController" should {

    "return OK when the view is accessed and feature flag is set to true" in {
      setUpMocks()
      FeatureSwitch.enable(MandateFeatureSwitches.registeringClientContentUpdate)
      val result = mockBeforeRegisteringClientController.view(service, "callingPage").apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) mustBe OK
    }

    "redirect to ClientPermissionController when the view is accessed and feature flag is set to false" in {
      setUpMocks()
      val result = mockBeforeRegisteringClientController.view(service, "callingPage").apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.ClientPermissionController.view("callingPage").url)
    }

    "redirect to ClientPermissionController when the submit action is called" in {
      setUpMocks()
      FeatureSwitch.enable(MandateFeatureSwitches.registeringClientContentUpdate)
      val result = mockBeforeRegisteringClientController.submit(callingPage = "nrl").apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.ClientPermissionController.view("beforeRegisteringClient").url)
    }

    "have the correct back link based on caller id" in {
      setUpMocks()
      FeatureSwitch.enable(MandateFeatureSwitches.registeringClientContentUpdate)
      val result = mockBeforeRegisteringClientController.view(service, "paySA").apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) mustBe OK
      contentAsString(result) must include(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.PaySAQuestionController.view().url)

    }
  }
}
