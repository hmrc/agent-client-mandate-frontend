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
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Format
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientMandateDisplayDetails

class UniqueAgentReferenceControllerISpec
    extends IntegrationSpec
    with MandateConstants {

  private def stubAuthorisedAgent(): Unit = {
    val agentRefNo = new AgentBusinessUtrGenerator().nextAgentBusinessUtr

    stubFor(
      post(urlMatching("/auth/authorise"))
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
                 |  "optionalCredentials": {
                 |    "providerId": "12345-credId",
                 |    "providerType": "GovernmentGateway"
                 |  },
                 |  "internalId": "internal id"
                 | }""".stripMargin
            )
        )
    )
  }

  "/agent/unique-reference" should {
    "retrieve the unique agent reference" when {
      "a unique reference is available" in {
        clearSessionCache()
        stubAuthorisedAgent()

        val cachedMandateDetails = ClientMandateDisplayDetails(
          name = "name",
          mandateId = "mandateId",
          agentLastUsedEmail = "agentLastUsedEmail"
        )

        await(
          dataCacheService.cacheFormData[ClientMandateDisplayDetails](
            agentRefCacheId,
            cachedMandateDetails
          )(
            cacheHeaderCarrier,
            ec,
            implicitly[Format[ClientMandateDisplayDetails]]
          )
        )

        val result: WSResponse = await(
          hitApplicationEndpoint("/agent/unique-reference").get()
        )

        result.status mustBe OK
      }
    }

    "redirect" when {
      "a unique reference is not available in the cache" in {
        clearSessionCache()
        stubAuthorisedAgent()

        val result: WSResponse = await(
          hitApplicationEndpoint("/agent/unique-reference").get()
        )

        result.status mustBe SEE_OTHER
      }
    }
  }
}
