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

package unit.uk.gov.hmrc.agentclientmandate.controllers.mappings



import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.validation.{Invalid, Valid}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.mappings.Constraints

class ConstraintsSpec extends WordSpec with MustMatchers with ScalaCheckPropertyChecks with Constraints {

  val maxVal: Int = 10

  "regexp" must {

    "return Valid for an input that matches the expression" in {
      val result = regexp("""^\w+$""", "error.invalid")("foo")
      result mustEqual Valid
    }

    "return Invalid for an input that does not match the expression" in {
      val result = regexp("""^\d+$""", "error.invalid")("foo")
      result mustEqual Invalid("error.invalid")
    }
  }

  "maxLength" must {

    "return Valid for a string shorter than the allowed length" in {
      val result = maxLength(maxVal, "error.length")("a" * 9)
      result mustEqual Valid
    }

    "return Valid for an empty string" in {
      val result = maxLength(maxVal, "error.length")("")
      result mustEqual Valid
    }

    "return Valid for a string equal to the allowed length" in {
      val result = maxLength(maxVal, "error.length")("a" * 10)
      result mustEqual Valid
    }

    "return Invalid for a string longer than the allowed length" in {
      val result = maxLength(maxVal, "error.length")("a" * 11)
      result mustEqual Invalid("error.length")
    }
  }

  "minLength" must {

    "return Valid for a string longer than the allowed length" in {
      val result = minLength(maxVal, "error.length")("abc@aaa.com")
      result mustEqual Valid
    }

    "return inValid for an empty string" in {
      val result = minLength(maxVal, "error.length")("")
      result mustEqual Invalid("error.length")
    }

  }

}