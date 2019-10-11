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
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views

class RejectClientFeatureSpec extends FeatureSpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The user can view the reject client page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page") {

      Given("A user visits the page")
      When("The user views the page")

      val html = views.html.agent.rejectClient("ATED", new YesNoQuestionForm("agent.reject-client.error").yesNoQuestionForm, "ACME Limited", "", Some("http://"))

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Are you sure you want to reject the request from this client? - GOV.UK")
      assert(document.title() === "Are you sure you want to reject the request from this client? - GOV.UK")
      And("The pre-header text is - Manage your ATED service")
      assert(document.getElementById("pre-heading").text() === "This section is: Manage your ATED service")
      And("The header text is - Are you sure you want to reject the request from ACME Limited?")
      assert(document.getElementById("heading").text() === "Are you sure you want to reject the request from ACME Limited?")

      And("The reject text is - Rejecting a client request means you will not be able to act on their behalf unless they submit another request.")
      assert(document.getElementById("reject-text").text() === "Rejecting a client request means you will not be able to act for them unless they submit another request.")

      And("The yes no radio buttons - exist and are set to Yes and No")
      assert(document.getElementById("yesNo-true").attr("value") === "true")
      assert(document.getElementById("yesNo-true_field").text() === "Yes")
      assert(document.getElementById("yesNo-false").attr("value") === "false")
      assert(document.getElementById("yesNo-false_field").text() === "No")

      And("The submit button is - Confirm")
      assert(document.getElementById("submit").text() === "Confirm")

    }
  }
}
