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

import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.utils.AgentClientMandateUtils._

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
        "isOverseas" -> optional(boolean).verifying(Messages("agent.overseas-client-question.error.isOverseas"), x => x.isDefined)
      )(OverseasClientQuestion.apply)(OverseasClientQuestion.unapply)
    )
}

case class CollectClientBusinessDetails(businessName: String, utr: String)

object CollectClientBusinessDetails {
  implicit val formats = Json.format[CollectClientBusinessDetails]
}

object CollectClientBusinessDetailsForm {

  val length40 = 40
  val length0 = 0
  val length105 = 105

  val collectClientBusinessDetails = Form(mapping(
    "businessName" -> text
      .verifying(Messages("agent.enter-business-details-error.businessName"), x => x.length > length0)
      .verifying(Messages("agent.enter-business-details-error.businessName.length"), x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "utr" -> text
      .verifying(Messages("agent.enter-business-details-error.utr"), x => x.length > length0)
      .verifying(Messages("agent.enter-business-details-error.utr.length"), x => x.isEmpty || (x.nonEmpty && x.matches("""^[0-9]{10}$""")))
      .verifying(Messages("agent.enter-business-details-error.invalidUTR"), x => x.isEmpty || (validateUTR(Option(x)) || !x.matches("""^[0-9]{10}$""")))

  )(CollectClientBusinessDetails.apply)(CollectClientBusinessDetails.unapply))
}

case class EditMandateDetails( displayName: String, email: String)

object EditMandateDetailsForm {

  val length40 = 40
  val length0 = 0
  val length105 = 105

  val editMandateDetailsForm = Form(mapping(
  "displayName" -> text
    .verifying(Messages("agent.edit-client.error.dispName"), x => x.length > length0)
    .verifying(Messages("agent.edit-client.error.dispName.length"), x => x.isEmpty || (x.nonEmpty && x.length <= length105)),

   "email" -> text.verifying(Messages("agent.edit-client.error.email"), email => email.nonEmpty)
  )(EditMandateDetails.apply)(EditMandateDetails.unapply))
}

case class NRLQuestion(nrl: Option[Boolean] = None)

object NRLQuestionForm {

  val nrlQuestionForm = Form(
    mapping(
      "nrl" -> optional(boolean).verifying(Messages("agent.nrl-question.nrl.not-selected.error"), a => a.isDefined)
    )(NRLQuestion.apply)(NRLQuestion.unapply)
  )

}

case class PaySAQuestion(paySA: Option[Boolean] = None)

object PaySAQuestion {

  val paySAQuestionForm = Form(
    mapping(
      "paySA" -> optional(boolean).verifying(Messages("agent.paySA-question.paySA.not-selected.error"), a => a.isDefined)
    )(PaySAQuestion.apply)(PaySAQuestion.unapply)
  )

}

case class ClientPermission(hasPermission: Option[Boolean] = None)

object ClientPermissionForm {

  val clientPermissionForm = Form(
    mapping(
      "hasPermission" -> optional(boolean).verifying(Messages("agent.client-permission.hasPermission.not-selected.error"), a => a.isDefined)
    )(ClientPermission.apply)(ClientPermission.unapply)
  )

}

case class ClientDisplayName(name: String)

object ClientDisplayName {
  implicit val formats = Json.format[ClientDisplayName]
}


case class ClientDisplayDetails(name: String, mandateId: String)

object ClientDisplayDetails {
  implicit val formats = Json.format[ClientDisplayDetails]
}

object ClientDisplayNameForm {

  val clientDisplayNameForm = Form(
    mapping(
      "clientDisplayName" -> text
        .verifying(Messages("agent.client-display-name.error.not-selected"), x => x.length > 0)
        .verifying(Messages("agent.client-display-name.error.length"), x => x.isEmpty || (x.nonEmpty && x.length <= 99))
    )(ClientDisplayName.apply)(ClientDisplayName.unapply)
  )

}
