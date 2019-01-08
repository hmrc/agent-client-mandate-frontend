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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.models.{ContactDetails, Mandate}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, EmailService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.EditMandateDetailsForm._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{EditMandateDetails, EditMandateDetailsForm}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

trait EditMandateDetailsController extends FrontendController with Actions {

  def acmService: AgentClientMandateService

  def emailService: EmailService

  def view(service: String, mandateId: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      acmService.fetchClientMandate(mandateId).map {
        case Some(mandate) =>
          val editMandateDetails = EditMandateDetails(displayName = mandate.clientDisplayName,
            email = mandate.agentParty.contactDetails.email)
          Ok(views.html.agent.editClient(editMandateDetailsForm.fill(editMandateDetails), service, mandateId, mandate.clientDisplayName, mandate.clientParty.map(_.name), getBackLink(service), showRemoveLink(mandate)))
        case _ => throw new RuntimeException(s"No Mandate returned with id $mandateId for service $service")
      }
  }

  def submit(service: String, mandateId: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request => EditMandateDetailsForm.validateEditEmail(editMandateDetailsForm.bindFromRequest).fold(
      formWithError => {
        acmService.fetchClientMandate(mandateId) map {
          case Some(mandate) =>
            BadRequest(views.html.agent.editClient(formWithError, service, mandateId, mandate.clientDisplayName,mandate.clientParty.map(_.name), getBackLink(service), showRemoveLink(mandate)))
          case _ => throw new RuntimeException(s"No Mandate returned with id $mandateId for service $service")
        }
      },
      editMandate => {
        emailService.validate(editMandate.email) flatMap { isValidEmail =>
          if (isValidEmail) {
            acmService.fetchClientMandate(mandateId) flatMap {
              case Some(m) =>
                val agentParty = m.agentParty.copy(contactDetails = ContactDetails(email = editMandate.email))
                acmService.editMandate(m.copy(clientDisplayName = editMandate.displayName,
                  agentParty = agentParty)) map {
                  case Some(updatedMandate) =>
                    Redirect(routes.AgentSummaryController.view())
                  case None => Redirect(routes.EditMandateDetailsController.view( mandateId))
                }
              case None => throw new RuntimeException(s"No Mandate Found with id $mandateId for service $service")
            }
          } else {
            val errorMsg = Messages("agent.enter-email.error.email.invalid-by-email-service")
            val errorForm = editMandateDetailsForm.withError(key = "agent-enter-email-form", message = errorMsg).fill(editMandate)
            acmService.fetchClientMandate(mandateId) map {
              case Some(mandate) =>
                BadRequest(views.html.agent.editClient(errorForm, service, mandateId, mandate.clientDisplayName, mandate.clientParty.map(_.name),getBackLink(service), showRemoveLink(mandate)))
              case _ => throw new RuntimeException(s"No Mandate returned with id $mandateId for service $service")
            }

          }
        }
      }
    )
  }

  private def showRemoveLink(mandate: Mandate) = {
    mandate.currentStatus.status != uk.gov.hmrc.agentclientmandate.models.Status.PendingActivation &&
    mandate.currentStatus.status != uk.gov.hmrc.agentclientmandate.models.Status.PendingCancellation
  }

  private def getBackLink(service: String) = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view().url)
  }
}

object EditMandateDetailsController extends EditMandateDetailsController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val acmService = AgentClientMandateService
  val emailService = EmailService
  // $COVERAGE-ON$
}
