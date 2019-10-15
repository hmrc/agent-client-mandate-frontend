
package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor, urlMatching, urlPathMatching}
import helpers.{AgentBusinessUtrGenerator, IntegrationSpec}
import play.api.http.{HeaderNames => HN}
import play.api.libs.ws.WSResponse
import play.api.mvc.Headers
import uk.gov.hmrc.http.HeaderNames

class UniqueAgentReferenceControllerISpec extends IntegrationSpec {

  "/agent/unique-reference" should {
    "retrieve the unique agent reference" when {
      "a unique reference is available" in {
        val agentRefNo = new AgentBusinessUtrGenerator().nextAgentBusinessUtr

        stubFor(post(urlMatching("/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(200)
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
              .withStatus(200)
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