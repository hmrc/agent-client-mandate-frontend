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
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.config.{FrontendAppConfig, FrontendAuthConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.{DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientDisplayNameForm._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayName}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.binders.ContinueUrl

import scala.concurrent.Future

object ClientDisplayNameController extends ClientDisplayNameController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  val emailService: EmailService = EmailService
  // $COVERAGE-ON$
}

trait ClientDisplayNameController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def authConnector: AuthConnector

  def view(service: String, redirectUrl: Option[ContinueUrl]) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit user =>
      implicit request =>
        redirectUrl match {
          case Some(x) if !x.isRelativeOrDev(FrontendAppConfig.env) => Future.successful(BadRequest("The return url is not correctly formatted"))
          case _ =>
            dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId) map {
              case Some(clientDisplayname) => Ok(views.html.agent.clientDisplayName(clientDisplayNameForm.fill(clientDisplayname), service, redirectUrl, getBackLink(service, redirectUrl)))
              case None => Ok(views.html.agent.clientDisplayName(clientDisplayNameForm, service, redirectUrl, getBackLink(service, redirectUrl)))
            }
        }
  }


  def editFromSummary(service: String, redirectUrl: Option[ContinueUrl]) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit user =>
      implicit request =>
        for {
          clientDisplayName <- dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId)
          callingPage <- dataCacheService.fetchAndGetFormData[String](callingPageCacheId)
        } yield {
          redirectUrl match {
            case Some(x) if !x.isRelativeOrDev(FrontendAppConfig.env) => BadRequest("The return url is not correctly formatted")
            case _ =>
              clientDisplayName match {
                case Some(clientDisplayname) => Ok(views.html.agent.clientDisplayName(clientDisplayNameForm.fill(clientDisplayname), service, redirectUrl, getBackLink(service,
                  Some(ContinueUrl(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.MandateDetailsController.view(callingPage.getOrElse("")).url)))))
                case None => Ok(views.html.agent.clientDisplayName(clientDisplayNameForm, service, redirectUrl, getBackLink(service, redirectUrl)))
              }
          }
        }
  }


  def submit(service: String, redirectUrl: Option[ContinueUrl]) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext =>
      implicit request =>

      redirectUrl match {
        case Some(x) if !x.isRelativeOrDev(FrontendAppConfig.env) => Future.successful(BadRequest("The return url is not correctly formatted"))
        case _ =>
          clientDisplayNameForm.bindFromRequest.fold(
            formWithError => Future.successful(BadRequest(views.html.agent.clientDisplayName(formWithError, service, redirectUrl, getBackLink(service, redirectUrl)))),
            data =>
              dataCacheService.cacheFormData[ClientDisplayName](clientDisplayNameFormId, data) map { cachedData =>
                redirectUrl match {
                  case Some(redirect) => Redirect(redirect.url)
                  case None => Redirect(routes.OverseasClientQuestionController.view())
                }
              })
      }
  }

  def getClientDisplayName(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext =>
      implicit request =>
        dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId).map { displayName =>
          Ok(Json.toJson(displayName))
        }
  }

  private def getBackLink(service: String, redirectUrl: Option[ContinueUrl]): Option[String] = {
    redirectUrl match {
      case Some(x) => Some(x.url)
      case None => Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.CollectAgentEmailController.view().url)
    }
  }
}
