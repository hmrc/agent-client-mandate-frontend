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

package uk.gov.hmrc.agentclientmandate.models


import java.time.LocalDate
import play.api.libs.json.{Json, OFormat}

case class EtmpContactDetails(phoneNumber: Option[String] = None,
                              mobileNumber: Option[String] = None,
                              faxNumber: Option[String] = None,
                              emailAddress: Option[String] = None)

object EtmpContactDetails {
  implicit val formats: OFormat[EtmpContactDetails] = Json.format[EtmpContactDetails]
}

case class Individual(firstName: String,
                      middleName: Option[String] = None,
                      lastName: String,
                      dateOfBirth: LocalDate)

object Individual {
  implicit val formats: OFormat[Individual] = Json.format[Individual]
}

case class Organisation(organisationName: String,
                        isAGroup: Option[Boolean] = None)

object Organisation {
  implicit val formats: OFormat[Organisation] = Json.format[Organisation]
}


case class BusinessRegistrationDisplayDetails(businessType: String,
                                              businessRegHeader: String,
                                              businessRegSubHeader: String,
                                              businessRegLede: Option[String],
                                              listOfIsoCode: List[(String, String)])

case class Identification(idNumber: String, issuingInstitution: String, issuingCountryCode: String)

object Identification {
  implicit val formats: OFormat[Identification] = Json.format[Identification]
}


case class AgentDetails(safeId: String,
                        isAnIndividual: Boolean,
                        individual: Option[Individual],
                        organisation: Option[Organisation],
                        addressDetails: RegisteredAddressDetails,
                        contactDetails: EtmpContactDetails,
                        identification: Option[Identification]) {

  def agentName: String = {
    if (isAnIndividual) s"${individual.map(_.firstName).getOrElse("")} ${individual.map(_.lastName).getOrElse("")}"
    else organisation.map(_.organisationName).getOrElse("")

  }
}

object AgentDetails {
  implicit val formats: OFormat[AgentDetails] = Json.format[AgentDetails]
}

case class UpdateRegistrationDetailsRequest(acknowledgementReference: String,
                                            isAnIndividual: Boolean,
                                            individual: Option[Individual],
                                            organisation: Option[Organisation],
                                            address: RegisteredAddressDetails,
                                            contactDetails: EtmpContactDetails,
                                            isAnAgent: Boolean,
                                            isAGroup: Boolean,
                                            identification: Option[Identification] = None)

object UpdateRegistrationDetailsRequest {
  implicit val formats: OFormat[UpdateRegistrationDetailsRequest] = Json.format[UpdateRegistrationDetailsRequest]
}
