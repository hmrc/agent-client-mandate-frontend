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
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.rejectClient

class RejectClientFeatureSpec extends AnyFeatureSpec
  with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with ViewTestHelper with GuiceOneServerPerSuite {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val injectedViewInstanceRejectClient: rejectClient = app.injector.instanceOf[views.html.agent.rejectClient]

  Feature("The user can view the reject client page") {

    info("as a user I want to view the correct page content")

    Scenario("user has visited the page") {

      Given("A user visits the page")
      When("The user views the page")

      val html = injectedViewInstanceRejectClient("ATED", new YesNoQuestionForm("agent.reject-client.error")
        .yesNoQuestionForm, "ACME Limited", "", Some("http://"))

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Are you sure you want to reject the request from this client? - GOV.UK - service.name")
      assert(document.title() === "agent.reject-client.title - GOV.UK - service.name")
      And("The pre-header text is - Manage your ATED service")
      assert(document.getElementsByTag("h1").text().contains("ated.screen-reader.section agent.edit-mandate-details.pre-header"))
      And("The header text is - Are you sure you want to reject the request from ACME Limited?")
      assert(document.getElementsByTag("h1").text() contains "agent.reject-client.header")

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
