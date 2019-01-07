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

import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.config.{FrontendAppConfig, FrontendAuthConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.{DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientMandateDisplayDetails}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmailForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.emailaddress._

import scala.concurrent.Future

object CollectAgentEmailController extends CollectAgentEmailController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait CollectAgentEmailController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def addClient(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit user => implicit request =>
      dataCacheService.fetchAndGetFormData[ClientMandateDisplayDetails](agentRefCacheId) map {
        case Some(clientMandateDisplayDetails) => Ok(views.html.agent.agentEnterEmail(agentEmailForm.fill(AgentEmail(clientMandateDisplayDetails.agentLastUsedEmail)), service, None, getBackLink(service, None)))
        case None => Ok(views.html.agent.agentEnterEmail(agentEmailForm, service,  None, getBackLink(service, None)))
      }
  }

  def view(service: String, redirectUrl: Option[ContinueUrl]) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit user => implicit request =>
      for {
        agentEmailCached <- dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId)
      } yield {
        redirectUrl match {
          case Some(url) if !url.isRelativeOrDev(FrontendAppConfig.env) => BadRequest("The return url is not correctly formatted")
          case _ =>
            agentEmailCached match {
              case Some(email) => Ok(views.html.agent.agentEnterEmail(agentEmailForm.fill(email), service, redirectUrl, getBackLink(service, redirectUrl)))
              case None => Ok(views.html.agent.agentEnterEmail(agentEmailForm, service, redirectUrl, getBackLink(service, redirectUrl)))
            }
        }
      }
  }

  def editFromSummary(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async { implicit user => implicit request =>
    for {
      agentEmail <- dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId)
      callingPage <- dataCacheService.fetchAndGetFormData[String](callingPageCacheId)
    } yield {
      agentEmail match {
        case Some(agentEmail) => Ok(views.html.agent.agentEnterEmail(agentEmailForm.fill(AgentEmail(agentEmail.email)), service, None, getBackLink(service,
          Some(ContinueUrl(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.MandateDetailsController.view(callingPage.getOrElse("")).url)))))
        case None => Ok(views.html.agent.agentEnterEmail(agentEmailForm, service,  None, getBackLink(service, None)))
      }
    }
  }

  def submit(service: String, redirectUrl: Option[ContinueUrl]) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      redirectUrl match {
        case Some(x) if !x.isRelativeOrDev(FrontendAppConfig.env) => Future.successful(BadRequest("The return url is not correctly formatted"))
        case _ =>
          agentEmailForm.bindFromRequest.fold(
            formWithError => {
              Future.successful(BadRequest(views.html.agent.agentEnterEmail(formWithError, service, redirectUrl, getBackLink(service, redirectUrl))))
            },
            data => {
                if (EmailAddress.isValid(data.email)) {
                  dataCacheService.cacheFormData[AgentEmail](agentEmailFormId, data) flatMap { cachedData =>
                    redirectUrl match {
                      case Some(redirect) => Future.successful(Redirect(redirect.url))
                      case None => Future.successful(Redirect(routes.ClientDisplayNameController.view()))
                    }
                  }
                } else {
                  val errorMsg = Messages("agent.enter-email.error.email.invalid-by-email-service")
                  val errorForm = agentEmailForm.withError(key = "email", message = errorMsg).fill(data)
                  Future.successful(BadRequest(views.html.agent.agentEnterEmail(errorForm, service, redirectUrl, getBackLink(service, redirectUrl))))
                }
            })
      }
  }

  def getAgentEmail(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId).map { agentEmail =>
          Ok(Json.toJson(agentEmail))
      }
  }

  private def getBackLink(service: String, redirectUrl: Option[ContinueUrl]):Option[String] = {
    redirectUrl match {
      case Some(x) => Some(x.url)
      case None => Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view().url)
    }
  }

}
