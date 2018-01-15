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

package unit.uk.gov.hmrc.agentclientmandate.services

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.EmailServiceConnector
import uk.gov.hmrc.agentclientmandate.service.EmailService

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, InternalServerException }

class EmailServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "EmailService" must {

    "use valid email connector" in {
      EmailService.emailServiceConnector must be(EmailServiceConnector)
    }

    "return true" when {
      "valid email is passed" in {
        when(mockEmailConnector.validate(Matchers.eq(validEmail))(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(validEmailResponse))))
        val result = await(TestEmailService.validate(validEmail))
        assert(result, "email was not valid")
      }
    }

    "return false" when {
      "invalid email is passed" in {
        when(mockEmailConnector.validate(Matchers.eq(validEmail))(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(invalidEmailResponse))))
        val result = await(TestEmailService.validate(validEmail))
        assert(!result, "email was valid")
      }
    }

    "throw an error" when {
      "when any other status is returned by email service" in {
        when(mockEmailConnector.validate(Matchers.eq(validEmail))(Matchers.any())) thenReturn {
          Future.successful(HttpResponse(BAD_REQUEST, Some(invalidEmailResponse)))
        }
        val result = TestEmailService.validate(validEmail)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.message must include("[EmailService][validate] - Error validating email")
      }
    }

  }

  val mockEmailConnector = mock[EmailServiceConnector]
  val validEmail = "aa@mail.com"
  val invalidEmail = "aa@invalid.com"
  val validEmailResponse = Json.parse("""{"valid": true}""")
  val invalidEmailResponse = Json.parse("""{"valid": false}""")
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    reset(mockEmailConnector)
  }

  object TestEmailService extends EmailService {
    override val emailServiceConnector = mockEmailConnector
  }

}
