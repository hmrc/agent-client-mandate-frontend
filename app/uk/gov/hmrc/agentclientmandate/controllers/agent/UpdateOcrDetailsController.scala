/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import play.api.Logger
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.{AgentClientMandateUtils, MandateConstants}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.agentclientmandate.models.{AgentDetails, BusinessRegistrationDisplayDetails, Identification}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{NonUkIdentificationForm, OverseasCompany}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.NonUkIdentificationForm._

import scala.concurrent.Future

trait UpdateOcrDetailsController extends FrontendController with Actions with MandateConstants {

  def agentClientMandateService: AgentClientMandateService

  def dataCacheService: DataCacheService

  def view(service: String) = AuthorisedFor(AgentRegime(), GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          agentDetails <- dataCacheService.fetchAndGetFormData[AgentDetails](agentDetailsFormId)
        } yield {
          agentDetails match {
            case Some(agentDetail) =>
              val nonUkId = OverseasCompany(Some(true),
                idNumber = agentDetail.identification.map(_.idNumber),
                issuingInstitution = agentDetail.identification.map(_.issuingInstitution),
                issuingCountryCode = agentDetail.identification.map(_.issuingCountryCode))
              Ok(views.html.agent.editDetails.update_ocr_details(nonUkIdentificationForm.fill(nonUkId), service, displayDetails(service), getBackLink(service)))
            case None =>
              Logger.warn(s"[UpdateOcrDetailsController][view] - No business details found to edit")
              throw new RuntimeException(Messages("agent.edit-details.error.no-registration-details"))
          }
        }
  }

  def submit(service: String) = AuthorisedFor(AgentRegime(), GGConfidence).async {
    implicit user =>
      implicit request =>
        NonUkIdentificationForm.validateNonUK(nonUkIdentificationForm.bindFromRequest).fold(
          formWithErrors => Future.successful(BadRequest(views.html.agent.editDetails.update_ocr_details(formWithErrors, service, displayDetails(service), getBackLink(service)))),
          updateDetails => {
            for {
              updatedDetails <- agentClientMandateService.updateRegisteredDetails(editNonUKIdDetails = Some(Identification(updateDetails.idNumber.getOrElse(""),
                updateDetails.issuingInstitution.getOrElse(""), updateDetails.issuingCountryCode.getOrElse(""))))
            } yield {
              updatedDetails match {
                case Some(x) => Redirect(routes.AgencyDetailsController.view())
                case None =>
                  val errorMsg = Messages("agent.edit-mandate-detail.save.error")
                  val errorForm = nonUkIdentificationForm.withError(key = "addressType", message = errorMsg).fill(updateDetails)
                  BadRequest(views.html.agent.editDetails.update_ocr_details(errorForm, service, displayDetails(service), getBackLink(service)))
              }
            }
          }
        )
  }

  private def getBackLink(service: String) = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgencyDetailsController.view().url)
  }

  private def displayDetails(service: String) = {
    BusinessRegistrationDisplayDetails("NUK",
      Messages("agent.edit-details.agent.non-uk.header"),
      Messages("agent.edit-details.text.agent", service),
      None,
      AgentClientMandateUtils.getIsoCodeTupleList)
  }

}

object UpdateOcrDetailsController extends UpdateOcrDetailsController {
  // $COVERAGE-OFF$
  val dataCacheService: DataCacheService = DataCacheService
  val authConnector: AuthConnector = FrontendAuthConnector
  val agentClientMandateService: AgentClientMandateService = AgentClientMandateService
  // $COVERAGE-ON$
}
