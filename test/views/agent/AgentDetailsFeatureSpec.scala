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
import uk.gov.hmrc.agentclientmandate.views
import unit.uk.gov.hmrc.agentclientmandate.builders.AgentBuilder

class AgentDetailsFeatureSpec extends FeatureSpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  implicit val request = FakeRequest()
  implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

  feature("The user can view the agent details page") {

    info("as a user I want to view the correct page content")

    scenario("user has visited the page") {

      Given("A user visits the page")
      When("The user views the page")

      val html = views.html.agent.agentDetails(AgentBuilder.buildAgentDetails, "service", Some("http://"))

      val document = Jsoup.parse(html.toString())
      Then("The title should match - Your details - GOV.UK")
      assert(document.title() === "Your details - GOV.UK")

      And("The pre-header text is - Edit details")
      assert(document.getElementById("pre-header").text() === "This section is: Edit details")
      And("The header text is - Your details")
      assert(document.getElementById("agency-details-header").text() === "Your details")

      When("The user views the table of information")

      Then("The agency name header text is - Business Name]")
      assert(document.getElementById("agency-name-header").text() === "Business Name")

      And("The agency name is - Org Name")
      assert(document.getElementById("agency-name-val").text() === "Org Name")

      And("The agency address header is - Address")
      assert(document.getElementById("agency-address-header").text() === "Address")

      And("The agency address details shows - address1 address2 FR")
      assert(document.getElementById("agency-address-val").text() === "address1 address2 FR")

    }


  }
}
