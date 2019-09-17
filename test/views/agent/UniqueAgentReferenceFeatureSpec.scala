/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientMandateDisplayDetails
import uk.gov.hmrc.agentclientmandate.views


class UniqueAgentReferenceFeatureSpec extends FeatureSpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The user can view the reject client page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page from ated") {

      Given("A user visits the page from ated")
      When("The user views the page from ated")

      val mandateId = "ABC123"
      val agentLastUsedEmail = "a.b@mail.com"
      val clientDisplayDetails = ClientMandateDisplayDetails("test name", mandateId,agentLastUsedEmail)
      val html = views.html.agent.uniqueAgentReference(clientDisplayDetails,  "ated")

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Your unique agent reference is ABC123 - GOV.UK")
      assert(document.title() === "Your unique authorisation number is ABC123 - GOV.UK")
      And("The banner text is - Your unique authorisation number for test name is ABC123")
      assert(document.getElementById("banner-text").text() === "Your unique authorisation number for test name is ABC123")
      And("The screen text is - What you must do next")
      assert(document.getElementById("what-you-must-do").text() === "What you must do next")

      And("The agents instructions")
      assert(document.getElementById("agent-instruction-1").text() === "You must give your client the unique authorisation number ABC123 so they can authorise you to act for them. This number is available from your list of clients.")
      assert(document.getElementById("agent-instruction-2").text() === "When your client authorises you, we will send you an email. You must sign in and accept their request within 28 days or you will need to start from the beginning.")

      And("The client-instruction - should be correct for the relevant service")
      assert(document.getElementById("tell-your-client").text() === "What to tell your client")
      assert(document.getElementById("agent.unique-reference.details.text.1").text() === "Sign in to your organisation Government Gateway account. If you do not have an account, create a Government Gateway account.")
      assert(document.getElementById("agent.unique-reference.details.text.2").text() === "When you are signed in, register for ATED using your registered name and Unique Taxpayer Reference (UTR).")
      assert(document.getElementById("agent.unique-reference.details.text.3").text() === "Add your or your agent’s contact email address.")
      assert(document.getElementById("agent.unique-reference.details.text.4").text() === "Enter the unique authorisation number ABC123.")

      And("The submit : View all my clients has the correct link")
      assert(document.getElementById("submit").text() === "View all my clients")

    }
  }
}
