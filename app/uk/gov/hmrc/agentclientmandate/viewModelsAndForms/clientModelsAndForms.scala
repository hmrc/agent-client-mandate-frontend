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

import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.models.Mandate

import scala.annotation.tailrec

case class ClientEmail(email: String)

object ClientEmail {
  implicit val formats = Json.format[ClientEmail]
}

object ClientEmailForm {

  val emailLength = 241
  val lengthZero = 0
  val emailRegex =
    """^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$""".r

  def validateEditEmail(f: Form[ClientEmail]): Form[ClientEmail] = {
    def validateEmailRegex(email: String) = {
      val x = emailRegex.findFirstMatchIn(email).exists(_ => true)
      val y = email.length == lengthZero
      val z = email.length > emailLength
      if (x || y || z) {
        f
      } else {
        f.withError((FormError("email", Messages("client.enter-email.error.general.agent-enter-email-form"))))
      }
    }

    if (!f.hasErrors) {
      val email = f.data.get("email").getOrElse("")
      if (email.trim.length == lengthZero) {
        f.withError(FormError("email", Messages("client.enter-email.error.email")))
      }
      else if (email.length > lengthZero && email.length > emailLength) {
        f.withError((FormError("email", Messages("client.enter-email.error.email.max.length"))))
      } else {
        validateEmailRegex(email)
      }
    } else f
  }


  val clientEmailForm =
    Form(
      mapping(
        "email" -> text
          .verifying(Messages("client.collect-email.error.email"), x => x.trim.length > lengthZero)
          .verifying(Messages("client.collect-email.error.email.length"), x => x.isEmpty || (x.nonEmpty && x.length <= emailLength))
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
