/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.agentclientmandate.models.RegisteredAddressDetails
import uk.gov.hmrc.agentclientmandate.utils.AgentClientMandateUtils.{emailRegex, _}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.mappings.Constraints
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

case class AgentSelectService(service: Option[String] = None)

object AgentSelectServiceForm {
  def selectServiceForm: Form[AgentSelectService] =
    Form(
      mapping(
        "service" -> optional(text).verifying("agent.select-service.error.service", serviceOpt => serviceOpt.isDefined)
      )(AgentSelectService.apply)(AgentSelectService.unapply)
    )
}

case class FilterClients(displayName: Option[String], showAllClients: String)

object FilterClients {
  implicit val formats: OFormat[FilterClients] = Json.format[FilterClients]
}

object FilterClientsForm {
 val filterClientsForm: Form[FilterClients] = Form(
    mapping(
       "displayName" -> optional(text),
       "showAllClients" -> text
  )(FilterClients.apply)(FilterClients.unapply)
  )
}

case class AgentEmail(email: String)

object AgentEmail {
  implicit val formats: OFormat[AgentEmail] = Json.format[AgentEmail]
}

object AgentEmailForm extends Constraints {
  def agentEmailForm: Form[AgentEmail] =
    Form(
      mapping(
        "email" -> text
          .verifying(regexp(emailRegex, "client.email.error.email.invalid"))
          .verifying(minLength(minimumEmailLength, "agent.edit-client.error.email"))
          .verifying(maxLength(maximumEmailLength, "client.email.error.email.too.long"))
      )(AgentEmail.apply)(AgentEmail.unapply)
    )
}

case class AgentMissingEmail(useEmailAddress: Option[Boolean] = None, email: Option[String] = None)

object AgentMissingEmail {
  implicit val formats: OFormat[AgentMissingEmail] = Json.format[AgentMissingEmail]
}

object AgentMissingEmailForm extends Constraints  {

  def agentMissingEmailForm: Form[AgentMissingEmail] =
    Form(
        mapping(

          "useEmailAddress" -> optional(boolean).verifying("agent.missing-email.must_answer", x => x.isDefined),
          "email" -> mandatoryIfTrue("useEmailAddress", text
            .verifying(regexp(emailRegex, "client.email.error.email.invalid"))
            .verifying(minLength(minimumEmailLength, "client.email.error.email.empty"))
            .verifying(maxLength(maximumEmailLength, "client.email.error.email.too.long")))

        )(AgentMissingEmail.apply)(AgentMissingEmail.unapply)
    )
}

case class OverseasClientQuestion(isOverseas: Option[Boolean] = None)

object OverseasClientQuestion {
  implicit val formats: OFormat[OverseasClientQuestion] = Json.format[OverseasClientQuestion]
}

object OverseasClientQuestionForm {
  def overseasClientQuestionForm: Form[OverseasClientQuestion] =
    Form(
      mapping(
        "isOverseas" -> optional(boolean).verifying("agent.overseas-client-question.error.isOverseas", x => x.isDefined)
      )(OverseasClientQuestion.apply)(OverseasClientQuestion.unapply)
    )
}

case class CollectClientBusinessDetails(businessName: String, utr: String)

object CollectClientBusinessDetails {
  implicit val formats: OFormat[CollectClientBusinessDetails] = Json.format[CollectClientBusinessDetails]
}

object CollectClientBusinessDetailsForm {

  val length40 = 40
  val length0 = 0
  val length105 = 105

  def collectClientBusinessDetails: Form[CollectClientBusinessDetails] = Form(mapping(
    "businessName" -> text
      .verifying("agent.enter-business-details-error.businessName", x => x.length > length0)
      .verifying("agent.enter-business-details-error.businessName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "utr" -> text
      .verifying("agent.enter-business-details-error.utr", x => x.length > length0)
      .verifying("agent.enter-business-details-error.utr.length", x => x.isEmpty || (x.nonEmpty && x.matches("""^[0-9]{10}$""")))
      .verifying("agent.enter-business-details-error.invalidUTR", x => x.isEmpty || (validateUTR(Option(x)) || !x.matches("""^[0-9]{10}$""")))

  )(CollectClientBusinessDetails.apply)(CollectClientBusinessDetails.unapply))
}

case class EditMandateDetails(displayName: String, email: String)

object EditMandateDetailsForm extends Constraints {

