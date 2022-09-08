/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class RejectClientFeatureSpec extends FeatureSpec  with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with ViewTestHelper with GuiceOneServerPerSuite {

  implicit val request = FakeRequest()
  val injectedViewInstanceRejectClient = app.injector.instanceOf[views.html.agent.rejectClient]

  feature("The user can view the reject client page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page") {

      Given("A user visits the page")
      When("The user views the page")

      val html = injectedViewInstanceRejectClient("ATED", new YesNoQuestionForm("agent.reject-client.error")
        .yesNoQuestionForm, "ACME Limited", "", Some("http://"))

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Are you sure you want to reject the request from this client? - GOV.UK")
      assert(document.title() === "agent.reject-client.title - GOV.UK")
      And("The pre-header text is - Manage your ATED service")
      assert(document.getElementById("pre-heading").text() === "ated.screen-reader.section agent.edit-mandate-details.pre-header")
      And("The header text is - Are you sure you want to reject the request from ACME Limited?")
      assert(document.getElementById("heading").text() === "agent.reject-client.header")

      And("The reject text is - Rejecting a client request means you will not be able to act on their behalf unless they submit another request.")
      assert(document.getElementById("reject-text").text() === "agent.reject-client.text")

      And("The yes no radio buttons - exist and are set to Yes and No")
      print(document.getElementById("yesNo"))
      assert(document.getElementById("yesNo").attr("value") === "true")
      assert(document.getElementsByAttributeValue("for", "yesNo").text() === "radio-yes")
      assert(document.getElementById("yesNo-2").attr("value") === "false")
      assert(document.getElementsByAttributeValue("for", "yesNo-2").text() === "radio-no")

      And("The submit button is - Confirm")
      assert(document.getElementById("submit").text() === "confirm-button")

    }
  }
}
