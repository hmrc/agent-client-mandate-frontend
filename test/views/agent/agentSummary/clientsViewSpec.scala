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

package views.agent.agentSummary

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.Mandates
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.FilterClientsForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.agentSummary.clients
import uk.gov.hmrc.domain.{AtedUtr, Generator}
import unit.uk.gov.hmrc.agentclientmandate.builders.AgentBuilder

class clientsViewSpec extends AnyFeatureSpec
  with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with ViewTestHelper with GuiceOneServerPerSuite {

  val registeredAddressDetails: RegisteredAddressDetails = RegisteredAddressDetails("123 Fake Street", "Somewhere", None, None, None, "GB")
  val agentDetails: AgentDetails = AgentBuilder.buildAgentDetails

  val mandateId = "12345678"
  val time1: DateTime = DateTime.now()
  val service = "ATED"
  val atedUtr: AtedUtr = new Generator().nextAtedUtr

  val clientParty: Party = Party("12345678", "test client", PartyType.Individual, ContactDetails("a.a@a.com", None))
  val clientParty1: Party = Party("12345679", "test client1", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty2: Party = Party("12345671", "test client2", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty3: Party = Party("12345671", "test client3", PartyType.Individual, ContactDetails("aa.aa@a.com", None))
  val clientParty4: Party = Party("12345671", "test client4", PartyType.Individual, ContactDetails("aa.aa@a.com", None))

  val mandateNew: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(clientParty1), currentStatus = MandateStatus(Status.New, time1, "credId"), statusHistory = Nil,
    Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name 1")
  val mandateActive: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(clientParty), currentStatus = MandateStatus(Status.Active, time1, "credId"),
    statusHistory = Seq(MandateStatus(Status.New, time1, "credId")),Subscription(None, Service("ated", "ATED")),
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
  val mandateActiveTwo: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(clientParty), currentStatus = MandateStatus(Status.Active, time1, "credId"),
    statusHistory = Seq(MandateStatus(Status.New, time1, "credId")),Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name 6")

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val injectedViewInstanceClients: clients = app.injector.instanceOf[views.html.agent.agentSummary.clients]

  Feature("The agent can view the agent summary page when they have active clients, pending clients and clients to accept") {

    info("as an agent I want to view the correct page content")

    Scenario("agent has visited the page and has current clients, pending clients and clients to accept") {

      Given("An agent visits the page and has active clients, pending clients and clients to accept")
      When("The agent views the mandates")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val activeMandates = Seq(mandateActive, mandateActiveTwo)
      val pendingMandates = Seq(mandateNew, mandateApproved, mandatePendingActivation, mandatePendingCancellation)

      val html = injectedViewInstanceClients("ATED", Mandates(activeMandates, pendingMandates), agentDetails, None, "", filterClientsForm)

      val document = Jsoup.parse(html.toString())
      Then("The title should match - client.summary.title - GOV.UK")
      assert(document.title() === "client.summary.title - GOV.UK")

      And ("The h1 should be ATED clients")
      assert(document.getElementsByTag("h1"). text() === "client.summary.title")

      And ("The caption should be this is for: Org Name")
      assert(document.getElementsByClass("govuk-caption-l"). text() === "ated.screen-reader.name Org Name")

      And("The Current Clients tab - should exist and have 2 item")
      assert(document.getElementById("active-mandate-tab").text === "client.summary.client-active.title")
      assert(document.getElementById("client-name-0").text() === "client display name 2")

      assert(document.getElementById("edit-client-link-0").text() === "client.summary.client-change client.summary.client-edit-details-for client display name 2")
      assert(document.getElementById("client-link-0").text() === "client.summary.client-view client.summary.client-details-for client display name 2")

      assert(document.getElementById("client-name-1").text() === "client display name 6")
      assert(document.getElementById("edit-client-link-1").text() === "client.summary.client-change client.summary.client-edit-details-for client display name 6")
      assert(document.getElementById("client-link-1").text() === "client.summary.client-view client.summary.client-details-for client display name 6")

      And("The Pending Clients tab - should exist and have 3 item")
      assert(document.getElementById("pending-mandate-tab").text() === "client.summary.client-pending.title")

      assert(document.getElementById("pending-client-data-0").text() === "client display name 1")
      assert(document.getElementById("pending-client-status-0").text() === "client.summary.client.pending-status")
      assert(document.getElementById("edit-pending-client-link-0").text() === "client.summary.client-change client.summary.client-edit-details-forclient display name 1")

      assert(document.getElementById("pending-client-data-1").text() === "client display name 5")
      assert(document.getElementById("pending-client-status-1").text() === "client.summary.client-pending")
      assert(document.getElementById("edit-pending-client-link-1").text() === "client.summary.client-change client.summary.client-edit-details-forclient display name 5")

      assert(document.getElementById("pending-client-data-2").text() === "client display name 4")
      assert(document.getElementById("pending-client-status-2").text() === "client.summary.client-pending")
      assert(document.getElementById("edit-pending-client-link-2").text() === "client.summary.client-change client.summary.client-edit-details-forclient display name 4")


      And("The Accept Clients tab - should exist and have 1 item")
      assert(document.getElementById("accept-clients-tab").text() === "client.summary.client-accept.title")

      assert(document.getElementById("accept-client-data-0").text() === "client display name 3")
      assert(document.getElementById("accept-client-link-0").text() === "client.summary.client-accept client display name 3")
      assert(document.getElementById("reject-client-link-0").text() === "client.summary.client-reject client display name 3")

      And("The Add Client Button - should not exist")
      assert(document.getElementById("add-client-btn") === null)
      And("The Add Client Link - should exist")
      assert(document.getElementById("add-client-link").text() === "client.summary.add-client")
    }

    Scenario("agent has visited the page and has clients but no pending clients") {

      Given("An agent visits the page and has current clients but no pending clients")
      When("The agent views the mandates")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val activeMandates = Seq(mandateActive)

      val html = injectedViewInstanceClients("ATED", Mandates(activeMandates, Nil), agentDetails, None, "", filterClientsForm)

      val document = Jsoup.parse(html.toString())
      Then("The title should match - client.summary.title - GOV.UK")
      assert(document.title() === "client.summary.title - GOV.UK")

      And("The Current Clients tab - should exist and have 1 item")
      assert(document.getElementById("active-mandate-tab").text === "client.summary.client-active.title")
      assert(document.getElementById("client-name-0").text() === "client display name 2")

      And("The Pending Clients tab - should exist and have 0 item")
      assert(document.getElementById("pending-mandate-tab").text() === "client.summary.client-pending.title")
      assert(document.getElementById("no-pending-clients").text() === "client.summary.no.pending.clients")

      And("The Accept Clients tab - should exist and have 0 item")
      assert(document.getElementById("accept-clients-tab").text() === "client.summary.client-accept.title")
      assert(document.getElementById("no-accept-clients").text() === "client.summary.no.accept.clients")

      And("The Current Clients summary list - has the correct data and View link")
      assert(document.getElementById("client-name-0").text === "client display name 2")
      assert(document.getElementById("client-link-0").text === "client.summary.client-view client.summary.client-details-for client display name 2")

      And("The Add Client Button - should not exist")
      assert(document.getElementById("add-client-btn") === null)
      And("The Add Client Link - should exist")
      assert(document.getElementById("add-client-link").text() === "client.summary.add-client")

    }

    Scenario("agent has visited the page and will filter clients") {

      Given("An agent visits the page and has current clients but no pending clients")
      When("The agent views the mandates")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val activeMandates = Seq(mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive,
        mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive, mandateActive,
        mandateActive, mandateActive)

      val html = injectedViewInstanceClients("ATED", Mandates(activeMandates, Nil), agentDetails, None, "", filterClientsForm)

      val document = Jsoup.parse(html.toString())

      And("The Current Clients tab - should exist and have 15 items")
      assert(document.getElementById("active-mandate-tab").text === "client.summary.client-active.title")

      And("The Pending Clients tab - should exist and have 0 item")
      assert(document.getElementById("pending-mandate-tab").text() === "client.summary.client-pending.title")
      assert(document.getElementById("no-pending-clients").text() === "client.summary.no.pending.clients")

      And("The Accept Clients tab - should exist and have 0 item")
      assert(document.getElementById("accept-clients-tab").text() === "client.summary.client-accept.title")
      assert(document.getElementById("no-accept-clients").text() === "client.summary.no.accept.clients")

      And("The Current Clients summary list - has the correct data and View link")
      assert(document.getElementById("client-name-0").text === "client display name 2")
      assert(document.getElementById("client-link-0").text === "client.summary.client-view client.summary.client-details-for client display name 2")

      And("The Add Client Button - should not exist")
      assert(document.getElementById("add-client-btn") === null)
      And("The Add Client Link - should exist")
      assert(document.getElementById("add-client-link").text() === "client.summary.add-client")

      And("The filter box should exist")
      assert(document.getElementById("filter-clients").text === "client.summary.filter-clients")
    }

    Scenario("agent has visited the page and has filtered current clients but there no results") {

      Given("An agent visits the page and has current clients but no pending clients")
      When("The agent views the mandates")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val html = injectedViewInstanceClients("ATED", Mandates(Nil, Nil), agentDetails, None, "", filterClientsForm, isUpdate = true)

      val document = Jsoup.parse(html.toString())

      And("The Clients tab - should exist and have 0 items")
      assert(document.getElementById("active-mandate-tab").text === "client.summary.client-active.title")
      assert(document.getElementById("no-active-clients").text() === "client.summary.no.current.clients")

      And("I should not see the clients cancelled panel")
      assert(document.getElementById("client-cancelled-title") === null)

      And("The Add Client Button - should not exist")
      assert(document.getElementById("add-client-btn") === null)
      And("The Add Client Link - should exist")
      assert(document.getElementById("add-client-link").text() === "client.summary.add-client")

      And("The filter box should exist")
      assert(document.getElementById("filter-clients").text === "client.summary.filter-clients")
    }

    Scenario("agent visits summary page with clients cancelled in last 28 days") {
      Given("agent visits page and client has cancelled mandate")
      When("agent views the mandates")

      val html = injectedViewInstanceClients("ATED", Mandates(Nil, Nil), agentDetails, Some(List("AAA")), "", filterClientsForm, isUpdate = true)
      val document = Jsoup.parse(html.toString())

      Then("I should see the clients cancelled panel")
      assert(document.getElementById("client-cancelled-title").text === "client.summary.client-cancelled.text")

      And("I should see the name of the client")
      assert(document.getElementById("client-cancelled-name-0").text === "AAA")
    }
  }
}
