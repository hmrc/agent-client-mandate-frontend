/*
 * Copyright 2020 HM Revenue & Customs
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
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.config.AgentClientMandateFrontendErrorHandler
import views.agent.ViewTestHelper

class AgentClientMandateFrontendErrorHandlerSpec extends PlaySpec with ViewTestHelper with MockitoSugar {

  "internalServerErrorTemplate" must {

    "retrieve the correct messages" in {
      implicit val request = FakeRequest()
      val errorHandler = new AgentClientMandateFrontendErrorHandler(mcc.messagesApi, mockConfig, mockAppConfig)
      val result = errorHandler.internalServerErrorTemplate
      val document = Jsoup.parse(contentAsString(result))

      document.title() must be("agent.client.mandate.generic.error.title")
      document.getElementsByTag("h1").text() must include("agent.client.mandate.generic.error.header")
      document.select("#content p").first().text() must be("agent.client.mandate.generic.error.message")
      document.select("#content p").last().text() must be("agent.client.mandate.generic.error.message2")
    }
  }
}
