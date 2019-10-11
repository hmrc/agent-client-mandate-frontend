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
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentclientmandate.config.{ConcreteAuthConnector, FrontendAppConfig}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmailForm._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientMandateDisplayDetails}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object CollectAgentEmailController extends CollectAgentEmailController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = ConcreteAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait CollectAgentEmailController extends FrontendController with AuthorisedWrappers with MandateConstants {

  def dataCacheService: DataCacheService

  def addClient(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[ClientMandateDisplayDetails](agentRefCacheId) map {
          case Some(clientMandateDisplayDetails) => Ok(views.html.agent.agentEnterEmail(agentEmailForm.fill(AgentEmail(clientMandateDisplayDetails.agentLastUsedEmail)), service, None, getBackLink(service, None)))
          case None => Ok(views.html.agent.agentEnterEmail(agentEmailForm, service, None, getBackLink(service, None)))
        }
      }
  }

  def view(service: String, redirectUrl: Option[ContinueUrl]): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
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
  }

  def editFromSummary(service: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      for {
        agentEmail <- dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId)
        callingPage <- dataCacheService.fetchAndGetFormData[String](callingPageCacheId)
      } yield {
        agentEmail match {
          case Some(agentEmail) => Ok(views.html.agent.agentEnterEmail(agentEmailForm.fill(AgentEmail(agentEmail.email)), service, None, getBackLink(service,
            Some(ContinueUrl(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.MandateDetailsController.view(callingPage.getOrElse("")).url)))))
          case None => Ok(views.html.agent.agentEnterEmail(agentEmailForm, service, None, getBackLink(service, None)))
        }
      }
    }
  }

  def submit(service: String, redirectUrl: Option[ContinueUrl]): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        redirectUrl match {
          case Some(x) if !x.isRelativeOrDev(FrontendAppConfig.env) => Future.successful(BadRequest("The return url is not correctly formatted"))
          case _ =>
            agentEmailForm.bindFromRequest.fold(
              formWithError => {
                Future.successful(BadRequest(views.html.agent.agentEnterEmail(formWithError, service, redirectUrl, getBackLink(service, redirectUrl))))
              },
              data => {
                dataCacheService.cacheFormData[AgentEmail](agentEmailFormId, data) flatMap { _ =>
                  redirectUrl match {
                    case Some(redirect) => Future.successful(Redirect(redirect.url))
                    case None => Future.successful(Redirect(routes.ClientDisplayNameController.view()))
                  }
                }
              })
        }
      }
  }

  def getAgentEmail(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId).map { agentEmail =>
          Ok(Json.toJson(agentEmail))
        }
      }
  }

  private def getBackLink(service: String, redirectUrl: Option[ContinueUrl]):Option[String] = {
    redirectUrl match {
      case Some(x) => Some(x.url)
      case None => Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view().url)
    }
  }

}
