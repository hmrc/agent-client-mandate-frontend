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

package unit.uk.gov.hmrc.agentclientmandate.controllers

import org.scalatestplus.play.PlaySpec
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.ApplicationController
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class ApplicationControllerSpec extends PlaySpec  {
  val service = "ATED"

  class Setup {
    val applicationController = new ApplicationController(stubMessagesControllerComponents())
  }

  "ApplicationController" must {

    "Keep Alive" must {

      "respond with an OK" in new Setup {
        val result: Future[Result] = applicationController.keepAlive.apply(FakeRequest())

        status(result) must be(OK)
      }
    }
  }
}
