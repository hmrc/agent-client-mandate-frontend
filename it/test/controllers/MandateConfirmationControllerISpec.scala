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
import helpers.IntegrationSpec
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Format
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentclientmandate.models._

import java.time.Instant

class MandateConfirmationControllerISpec extends IntegrationSpec {

  val mandate: Mandate =
    Mandate(
      id = "ABC123",
      createdBy = User("cerdId", "Joe Bloggs"),
      agentParty = Party(
        "ated-ref-no",
        "name",
        `type` = PartyType.Organisation,
        contactDetails = ContactDetails("aa@aa.com", None)
      ),
      clientParty = Some(
        Party(
          "client-id",
          "client name",
          `type` = PartyType.Organisation,
          contactDetails = ContactDetails("bb@bb.com", None)
        )
      ),
      currentStatus = MandateStatus(Status.New, Instant.now(), updatedBy = ""),
      statusHistory = Nil,
      subscription = Subscription(
        referenceNumber = None,
        service = Service(id = "ated-ref-no", name = "ated")
      ),
      clientDisplayName = "client display name"
    )

  private def stubAuthorisedClient(): Unit =
    stubFor(
      post(urlMatching("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(
              s"""{
                 | "optionalCredentials": {
                 |   "providerId": "12345-credId",
                 |   "providerType": "GovernmentGateway"
                 | }
                 |}""".stripMargin
            )
        )
    )

  "/client/confirmation" should {
    "retrieve the mandate confirmation" when {
      "a client approved mandate is available" in {
        clearSessionCache()
        stubAuthorisedClient()

        await(
          dataCacheService.cacheFormData[Mandate](
            "client-approved",
            mandate
          )(
            cacheHeaderCarrier,
            ec,
            implicitly[Format[Mandate]]
          )
        )

        val result: WSResponse = await(
          hitApplicationEndpoint("/client/confirmation").get()
        )

        result.status mustBe OK
      }
    }

    "redirect" when {
      "a client approved mandate is not available in the cache" in {
        clearSessionCache()
        stubAuthorisedClient()

        val result: WSResponse = await(
          hitApplicationEndpoint("/client/confirmation").get()
        )

        result.status mustBe SEE_OTHER
      }
    }
  }
}
