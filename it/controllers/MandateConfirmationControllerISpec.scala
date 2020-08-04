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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.{AgentBusinessUtrGenerator, IntegrationSpec}
import org.joda.time.DateTime
import play.api.http.{HeaderNames => HN}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.http.HeaderNames
import play.api.http.Status.OK

class MandateConfirmationControllerISpec extends IntegrationSpec {

  val mandate: Mandate =
    Mandate(
      id = "ABC123",
      createdBy = User("cerdId", "Joe Bloggs"),
      agentParty = Party("ated-ref-no", "name", `type` = PartyType.Organisation, contactDetails = ContactDetails("aa@aa.com", None)),
      clientParty = Some(Party("client-id", "client name", `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None))),
      currentStatus = MandateStatus(status = Status.New, DateTime.now(), updatedBy = ""),
      statusHistory = Nil,
      subscription = Subscription(referenceNumber = None, service = Service(id = "ated-ref-no", name = "ated")),
      clientDisplayName = "client display name"
    )

  "/agent/unique-reference" should {
    "retrieve the unique agent reference" when {
      "a unique reference is available" in {
        val agentRefNo = new AgentBusinessUtrGenerator().nextAgentBusinessUtr
        val userId = "/foo/bar"

        stubFor(post(urlMatching("/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   | "optionalCredentials": {
                   |    "providerId": "12345-credId",
                   |    "providerType": "GovernmmentGateway"
                   | }
                   |}""".stripMargin
              )
          )
        )

        val mandateJson: String = Json.toJson(mandate).toString()

        stubFor(get(urlPathMatching(s"/keystore/agent-client-mandate-frontend/$SessionId"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   | "id" : "$SessionId",
                   | "data" : {
                   |    "client-approved" : $mandateJson
                   | }
                   |}""".stripMargin
              )
          )
        )

        val result: WSResponse = await(hitApplicationEndpoint("/client/confirmation")
          .withHttpHeaders(HN.SET_COOKIE -> getSessionCookie())
          .withHttpHeaders(HeaderNames.xSessionId -> s"$SessionId")
          .get())

        result.status mustBe 200
      }
    }
  }

}