  val length0 = 0
  val length99 = 99

  def editMandateDetailsForm: Form[EditMandateDetails] =
    Form(
      mapping(
    "displayName" -> text
      .verifying("agent.edit-client.error.dispName", x => x.length > length0)
      .verifying("agent.edit-client.error.dispName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length99)),

    "email" -> text
      .verifying(regexp(emailRegex, "client.email.error.email.invalid"))
      .verifying(minLength(minimumEmailLength, "agent.edit-client.error.email"))
      .verifying(maxLength(maximumEmailLength, "client.email.error.email.too.long"))
  )(EditMandateDetails.apply)(EditMandateDetails.unapply))

}

case class NRLQuestion(nrl: Option[Boolean] = None)

object NRLQuestionForm {
  implicit val formats: OFormat[NRLQuestion] = Json.format[NRLQuestion]

  def nrlQuestionForm: Form[NRLQuestion] = Form(
    mapping(
      "nrl" -> optional(boolean).verifying("agent.nrl-question.nrl.not-selected.error", a => a.isDefined)
    )(NRLQuestion.apply)(NRLQuestion.unapply)
  )

}

case class PaySAQuestion(paySA: Option[Boolean] = None)

object PaySAQuestion {
  implicit val formats: OFormat[PaySAQuestion] = Json.format[PaySAQuestion]

  def paySAQuestionForm: Form[PaySAQuestion] = Form(
    mapping(
      "paySA" -> optional(boolean).verifying("agent.paySA-question.paySA.not-selected.error", a => a.isDefined)
    )(PaySAQuestion.apply)(PaySAQuestion.unapply)
  )

}

case class ClientPermission(hasPermission: Option[Boolean] = None)

object ClientPermissionForm {
  implicit val formats: OFormat[ClientPermission] = Json.format[ClientPermission]

  def clientPermissionForm: Form[ClientPermission] = Form(
    mapping(
      "hasPermission" -> optional(boolean).verifying("agent.client-permission.hasPermission.not-selected.error", a => a.isDefined)
    )(ClientPermission.apply)(ClientPermission.unapply)
  )

}

case class PrevUniqueAuthNum(authNum: Option[Boolean] = None)

object PrevUniqueAuthNumForm{
  implicit val formats: OFormat[PrevUniqueAuthNum] = Json.format[PrevUniqueAuthNum]

  def prevUniqueAuthNumForm: Form[PrevUniqueAuthNum] = Form(
    mapping(
      "authNum" -> optional(boolean).verifying("agent.prev-auth-num.not-selected.field-error", a => a.isDefined)
    )(PrevUniqueAuthNum.apply)(PrevUniqueAuthNum.unapply)
  )

}

case class PrevRegistered(prevRegistered: Option[Boolean] = None)

object PrevRegisteredForm {
  implicit val formats: OFormat[PrevRegistered] = Json.format[PrevRegistered]

  def prevRegisteredForm: Form[PrevRegistered] = Form(
    mapping(
      "prevRegistered" -> optional(boolean).verifying("agent.client-prev-registered.not-selected.field-error", a => a.isDefined)
    )(PrevRegistered.apply)(PrevRegistered.unapply)
  )

}


case class ClientDisplayName(name: String)

object ClientDisplayName {
  implicit val formats: OFormat[ClientDisplayName] = Json.format[ClientDisplayName]
}


case class ClientMandateDisplayDetails(name: String, mandateId: String, agentLastUsedEmail: String)

object ClientMandateDisplayDetails {
  implicit val formats: OFormat[ClientMandateDisplayDetails] = Json.format[ClientMandateDisplayDetails]
}

object ClientDisplayNameForm {

  val lengthZero = 0
  def clientDisplayNameForm: Form[ClientDisplayName] = Form(
    mapping(
      "clientDisplayName" -> text
        .verifying("agent.client-display-name.error.not-selected", x => x.trim.length > lengthZero)
        .verifying("agent.client-display-name.error.length", x => x.isEmpty || (x.nonEmpty && x.length <= 99))
    )(ClientDisplayName.apply)(ClientDisplayName.unapply)
  )

}

case class EditAgentAddressDetails(agentName: String, address: RegisteredAddressDetails)

object EditAgentAddressDetails {
  implicit val formats: OFormat[EditAgentAddressDetails] = Json.format[EditAgentAddressDetails]
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

