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

package views.agent

import org.jsoup.Jsoup
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.test.FakeRequest
import play.api.i18n.{Messages, MessagesApi}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.agentclientmandate.views.html.agent.cannotRegisterClientKickout
import uk.gov.hmrc.agentclientmandate.config.AppConfig


  class CannotRegisterClientKickoutSpec
    extends AnyWordSpec
      with Matchers
      with GuiceOneAppPerSuite {

    private val view   = app.injector.instanceOf[cannotRegisterClientKickout]
    private val mcc    = app.injector.instanceOf[play.api.mvc.MessagesControllerComponents]
    private implicit val messagesApi: MessagesApi = mcc.messagesApi
    private implicit val messages: Messages = messagesApi.preferred(FakeRequest())
    private implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    "cannotRegisterClientKickout.scala.html" should {

      "render header, paragraphs, back‑link, hr and button" in {
        val html = view(
          agentSummaryUrl = "/summary",
          backLink  = Some("/agent/client-permission/pageId")
        )(FakeRequest(), messages, appConfig)

        val doc = Jsoup.parse(html.body)

        doc.title must include(messages("agent.cannot.register.client.title"))

        val h1 = doc.select("h1.govuk-heading-xl").text()
        h1 must include(messages("agent.cannot.register.client.header"))

        val paras = doc.select("div.govuk-body p.govuk-body").eachText()
        paras must contain(messages("agent.cannot.register.client.message1"))
        paras must contain(messages("agent.cannot.register.client.message2"))


        val btn = doc.select("a.govuk-button").text()
        btn must include(messages("agent.cannot.register.client.kickout.button"))

        val backLink = doc.select("a.govuk-back-link")
        backLink.text mustBe "Back"
        backLink.attr("href") mustBe "/agent/client-permission/pageId"
      }
    }
  }


