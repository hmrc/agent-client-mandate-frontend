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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.{ContactDetails, Mandate}
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.EditMandateDetails
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.EditMandateDetailsForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class EditMandateDetailsController @Inject()(
                                              mcc: MessagesControllerComponents,
                                              acmService: AgentClientMandateService,
                                              implicit val ec: ExecutionContext,
                                              implicit val appConfig: AppConfig,
                                              val authConnector: AuthConnector,
                                              templateEditClient: views.html.agent.editClient
                                            ) extends FrontendController(mcc) with AuthorisedWrappers {

  def view(service: String, mandateId: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { authRetrievals =>
        acmService.fetchClientMandate(mandateId, authRetrievals).map {
          case Some(mandate) =>
            val editMandateDetails = EditMandateDetails(displayName = mandate.clientDisplayName,
              email = mandate.agentParty.contactDetails.email)
            Ok(templateEditClient(editMandateDetailsForm.fill(editMandateDetails), service, mandateId,
              mandate.clientDisplayName, mandate.clientParty.map(_.name), getBackLink(service), showRemoveLink(mandate)))
          case _ => throw new RuntimeException(s"No Mandate returned with id $mandateId for service $service")
        }
      }
  }

  def submit(service: String, mandateId: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { authRetrievals =>
        editMandateDetailsForm.bindFromRequest().fold(
          formWithError => {
            acmService.fetchClientMandate(mandateId, authRetrievals) map {
              case Some(mandate) =>
                BadRequest(templateEditClient(formWithError, service, mandateId, mandate.clientDisplayName,
                  mandate.clientParty.map(_.name), getBackLink(service), showRemoveLink(mandate)))
              case _ => throw new RuntimeException(s"No Mandate returned with id $mandateId for service $service")
            }
          },
          editMandate => {
            acmService.fetchClientMandate(mandateId, authRetrievals) flatMap {
              case Some(m) =>
                val agentParty = m.agentParty.copy(contactDetails = ContactDetails(email = editMandate.email))
                acmService.editMandate(
                  m.copy(
                    clientDisplayName = editMandate.displayName,
                    agentParty = agentParty
                  ), authRetrievals
                ) map {
                  case Some(_) =>
                    Redirect(routes.AgentSummaryController.view())
                  case None => Redirect(routes.EditMandateDetailsController.view(mandateId))
                }
              case None => throw new RuntimeException(s"No Mandate Found with id $mandateId for service $service")
            }
          }
        )
      }
  }

  private def showRemoveLink(mandate: Mandate): Boolean = {
    mandate.currentStatus.status != uk.gov.hmrc.agentclientmandate.models.Status.PendingActivation &&
    mandate.currentStatus.status != uk.gov.hmrc.agentclientmandate.models.Status.PendingCancellation
  }

  private def getBackLink(service: String): Some[String] = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view().url)
  }
}
