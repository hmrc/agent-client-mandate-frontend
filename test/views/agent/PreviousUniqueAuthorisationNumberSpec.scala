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

package views.agent

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import unit.uk.gov.hmrc.agentclientmandate.builders.TestApplicationBuilder
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevUniqueAuthNum
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.previousUniqueAuthorisationNumber
import unit.uk.gov.hmrc.agentclientmandate.builders.TestApplicationBuilder

class PreviousUniqueAuthorisationNumberSpec extends AnyWordSpec with MockitoSugar with ViewTestHelper with GuiceOneServerPerSuite with TestApplicationBuilder {

  given appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  given specMessages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  val injectedViewInstancePrevUniqueAuthNum: previousUniqueAuthorisationNumber = app.injector.instanceOf[uk.gov.hmrc.agentclientmandate.views.html.agent.previousUniqueAuthorisationNumber]

  val view: Html = injectedViewInstancePrevUniqueAuthNum(
    uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevUniqueAuthNumForm.prevUniqueAuthNumForm.fill(uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevUniqueAuthNum()),
    "ATED",
    "callingPage",
    Some("/back-link")
  )
  val doc: Document = Jsoup.parse(view.toString)

  "The Previous unique Authorisation Number view" when {
    "rendered" must {

      "have the correct page title" in {
        doc.title mustBe "Previous unique authorisation number - Submit and view your ATED returns - GOV.UK"
      }

      "have the correct header and subheader" in {
        doc.select("h1").text() mustBe "Previous unique authorisation number"
        doc.select("#subheader").text() mustBe "This section is: Add a client"
      }

      "have the correct paragraph text" in {
        doc.select("#agent-p1").text() mustBe "You will need the clients unique authorisation number (UAN) to access the clients ATED account."
        doc.select("#agent-p2").text() mustBe "Their previous agent will have this number. You will not be able to enter this number at a later date."
        doc.select("#agent-p3").text() mustBe "Their previous agent can find the UAN on the client record on their ATED account. They need to select ‘edit link’ next to the client's name, which shows the number at the top."
        doc.select("#agent-p4").text() mustBe "To allow your client to transfer to you, the previous agent needs to select 'remove this client'. Until they do this, your client will show as 'client pending'."
      }

      "have a back link when backLink is defined" in {
        val backLink = doc.getElementsByClass("govuk-back-link")
        backLink.attr("href") mustBe "/back-link"
        backLink.text() mustBe "Back"
      }

      "have yes and no radio buttons" in {
        val yesRadio = doc.getElementById("yesRadioButton")
        yesRadio.attr("type") mustBe "radio"
        yesRadio.attr("name") mustBe "authNum"
        yesRadio.attr("value") mustBe "true"

        val noRadio = doc.getElementById("noRadioButton")
        noRadio.attr("type") mustBe "radio"
        noRadio.attr("name") mustBe "authNum"
        noRadio.attr("value") mustBe "false"
      }

      "have a continue button with correct attributes" in {
        val button = doc.getElementById("submit")
        button.text() mustBe "Save and continue"
        button.attr("type") mustBe "submit"
      }
    }
  }
}
