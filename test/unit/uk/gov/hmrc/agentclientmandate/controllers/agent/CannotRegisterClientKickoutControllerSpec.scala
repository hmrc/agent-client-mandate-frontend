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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.CannotRegisterClientKickoutController
import uk.gov.hmrc.agentclientmandate.views.html.agent.cannotRegisterClientKickout
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CannotRegisterClientKickoutControllerSpec extends PlaySpec with BeforeAndAfterEach with MockitoSugar with MockControllerSetup with GuiceOneAppPerSuite {

  implicit val implicitMockServicesConfig: ServicesConfig = mockServicesConfig
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  private val injectedView: cannotRegisterClientKickout = app.injector.instanceOf[cannotRegisterClientKickout]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
  }

  private val controller = new CannotRegisterClientKickoutController(
    mcc,
    mockAuthConnector,
    global,
    mockAppConfig,
    mockServicesConfig,
    injectedView
  )

  "KickoutController GET show" must {
    "redirect to sign-in for UNAUTHENTICATED agents" in {
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result: Future[Result] =
        controller.show("pageId").apply(SessionBuilder.buildRequestWithSessionNoUser)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get must include("/gg/sign-in")
    }

    "redirect to sign-in for UNAUTHORiSED agents" in {
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result: Future[Result] =
        controller.show("pageId").apply(SessionBuilder.buildRequestWithSession("user1"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get must include("/gg/sign-in")
    }

    "return OK and render the kick-out page" in {
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      val result: Future[Result] =
        controller.show("pageId").apply(SessionBuilder.buildRequestWithSession("user1"))
      status(result) mustBe OK
    }
  }
}