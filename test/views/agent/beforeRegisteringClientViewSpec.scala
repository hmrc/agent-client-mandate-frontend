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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.views.html.agent.beforeRegisteringClient


class beforeRegisteringClientViewSpec extends AnyWordSpec with MockitoSugar with ViewTestHelper with GuiceOneServerPerSuite{

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  override implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  val injectedViewInstanceBeforeRegisteringClient: beforeRegisteringClient = app.injector.instanceOf[beforeRegisteringClient]

  val view: Html = injectedViewInstanceBeforeRegisteringClient("callingPage", "ATED", "backLink")
  val doc: Document = Jsoup.parse(view.toString)

  "The before registering client view" when {
    "rendered" must {

      "have the correct page title" in {
        doc.title mustBe ("Before registering your client - Submit and view your ATED returns - GOV.UK")
      }

      "have the correct header" in {
        doc.select("h1").text() mustBe ("This section is: Add a client Before registering your client")
      }

      "have the correct information text" in {
        doc.getElementById("bullet-1").html() mustBe "each client you register must complete an <a class=\"govuk-link\" href=\"https://www.gov.uk/government/publications/annual-tax-on-enveloped-dwellings-ated-1\"> ATED 1 </a> form. If you already have an ATED 1 for a client, they do not need to complete another."
        doc.getElementById("bullet-2").html() mustBe "once you have registered them, send their ATED 1 to HMRC and keep a copy for your records."
        doc.getElementById("bullet-3").html() mustBe "form 64-8 does not cover ATED or ATED-related Capital Gains Tax."
      }

      "have the correct link to the ATED 1 form" in {
        doc.getElementById("bullet-1").select("a").attr("href") mustBe "https://www.gov.uk/government/publications/annual-tax-on-enveloped-dwellings-ated-1"
      }

      "have a back link when backLink is defined" in {
        val viewWithBackLink: Html = injectedViewInstanceBeforeRegisteringClient("callingPage", "ATED", "/back-link")
        val docWithBackLink: Document = Jsoup.parse(viewWithBackLink.toString)

        val backLink = docWithBackLink.select("a.govuk-back-link")
        backLink.attr("href") mustBe "/back-link"
        backLink.text() mustBe "Back"
      }

      "have a continue button with correct attributes" in {
        val button = doc.select("button#submit")
        button.text() mustBe "Continue"
        button.attr("type") mustBe "submit"
      }
    }
  }
}