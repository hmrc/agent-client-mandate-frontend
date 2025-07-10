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

class clientPermissionViewSpec extends AnyWordSpec with MockitoSugar with ViewTestHelper with GuiceOneServerPerSuite {

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  override implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  val injectedViewInstanceClientPermission: uk.gov.hmrc.agentclientmandate.views.html.agent.clientPermission_new = app.injector.instanceOf[uk.gov.hmrc.agentclientmandate.views.html.agent.clientPermission_new]

  val view: Html = injectedViewInstanceClientPermission(
    uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientPermissionForm.clientPermissionForm.fill(uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientPermission()),
    "ATED",
    "callingPage",
    Some("/back-link")
  )
  val doc: Document = Jsoup.parse(view.toString)

  "The client Permission view" when {
    "rendered" must {

      "have the correct page title" in {
        doc.title mustBe "Do you have permission to register on behalf of your client? - Submit and view your ATED returns - GOV.UK"
      }

      "have the correct header" in {
        doc.select("h1").text() mustBe "This section is: Add a client Do you have permission to register on behalf of your client?"
      }

      "have a back link when backLink is defined" in {
        val backLink = doc.getElementsByClass("govuk-back-link")
        backLink.attr("href") mustBe "/back-link"
        backLink.text() mustBe "Back"
      }

      "have yes and no radio buttons" in {
        val yesRadio = doc.getElementById("hasPermission")
        yesRadio.attr("type") mustBe "radio"
        yesRadio.attr("name") mustBe "hasPermission"
        yesRadio.attr("value") mustBe "true"

        val noRadio = doc.getElementById("hasPermission-2")
        noRadio.attr("type") mustBe "radio"
        noRadio.attr("name") mustBe "hasPermission"
        noRadio.attr("value") mustBe "false"
      }

      "have a continue button with correct attributes" in {
        val button = doc.getElementById("submit")
        button.text() mustBe "Continue"
        button.attr("type") mustBe "submit"
      }
    }
  }
}
