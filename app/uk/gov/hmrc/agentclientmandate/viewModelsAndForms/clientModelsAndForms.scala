/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.viewModelsAndForms

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.agentclientmandate.models.Mandate
import uk.gov.hmrc.agentclientmandate.utils.AgentClientMandateUtils.{emailRegex, maximumEmailLength, minimumEmailLength}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.mappings.Constraints

case class ClientEmail(email: String)

object ClientEmail {
  implicit val formats: OFormat[ClientEmail] = Json.format[ClientEmail]
}

object ClientEmailForm extends Constraints {
  val clientEmailForm =
    Form(
      mapping(
        "email" -> text
          .verifying(regexp(emailRegex, "client.email.error.email.invalid"))
          .verifying(minLength(minimumEmailLength, "client.email.error.email.empty"))
          .verifying(maxLength(maximumEmailLength, "client.email.error.email.too.long"))
      )(ClientEmail.apply)(ClientEmail.unapply)
    )

}
    case class MandateReference(mandateRef: String)

    object MandateReference {
      implicit val formats: OFormat[MandateReference] = Json.format[MandateReference]
    }

    object MandateReferenceForm {

      val mandateRefLength = 8

      def mandateRefForm =
        Form(
          mapping(
            "mandateRef" -> text.transform[String](a => a.trim.replaceAll("\\s+", ""), a => a.trim.replaceAll("\\s+", "").toUpperCase)
              .verifying("client.search-mandate.error.mandateRef", x => x.nonEmpty)
              .verifying("client.search-mandate.error.mandateRef.length", x => x.isEmpty || (x.nonEmpty && x.length <= mandateRefLength))
          )
          (MandateReference.apply)(MandateReference.unapply)
        )
    }

    case class ClientCache(
                            email: Option[ClientEmail] = None,
                            mandate: Option[Mandate] = None
                          )

    object ClientCache {
      implicit val formats: OFormat[ClientCache] = Json.format[ClientCache]
      }
