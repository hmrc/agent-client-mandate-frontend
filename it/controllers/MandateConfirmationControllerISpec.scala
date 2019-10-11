
package controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.{AgentBusinessUtrGenerator, IntegrationSpec}
import org.joda.time.DateTime
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentclientmandate.models.{ContactDetails, Mandate, MandateStatus, Party, PartyType, Service, Status, Subscription, User}

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
              .withStatus(200)
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
              .withStatus(200)
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
          .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
          .get())

        result.status mustBe 200
      }
    }
  }

}