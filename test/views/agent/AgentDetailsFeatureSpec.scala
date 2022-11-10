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
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.agentDetails
import unit.uk.gov.hmrc.agentclientmandate.builders.AgentBuilder

class AgentDetailsFeatureSpec extends AnyFeatureSpec
  with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with ViewTestHelper with GuiceOneServerPerSuite {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val injectedViewInstanceAgentDetails: agentDetails = app.injector.instanceOf[views.html.agent.agentDetails]

  Feature("The user can view the agent details page") {

    info("as a user I want to view the correct page content")

    Scenario("user has visited the page") {

      Given("A user visits the page")
      When("The user views the page")

      val html = injectedViewInstanceAgentDetails(AgentBuilder.buildAgentDetails, "service", Some("http://"))

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Your details - GOV.UK")
      assert(document.title() === "agent.edit-details.title - GOV.UK")

      And("The pre-header text is - Edit details")
      assert(document.getElementsByTag("header").text() contains "ated.screen-reader.section agent.edit-details.sub-header")
      And("The header text is - Your details")
      assert(document.getElementsByTag("header").text() contains "agent.edit-details.header")

      When("The user views the table of information")

      Then("The agency name header text is - Business Name]")
      assert(document.getElementById("agency-name-header").text() === "agent.edit-details.agency.name")

      And("The agency name is - Org Name")
      assert(document.getElementById("agency-name-val").text() === "Org Name")

      And("The agency address header is - Address")
      assert(document.getElementById("agency-address-header").text() === "agent.edit-details.registered.address")

      And("The agency address details shows - address1 address2 FR")
      assert(document.getElementById("agency-address-val").text() === "address1 address2 FR")

    }

  }
}
