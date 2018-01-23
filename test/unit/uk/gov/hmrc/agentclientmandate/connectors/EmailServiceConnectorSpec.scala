/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.uk.gov.hmrc.agentclientmandate.connectors

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.{EmailServiceConnector, EmailToValidate}
import uk.gov.hmrc.http.{CorePost, HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class EmailServiceConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "EmailServiceConnector" must {

    "know the service url to connect to" when {

      "trying to connect to Email service" in {
        TestEmailServiceConnector.serviceUrl must be("http://localhost:8300")
      }

    }

    "return HttpResponse" when {
      "email service responds with a HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockWSHttp.POST[EmailToValidate, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(responseJson))))
        val result = await(TestEmailServiceConnector.validate("aa@mail.com"))
        result.status must be(OK)
      }
    }

  }

  trait MockedVerbs extends CorePost
  val mockWSHttp: CorePost = mock[MockedVerbs]

  override def beforeEach(): Unit = {
    reset(mockWSHttp)
  }

  val responseJson = Json.parse("""{"valid": true}""")

  object TestEmailServiceConnector extends EmailServiceConnector {
    override val serviceUrl = baseUrl("email")
    override val http = mockWSHttp
  }

}
