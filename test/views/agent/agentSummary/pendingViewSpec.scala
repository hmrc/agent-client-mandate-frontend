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
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import play.api.i18n.{Lang, Messages}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.Mandates
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.domain.{AtedUtr, Generator}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import unit.uk.gov.hmrc.agentclientmandate.builders.AgentBuilder

class pendingViewSpec extends FeatureSpec  with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with ViewTestHelper {

  val registeredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails: AgentDetails = AgentBuilder.buildAgentDetails

  val mandateId = "12345678"
  val time1: DateTime = DateTime.now()
  val service: String = "ATED"
  val atedUtr: AtedUtr = new Generator().nextAtedUtr

  val clientParty = Party("12345678", "test client", PartyType.Individual, ContactDetails("a.a@a.com", None))
  val clientParty1 = Party("12345679", "test client1", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty2 = Party("12345671", "test client2", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty3 = Party("12345671", "test client3", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty4 = Party("12345671", "test client4", PartyType.Individual, ContactDetails("aa.aa@a.com", None))

  val mandateNew: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = None, currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Nil,
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 1")
  val mandateActive: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(clientParty), currentStatus = MandateStatus(Status.Active, time1, "credId"),
    statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name 2")
  val mandateApproved: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(clientParty3), currentStatus = MandateStatus(Status.Approved, time1, "credId"),
    statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name 3")
  val mandatePendingCancellation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123458", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(clientParty4), currentStatus = MandateStatus(Status.PendingCancellation, time1, "credId"),
    statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name 4")
  val mandatePendingActivation: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123451", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(clientParty2), currentStatus = MandateStatus(Status.PendingActivation, time1, "credId"),
    statusHistory = Seq(MandateStatus(Status.New, time1, "credId")), Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name 5")

  implicit val request = FakeRequest()

  feature("The agent can view the agent summary page when they only have pending clients") {

    info("as an agent I want to view the correct page content")

    scenario("agent has visited the page and has no clients but has pending clients") {

      Given("An agent visits the page and has no clients but has pending clients")
      When("The agent views the mandates")
      implicit val request = FakeRequest()

      val pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)

      val html = views.html.agent.agentSummary.pending("ATED", Mandates(Nil, pendingMandates), agentDetails, None, "")

      val document = Jsoup.parse(html.toString())
      Then("The title should match - client.summary.title - GOV.UK")
      assert(document.title() === "client.summary.title - GOV.UK")

      And("I should not see the clients cancelled panel")
      assert(document.getElementById("client-cancelled-title") === null)
      
      And("The Pending Clients tab - should exist")
      assert(document.getElementById("pending-clients").text === "client.summary.client-pending.title selected")




      And("The Add Client Button - should not exist")
      assert(document.getElementById("add-client-btn") === null)
      And("The Add Client Link - should exist")
      assert(document.getElementById("add-client-link").text() === "client.summary.add-client")
    }

    scenario("agent visits summary page with clients cancelled in last 28 days") {
      Given("agent visits page and client has cancelled mandate")
      When("agent views the mandates")

      val pendingMandates = Seq(mandateNew, mandatePendingActivation, mandateApproved, mandatePendingCancellation)
      val html = views.html.agent.agentSummary.pending("ATED", Mandates(Nil, pendingMandates), agentDetails, Some(List("AAA")), "")
      val document = Jsoup.parse(html.toString())

      Then("I should see the clients cancelled panel")
      assert(document.getElementById("client-cancelled-title").text === "client.summary.client-cancelled.text")

      And("I should see the name of the client")
      assert(document.getElementById("client-cancelled-name-0").text === "AAA")
    }
  }
}
