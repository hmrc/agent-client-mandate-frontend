/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.{AgentDetails, BusinessRegistrationDisplayDetails, Identification}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.NonUkIdentificationForm._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{NonUkIdentificationForm, OverseasCompany}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateOcrDetailsController @Inject()(
                                            agentClientMandateService: AgentClientMandateService,
                                            dataCacheService: DataCacheService,
                                            mcc: MessagesControllerComponents,
                                            val authConnector: AuthConnector,
                                            implicit val ec: ExecutionContext,
                                            implicit val appConfig: AppConfig
                                          ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants with I18nSupport {

  def view(service: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
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
            throw new RuntimeException("No Registration Details found")
        }
      }
    }
  }

  def submit(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { agentAuthRetrievals =>
        NonUkIdentificationForm.validateNonUK(nonUkIdentificationForm.bindFromRequest).fold(
          formWithErrors => Future.successful(
            BadRequest(views.html.agent.editDetails.update_ocr_details(
              formWithErrors, service, displayDetails(service), getBackLink(service)
            ))
          ),
          updateDetails => {
            for {
              updatedDetails <- agentClientMandateService.updateRegisteredDetails(
                editNonUKIdDetails = Some(Identification(
                  updateDetails.idNumber.getOrElse(""),
                  updateDetails.issuingInstitution.getOrElse(""),
                  updateDetails.issuingCountryCode.getOrElse("")
                )),
                agentAuthRetrievals = agentAuthRetrievals
              )
            } yield {
              updatedDetails match {
                case Some(_) => Redirect(routes.AgencyDetailsController.view())
                case None =>
                  val errorMsg = "agent.edit-mandate-detail.save.error"
                  val errorForm = nonUkIdentificationForm.withError(key = "addressType", message = errorMsg).fill(updateDetails)
                  BadRequest(views.html.agent.editDetails.update_ocr_details(errorForm, service, displayDetails(service), getBackLink(service)))
              }
            }
          }
        )
      }
  }

  private def getBackLink(service: String): Some[String] = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgencyDetailsController.view().url)
  }

  private def displayDetails(service: String): BusinessRegistrationDisplayDetails = {
    BusinessRegistrationDisplayDetails("NUK",
      "agent.edit-details.agent.non-uk.header",
      "agent.edit-details.text.agent",
      None,
      appConfig.getIsoCodeTupleList)
  }

}
