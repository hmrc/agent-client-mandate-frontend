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

package unit.uk.gov.hmrc.agentclientmandate.config

import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.config.AgentClientMandateFrontendErrorHandler
import uk.gov.hmrc.agentclientmandate.views.html.error_template
import views.agent.ViewTestHelper

class AgentClientMandateFrontendErrorHandlerSpec extends PlaySpec with GuiceOneAppPerSuite with ViewTestHelper with MockitoSugar with Injecting {

  "internalServerErrorTemplate" must {

    "retrieve the correct messages" in {
      implicit val request = FakeRequest()
      val errorTemplate: error_template = inject[error_template]
      val errorHandler = new AgentClientMandateFrontendErrorHandler(mcc.messagesApi, mockConfig, errorTemplate, mockAppConfig)
      val result = errorHandler.internalServerErrorTemplate
      val document = Jsoup.parse(contentAsString(result))

      document.title() must be("agent.client.mandate.generic.error.title - service.name")
      document.getElementsByTag("h1").text() must include("agent.client.mandate.generic.error.header")
      document.select("#main-content p").first().text() must be("agent.client.mandate.generic.error.message")
      document.select("#main-content p").last().text() must be("agent.client.mandate.generic.error.message2")
    }
  }
}