  def editAgentAddressDetailsForm: Form[EditAgentAddressDetails] = Form(
    mapping(
      "agentName" -> text.
        verifying("agent.edit-details-error.businessName", x => x.trim.length > length0)
        .verifying("agent.edit-details-error.businessName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
      "address" -> mapping(
        "addressLine1" -> text.
          verifying("agent.edit-details-error.line_1", x => x.trim.length > length0)
          .verifying("agent.edit-details-error.line_1.length", x => x.isEmpty || (x.nonEmpty && x.length <= length35)),
        "addressLine2" -> text.
          verifying("agent.edit-details-error.line_2", x => x.trim.length > length0)
          .verifying("agent.edit-details-error.line_2.length", x => x.isEmpty || (x.nonEmpty && x.length <= length35)),
        "addressLine3" -> optional(text)
          .verifying("agent.edit-details-error.line_3.length", x => x.isEmpty || (x.nonEmpty && x.get.length <= length35)),
        "addressLine4" -> optional(text)
          .verifying("agent.edit-details-error.line_4.length", x => x.isEmpty || (x.nonEmpty && x.get.length <= length35)),
        "postalCode" -> optional(text)
          .verifying("agent.edit-details-error.postcode.length",
            x => x.isEmpty || (x.nonEmpty && x.get.length <= postcodeLength)),
        "countryCode" -> text.
          verifying("agent.edit-details-error.country", x => x.length > length0)
      )(RegisteredAddressDetails.apply)(RegisteredAddressDetails.unapply)
    )(EditAgentAddressDetails.apply)(EditAgentAddressDetails.unapply)
  )
}

case class OverseasCompany(hasBusinessUniqueId: Option[Boolean] = Some(false),
                           idNumber: Option[String] = None,
                           issuingInstitution: Option[String] = None,
                           issuingCountryCode: Option[String] = None)

object OverseasCompany {
  implicit val formats: OFormat[OverseasCompany] = Json.format[OverseasCompany]
}

object NonUkIdentificationForm {
  val length40 = 40
  val length60 = 60
  val length0 = 0

  val countryUK = "GB"

  def nonUkIdentificationForm: Form[OverseasCompany] = Form(
    mapping(
      "hasBusinessUniqueId" -> optional(boolean).verifying("agent.edit-details-error.hasBusinessUniqueId.not-selected", x => x.isDefined),
      "idNumber" -> optional(text)
        .verifying("agent.edit-details-error.businessUniqueId.length", x => x.isEmpty || (x.nonEmpty && x.get.length <= length60)),
      "issuingInstitution" -> optional(text)
        .verifying("agent.edit-details-error.issuingInstitution.length", x => x.isEmpty || (x.nonEmpty && x.get.length <= length40)),
      "issuingCountryCode" -> optional(text)
    )(OverseasCompany.apply)(OverseasCompany.unapply)
  )

  def validateNonUK(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
    validateNonUkIdentifiers(registrationData)
  }

  def validateNonUkIdentifiers(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
    validateNonUkIdentifiersInstitution(validateNonUkIdentifiersCountry(validateNonUkIdentifiersId(registrationData)))
  }

  def validateNonUkIdentifiersInstitution(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
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
        registrationData.withError(key = "issuingInstitution", message = "agent.edit-mandate-details-error.issuingInstitution.select")
      case _ => registrationData
    }
  }

  def validateNonUkIdentifiersCountry(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
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
        registrationData.withError(key = "issuingCountryCode", message = "agent.edit-mandate-details-error.issuingCountry.select")
      case Some(true) if issuingCountry.isDefined && issuingCountry.fold("")(x => x).matches(countryUK) =>
        registrationData.withError(key = "issuingCountryCode", message = "agent.edit-mandate-details-error.non-uk")
      case _ => registrationData
    }
  }

  def validateNonUkIdentifiersId(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
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
        registrationData.withError(key = "idNumber", message = "agent.edit-mandate-details-error.businessUniqueId.select")
      case _ => registrationData
    }
  }

}
