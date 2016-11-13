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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import play.api.i18n.Messages
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.models.ContactDetails
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, EmailService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.EditMandateDetails
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.EditMandateDetailsForm._

import scala.concurrent.Future
import uk.gov.hmrc.play.frontend.auth.{Actions, Delegator}

trait EditMandateDetailsController extends FrontendController with Actions {

  def acmService: AgentClientMandateService

  def emailService: EmailService

  def view(service: String, mandateId: String) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      acmService.fetchClientMandate(mandateId).map {
        case Some(mandate) =>
          val editMandateDetails = EditMandateDetails(displayName = mandate.clientDisplayName,
            email = mandate.clientParty.fold(throw new RuntimeException("No client found!"))(_.contactDetails.email))
          Ok(views.html.agent.editClient(editMandateDetailsForm.fill(editMandateDetails), service, mandateId))
        case _ => throw new RuntimeException("No Mandate returned")
      }
  }

  def submit(service: String, mandateId: String) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit authContext => implicit request => editMandateDetailsForm.bindFromRequest.fold(
      formWithError => Future.successful(BadRequest(views.html.agent.editClient(formWithError, service, mandateId))),
      editMandate => {
        emailService.validate(editMandate.email) flatMap { isValidEmail =>
          if (isValidEmail) {
            acmService.fetchClientMandate(mandateId) flatMap {
              case Some(m) =>
                val clientParty = m.clientParty.fold(throw new RuntimeException("No client party found!"))(_.copy(contactDetails = ContactDetails(email = editMandate.email)))
                acmService.editMandate(m.copy(clientDisplayName = editMandate.displayName,
                  clientParty = Some(clientParty))) map {
                  case Some(updatedMandate) =>
                    Redirect(routes.AgentSummaryController.view(service))
                  case None => Redirect(routes.EditMandateDetailsController.view(service, mandateId))
                }
              case None => throw new RuntimeException("No Mandate Found!")
            }
          } else {
            val errorMsg = Messages("agent.enter-email.error.email.invalid-by-email-service")
            val errorForm = editMandateDetailsForm.withError(key = "agent-enter-email-form", message = errorMsg).fill(editMandate)
            Future.successful(BadRequest(views.html.agent.editClient(errorForm, service, mandateId)))
          }
        }
      }
    )
  }
}

object EditMandateDetailsController extends EditMandateDetailsController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val acmService = AgentClientMandateService
  val emailService = EmailService
  // $COVERAGE-ON$
}
