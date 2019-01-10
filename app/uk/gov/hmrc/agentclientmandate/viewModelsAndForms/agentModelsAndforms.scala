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

import forms.mappings.{Constraints, Mappings}
import play.api.data.Forms._
import play.api.data.{Form, FormError, Mapping}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.models.{Identification, RegisteredAddressDetails}
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

case class FilterClients(displayName: Option[String], showAllClients: String)

object FilterClients {
  implicit val formats = Json.format[FilterClients]
}

object FilterClientsForm {
 val filterClientsForm = Form(
    mapping(
       "displayName" -> optional(text),
       "showAllClients" -> text
  )(FilterClients.apply)(FilterClients.unapply)
  )
}

case class AgentEmail(email: String)

object AgentEmail {
  implicit val formats = Json.format[AgentEmail]
}

object AgentEmailForm extends Constraints {
  val minimumLength = 1
  val maximumLength = 241

  val emailRegex =
    """^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$"""


  val agentEmailForm =
    Form(
      mapping(
        "email" -> text
          .verifying(regexp(emailRegex, "client.email.error.email.invalid"))
          .verifying(maxLength(maximumLength, "client.email.error.email.invalid"))
          .verifying(minLength(minimumLength, "client.email.error.email.invalid"))
      )(AgentEmail.apply)(AgentEmail.unapply)
    )
}

case class AgentMissingEmail(useEmailAddress: Option[Boolean] = None, email: Option[String] = None)

object AgentMissingEmail {
  implicit val formats = Json.format[AgentMissingEmail]
}

object AgentMissingEmailForm {
  val maxlength = 241
  val lengthZero = 0
  val emailRegex =
    """^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$""".r


  def validateAgentMissingEmail(f: Form[AgentMissingEmail]): Form[AgentMissingEmail] = {
    if (!f.hasErrors) {
      val emailConsent = f.data.get("useEmailAddress")
      val formErrors = emailConsent match {
        case Some("true") => {
          val email = f.data.get("email").getOrElse("")
          if (email.trim.length == lengthZero) {
            Seq(FormError("email", Messages("agent.enter-email.error.email")))
          }
          else if (email.length > lengthZero && email.length > maxlength) {
            Seq(FormError("email", Messages("agent.enter-email.error.email.max.length")))
          } else {
            val x = emailRegex.findFirstMatchIn(email).exists(_ => true)
            val y = email.length == lengthZero
            val z = email.length > maxlength
            if (x || y || z) {
              Nil
            } else {
              Seq(FormError("email", Messages("agent.enter-email.error.general.agent-enter-email-form")))
            }
          }
        }
        case _ => Nil
      }
      addErrorsToForm(f, formErrors)
    } else f
  }


  val agentMissingEmailForm =
    Form(
      mapping(
        "useEmailAddress" -> optional(boolean).verifying(Messages("agent.missing-email.must_answer"), x => x.isDefined),
        "email" -> optional(text)
      )(AgentMissingEmail.apply)(AgentMissingEmail.unapply)
    )

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

case class EditMandateDetails(displayName: String, email: String)

object EditMandateDetailsForm {

  val length40 = 40
  val length0 = 0
  val length105 = 105
  val length99 = 99
  val emailLength = 241
  val emailRegex =
    """^(?!\.)("([^"\r\\]|\\["\r\\])*"|([-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]|(?<!\.)\.)*)(?<!\.)@[a-zA-Z0-9][\w\.-]*[a-zA-Z0-9]\.[a-zA-Z][a-zA-Z\.]*[a-zA-Z]$""".r

  def validateEditEmail(f: Form[EditMandateDetails]): Form[EditMandateDetails] = {
    def validateEmailRegex(email: String) = {
      val x = emailRegex.findFirstMatchIn(email).exists(_ => true)
      val y = email.length == length0
      val z = email.length > emailLength
      if (x || y || z) {
        f
      } else {
        f.withError((FormError("email", Messages("agent.enter-email.error.general.agent-enter-email-form"))))
      }
    }

    if (!f.hasErrors) {
      val email = f.data.get("email").getOrElse("")
      if (email.trim.length == length0) {
        f.withError(FormError("email", Messages("agent.enter-email.error.email")))
      }
      else if (email.length > length0 && email.length > emailLength) {
        f.withError((FormError("email", Messages("agent.enter-email.error.email.max.length"))))
      } else {
        validateEmailRegex(email)
      }
    } else f
  }


