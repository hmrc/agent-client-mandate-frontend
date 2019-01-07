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

package views.agent.agentSummary

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.Mandates
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.FilterClientsForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.domain.Generator
import unit.uk.gov.hmrc.agentclientmandate.builders.AgentBuilder

class noClientsNoPendingViewSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  val registeredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails = AgentBuilder.buildAgentDetails

  val mandateId = "12345678"
  val time1 = DateTime.now()
  val service = "ATED"
  val atedUtr = new Generator().nextAtedUtr

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The agent can view the agent summary page but they have no clients and no pending clients") {

    info("as an agent I want to view the correct page content")

    scenario("agent has visited the page but has no clients or pending clients") {

      Given("An agent visits the page and has no mandates")
      When("The agent views the empty page")

      val html = views.html.agent.agentSummary.noClientsNoPending("ATED", agentDetails, None)

      val document = Jsoup.parse(html.toString())
      Then("The title should match - ATED clients - GOV.UK")
      assert(document.title() === "ATED clients - GOV.UK")

      And("I should not see the clients cancelled panel")
      assert(document.getElementById("client-cancelled-title") === null)

      And("The Pre Header should be the agents name - ABC Ltd.")
      assert(document.getElementById("pre-header").text() === "this is for: Org Name")

      And("The Add Client Button - should exist")
      assert(document.getElementById("add-client-btn").text() === "Add a client")

      And("The Add Client Link - should not exist")
      assert(document.getElementById("add-client-link") === null)

      And("The sign out link should return to ATED")
      assert(document.getElementById("logOutNavHref").attr("href") === ("http://localhost:9916/ated/logout"))
    }

    scenario("agent visits summary page with clients cancelled in last 28 days") {
      Given("agent visits page and client has cancelled mandate")
      When("agent views the mandates")

      val html = views.html.agent.agentSummary.noClientsNoPending("ATED", agentDetails, Some(List("AAA")))
      val document = Jsoup.parse(html.toString())

      Then("I should see the clients cancelled panel")
      assert(document.getElementById("client-cancelled-title").text === "Your authority to act for these clients has been removed:")

      And("I should see the name of the client")
      assert(document.getElementById("client-cancelled-name-0").text === "AAA")
    }
  }
}
