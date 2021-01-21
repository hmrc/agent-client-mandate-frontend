/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.uk.gov.hmrc.agentclientmandate.viewModelsAndForms

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{MandateReference, MandateReferenceForm}
import unit.uk.gov.hmrc.agentclientmandate.builders.MockControllerSetup

class ClientModelsAndFormsSpec extends PlaySpec with MockitoSugar with MockControllerSetup {

  "clientAuthNumForm" must {

    "render clientAuthNumForm successfully on entering valid input data" in {
      val form = MandateReferenceForm.clientAuthNumForm.fillAndValidate(MandateReference("B2F06DD4"))
      form.hasErrors must be (false)
      form.errors.size must be (0)
      form.errors must be (List())
      form.data.keys must be (Set("mandateRef"))
      form.value must be (Some(MandateReference("B2F06DD4")))
    }

    "throw error on entering no input data" in {
      val form = MandateReferenceForm.clientAuthNumForm.fillAndValidate(MandateReference(""))
      form.hasErrors must be (true)
      form.errors.size must be (1)
      form.errors.head.message must be ("client.search-mandate.error.clientAuthNum")
    }

    "throw error on entering invalid input data" in {
      val form = MandateReferenceForm.clientAuthNumForm.fillAndValidate(MandateReference("@!@!@!@!@!"))
      form.hasErrors must be (true)
      form.errors.size must be (1)
      form.errors.head.message must be ("client.search-mandate.error.clientAuthNum.length")
    }

  }
}