  val editMandateDetailsForm = Form(mapping(
    "displayName" -> text
      .verifying(Messages("agent.edit-client.error.dispName"), x => x.length > length0)
      .verifying(Messages("agent.edit-client.error.dispName.length"), x => x.isEmpty || (x.nonEmpty && x.length <= length99)),

    "email" -> text.verifying(Messages("agent.edit-client.error.email"), email => email.nonEmpty)
  )(EditMandateDetails.apply)(EditMandateDetails.unapply))
}

case class NRLQuestion(nrl: Option[Boolean] = None)

object NRLQuestionForm {
  implicit val formats = Json.format[NRLQuestion]

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
  implicit val formats = Json.format[ClientPermission]

  val clientPermissionForm = Form(
    mapping(
      "hasPermission" -> optional(boolean).verifying(Messages("agent.client-permission.hasPermission.not-selected.error"), a => a.isDefined)
    )(ClientPermission.apply)(ClientPermission.unapply)
  )

}

case class PrevRegistered(prevRegistered: Option[Boolean] = None)

object PrevRegisteredForm {
  implicit val formats = Json.format[PrevRegistered]

  val prevRegisteredForm = Form(
    mapping(
      "prevRegistered" -> optional(boolean).verifying(Messages("agent.client-prev-registered.not-selected.field-error"), a => a.isDefined)
    )(PrevRegistered.apply)(PrevRegistered.unapply)
  )

}


case class ClientDisplayName(name: String)

object ClientDisplayName {
  implicit val formats = Json.format[ClientDisplayName]
}


case class ClientMandateDisplayDetails(name: String, mandateId: String, agentLastUsedEmail: String)

object ClientMandateDisplayDetails {
  implicit val formats = Json.format[ClientMandateDisplayDetails]
}

object ClientDisplayNameForm {

  val lengthZero = 0
  val clientDisplayNameForm = Form(
    mapping(
      "clientDisplayName" -> text
        .verifying(Messages("agent.client-display-name.error.not-selected"), x => x.trim.length > lengthZero)
        .verifying(Messages("agent.client-display-name.error.length"), x => x.isEmpty || (x.nonEmpty && x.length <= 99))
    )(ClientDisplayName.apply)(ClientDisplayName.unapply)
  )

}

case class EditAgentAddressDetails(agentName: String, address: RegisteredAddressDetails)

object EditAgentAddressDetails {
  implicit val formats = Json.format[EditAgentAddressDetails]
}

object EditAgentAddressDetailsForm {

  val postcodeLength = 10
  val length40 = 40
  val length35 = 35
  val length0 = 0
  val length2 = 2
  val length60 = 60
  val length105 = 105

  val countryUK = "GB"

  val editAgentAddressDetailsForm = Form(
    mapping(
      "agentName" -> text.
        verifying(Messages("agent.edit-details-error.businessName"), x => x.trim.length > length0)
        .verifying(Messages("agent.edit-details-error.businessName.length", length105), x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
      "address" -> mapping(
        "addressLine1" -> text.
          verifying(Messages("agent.edit-details-error.line_1"), x => x.trim.length > length0)
          .verifying(Messages("agent.edit-details-error.line_1.length", length35), x => x.isEmpty || (x.nonEmpty && x.length <= length35)),
        "addressLine2" -> text.
          verifying(Messages("agent.edit-details-error.line_2"), x => x.trim.length > length0)
          .verifying(Messages("agent.edit-details-error.line_2.length", length35), x => x.isEmpty || (x.nonEmpty && x.length <= length35)),
        "addressLine3" -> optional(text)
          .verifying(Messages("agent.edit-details-error.line_3.length", length35), x => x.isEmpty || (x.nonEmpty && x.get.length <= length35)),
        "addressLine4" -> optional(text)
          .verifying(Messages("agent.edit-details-error.line_4.length", length35), x => x.isEmpty || (x.nonEmpty && x.get.length <= length35)),
        "postalCode" -> optional(text)
          .verifying(Messages("agent.edit-details-error.postcode.length", postcodeLength),
            x => x.isEmpty || (x.nonEmpty && x.get.length <= postcodeLength)),
        "countryCode" -> text.
          verifying(Messages("agent.edit-details-error.country"), x => x.length > length0)
      )(RegisteredAddressDetails.apply)(RegisteredAddressDetails.unapply)
    )(EditAgentAddressDetails.apply)(EditAgentAddressDetails.unapply)
  )

  def validateCountryNonUKAndPostcode(agentData: Form[EditAgentAddressDetails]) = {
    val country = agentData.data.get("businessAddress.country") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    val countryForm = {
      if (country.fold("")(x => x).matches(countryUK)) {
        agentData.withError(key = "businessAddress.country", message = Messages("agent.edit-details-error.non-uk"))
      } else {
        agentData
      }
    }
  }
}

case class OverseasCompany(hasBusinessUniqueId: Option[Boolean] = Some(false),
                           idNumber: Option[String] = None,
                           issuingInstitution: Option[String] = None,
                           issuingCountryCode: Option[String] = None)

object OverseasCompany {
  implicit val formats = Json.format[OverseasCompany]
}

object NonUkIdentificationForm {
  val length40 = 40
  val length60 = 60
  val length0 = 0

