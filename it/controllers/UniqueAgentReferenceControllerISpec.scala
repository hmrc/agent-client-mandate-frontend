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
import play.api.http.Status.OK
import play.api.http.{HeaderNames => HN}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HeaderNames

class UniqueAgentReferenceControllerISpec extends IntegrationSpec {

  "/agent/unique-reference" should {
    "retrieve the unique agent reference" when {
      "a unique reference is available" in {
        val agentRefNo = new AgentBusinessUtrGenerator().nextAgentBusinessUtr

        stubFor(post(urlMatching("/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   | "authorisedEnrolments" : [{
                   |    "key": "HMRC-AGENT-AGENT",
                   |    "identifiers": [{ "key": "AgentRefNumber", "value": "$agentRefNo" }]
                   |  }],
                   |  "agentInformation": {
                   |    "agentCode" : "M4T81X",
                   |    "agentFriendlyName" : "Mr Anderson",
                   |    "agentId": "NE0"
                   |  },
                   |  "agentCode": "M4T81X",
                   |  "optionalCredentials": {"providerId": "12345-credId", "providerType": "GovernmmentGateway"},
                   |  "internalId": "internal id"
                   | }""".stripMargin
              )
          )
        )

        stubFor(get(urlPathMatching(s"/keystore/agent-client-mandate-frontend/$SessionId"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   | "id" : "$SessionId",
                   | "data" : {
                   |    "agent-ref-id" : {
                   |      "name": "name",
                   |      "mandateId": "mandateId",
                   |      "agentLastUsedEmail": "agentLastUsedEmail"
                   |    }
                   | }
                   |}""".stripMargin
              )
          )
        )

        val result: WSResponse = await(hitApplicationEndpoint("/agent/unique-reference")
          .withHeaders(HN.SET_COOKIE -> getSessionCookie())
          .withHeaders(HeaderNames.xSessionId -> s"$SessionId")
          .withHeaders("Authorization" -> "value")
          .get())

        result.status mustBe 200
      }
    }
  }

}