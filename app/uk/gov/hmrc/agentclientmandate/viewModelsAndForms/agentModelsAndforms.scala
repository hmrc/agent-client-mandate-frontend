/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.libs.json.Json

import scala.annotation.tailrec

case class AgentSelectService(service: Option[String] = None)

object AgentSelectServiceForm {
  val selectServiceForm =
    Form(
      mapping(
        "service" -> optional(text).verifying(Messages("agent.select-service.error.service"), serviceOpt => serviceOpt.isDefined)
      )(AgentSelectService.apply)(AgentSelectService.unapply)
    )
}

case class AgentEmail(email: String, confirmEmail: String)

object AgentEmail {
  implicit val formats = Json.format[AgentEmail]
}

object AgentEmailForm {
  val agentEmailForm =
    Form(
      mapping(
        "email" -> text.verifying(Messages("agent.enter-email.error.email"), email => email.nonEmpty),
        "confirmEmail" -> text.verifying(Messages("agent.enter-email.error.confirmEmail"), email => email.nonEmpty)
      )(AgentEmail.apply)(AgentEmail.unapply)
    )

  def validateConfirmEmail(emailForm: Form[AgentEmail]): Form[AgentEmail] = {
    def validate = {
      val email = emailForm.data.get("email").map(_.trim)
      val confirmEmail = emailForm.data.get("confirmEmail").map(_.trim)
      (email, confirmEmail) match {
        case (Some(e1), Some(e2)) if e1 == e2 => Seq()
        case (Some(e1), Some(e2)) => Seq(Some(FormError("confirmEmail", Messages("agent.enter-email.error.confirm-email.not-equal"))))
        case _ => Seq()
      }
    }
    addErrorsToForm(emailForm, validate.flatten)
  }

  private def addErrorsToForm[A](form: Form[A], formErrors: Seq[FormError]): Form[A] = {
    @tailrec
    def y(f: Form[A], fe: Seq[FormError]): Form[A] = {
      if (fe.isEmpty) f
      else y(f.withError(fe.head), fe.tail)
    }
    y(form, formErrors)
  }

}

case class OverseasClientQuestion(isOverseas: Option[Boolean] = None)

object OverseasClientQuestion {
  implicit val formats = Json.format[OverseasClientQuestion]
}

object OverseasClientQuestionForm {
  val overseasClientQuestionForm =
    Form(
      mapping(
        "isOverseas" -> optional(boolean).verifying(Messages("agent.overseas-client-question.error.isOverseas"),x => x.isDefined)
      )(OverseasClientQuestion.apply)(OverseasClientQuestion.unapply)
    )
}