  val countryUK = "GB"

  val nonUkIdentificationForm = Form(
    mapping(
      "hasBusinessUniqueId" -> optional(boolean).verifying(Messages("agent.edit-details-error.hasBusinessUniqueId.not-selected"), x => x.isDefined),
      "idNumber" -> optional(text)
        .verifying(Messages("agent.edit-details-error.businessUniqueId.length", length60), x => x.isEmpty || (x.nonEmpty && x.get.length <= length60)),
      "issuingInstitution" -> optional(text)
        .verifying(Messages("agent.edit-details-error.issuingInstitution.length", length40), x => x.isEmpty || (x.nonEmpty && x.get.length <= length40)),
      "issuingCountryCode" -> optional(text)
    )(OverseasCompany.apply)(OverseasCompany.unapply)
  )

  def validateNonUK(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
    validateNonUkIdentifiers(registrationData)
  }

  def validateNonUkIdentifiers(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
    validateNonUkIdentifiersInstitution(validateNonUkIdentifiersCountry(validateNonUkIdentifiersId(registrationData)))
  }

  def validateNonUkIdentifiersInstitution(registrationData: Form[OverseasCompany]) = {
    val hasBusinessUniqueId = registrationData.data.get("hasBusinessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    } map {
      _.toBoolean
    }
    val issuingInstitution = registrationData.data.get("issuingInstitution") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    hasBusinessUniqueId match {
      case Some(true) if issuingInstitution.isEmpty =>
        registrationData.withError(key = "issuingInstitution", message = Messages("agent.edit-mandate-details-error.issuingInstitution.select"))
      case _ => registrationData
    }
  }

  def validateNonUkIdentifiersCountry(registrationData: Form[OverseasCompany]) = {
    val hasBusinessUniqueId = registrationData.data.get("hasBusinessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    } map {
      _.toBoolean
    }
    val issuingCountry = registrationData.data.get("issuingCountryCode") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    hasBusinessUniqueId match {
      case Some(true) if issuingCountry.isEmpty =>
        registrationData.withError(key = "issuingCountryCode", message = Messages("agent.edit-mandate-details-error.issuingCountry.select"))
      case Some(true) if issuingCountry.isDefined && issuingCountry.fold("")(x => x).matches(countryUK) =>
        registrationData.withError(key = "issuingCountryCode", message = Messages("agent.edit-mandate-details-error.non-uk"))
      case _ => registrationData
    }
  }

  def validateNonUkIdentifiersId(registrationData: Form[OverseasCompany]) = {
    val hasBusinessUniqueId = registrationData.data.get("hasBusinessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    } map {
      _.toBoolean
    }
    val businessUniqueId = registrationData.data.get("idNumber") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    hasBusinessUniqueId match {
      case Some(true) if businessUniqueId.isEmpty =>
        registrationData.withError(key = "idNumber", message = Messages("agent.edit-mandate-details-error.businessUniqueId.select"))
      case _ => registrationData
    }
  }

}
