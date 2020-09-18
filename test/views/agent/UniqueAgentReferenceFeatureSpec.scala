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

package views.agent

import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientMandateDisplayDetails
import uk.gov.hmrc.agentclientmandate.views
import org.scalatestplus.play.guice.GuiceOneServerPerSuite


class UniqueAgentReferenceFeatureSpec extends FeatureSpec  with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with ViewTestHelper with GuiceOneServerPerSuite {

  implicit val request = FakeRequest()
  val injectedViewInstanceUniqueAgentReference = app.injector.instanceOf[views.html.agent.uniqueAgentReference]

  feature("The user can view the reject client page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page from ated") {

      Given("A user visits the page from ated")
      When("The user views the page from ated")

      val mandateId = "ABC123"
      val agentLastUsedEmail = "a.b@mail.com"
      val clientDisplayDetails = ClientMandateDisplayDetails("test name", mandateId,agentLastUsedEmail)
      val html = injectedViewInstanceUniqueAgentReference(clientDisplayDetails,  "ated")

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Your unique agent reference is ABC123 - GOV.UK")
      assert(document.title() === "agent.unique-reference.title - GOV.UK")
      And("The banner text is - Your unique authorisation number for test name is ABC123")
      assert(document.getElementById("banner-text").text() === "agent.unique-reference.header")
      And("The screen text is - What you must do next")
      assert(document.getElementById("what-you-must-do").text() === "agent.unique-reference.next.heading.text")

      And("The agents instructions")
      assert(document.getElementById("agent-instruction-1").text() === "agent.unique-reference.do-next1")
      assert(document.getElementById("agent-instruction-2").text() === "agent.unique-reference.do-next2")

      And("The client-instruction - should be correct for the relevant service")
      assert(document.getElementById("tell-your-client").text() === "agent.unique-reference.tell-client")
      assert(document.getElementById("agent.unique-reference.details.text.2").text() === "agent.unique-reference.details.text.2")
      assert(document.getElementById("agent.unique-reference.details.text.1").text() === "agent.unique-reference.details.text.1")
      assert(document.getElementById("agent.unique-reference.details.text.3").text() === "agent.unique-reference.details.text.3")
      assert(document.getElementById("agent.unique-reference.details.text.4").text() === "agent.unique-reference.details.text.4")

      And("The submit : View all my clients has the correct link")
      assert(document.getElementById("submit").text() === "agent.unique-reference.button")

    }
  }
}
