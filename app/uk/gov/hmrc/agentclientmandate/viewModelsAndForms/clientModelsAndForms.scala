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

import forms.mappings.Constraints
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.models.Mandate
import uk.gov.hmrc.agentclientmandate.utils.AgentClientMandateUtils.{emailRegex, maximumEmailLength, minimumEmailLength}

import scala.annotation.tailrec

case class ClientEmail(email: String)

object ClientEmail {
  implicit val formats = Json.format[ClientEmail]
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
      implicit val formats = Json.format[MandateReference]
    }

    object MandateReferenceForm {

      val mandateRefLength = 8

      val mandateRefForm =
        Form(
          mapping(
            "mandateRef" -> text.transform[String](a => a.trim.replaceAll("\\s+", ""), a => a.trim.replaceAll("\\s+", "").toUpperCase)
              .verifying(Messages("client.search-mandate.error.mandateRef"), x => x.nonEmpty)
              .verifying(Messages("client.search-mandate.error.mandateRef.length"), x => x.isEmpty || (x.nonEmpty && x.length <= mandateRefLength))
          )
          (MandateReference.apply)(MandateReference.unapply)
        )
    }

    case class ClientCache(
                            email: Option[ClientEmail] = None,
                            mandate: Option[Mandate] = None
                          )

    object ClientCache {
      implicit val formats = Json.format[ClientCache]
      }
