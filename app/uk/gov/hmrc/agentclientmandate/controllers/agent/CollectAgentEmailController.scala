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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.{AgentClientMandateUtils, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmailForm._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientMandateDisplayDetails}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CollectAgentEmailController @Inject()(
                                           mcc: MessagesControllerComponents,
                                           val authConnector: AuthConnector,
                                           dataCacheService: DataCacheService,
                                           implicit val ec: ExecutionContext,
                                           implicit val appConfig: AppConfig
                                           ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  def addClient(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[ClientMandateDisplayDetails](agentRefCacheId) map {
          case Some(clientMandateDisplayDetails) => Ok(views.html.agent.agentEnterEmail(
            agentEmailForm.fill(AgentEmail(clientMandateDisplayDetails.agentLastUsedEmail)), service, None, getBackLink(service, None)))
          case None => Ok(views.html.agent.agentEnterEmail(agentEmailForm, service, None, getBackLink(service, None)))
        }
      }
  }

  def view(service: String, redirectUrl: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        for {
          agentEmailCached <- dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId)
        } yield {
          redirectUrl match {
            case Some(url) if !AgentClientMandateUtils.isRelativeOrDev(url) => BadRequest("The return url is not correctly formatted")
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
            Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.MandateDetailsController.view(callingPage.getOrElse("")).url))))
          case None => Ok(views.html.agent.agentEnterEmail(agentEmailForm, service, None, getBackLink(service, None)))
        }
      }
    }
  }

  def submit(service: String, redirectUrl: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        redirectUrl match {
          case Some(x) if !AgentClientMandateUtils.isRelativeOrDev(x) => Future.successful(BadRequest("The return url is not correctly formatted"))
          case _ =>
            agentEmailForm.bindFromRequest.fold(
              formWithError => {
                Future.successful(BadRequest(views.html.agent.agentEnterEmail(formWithError, service, redirectUrl, getBackLink(service, redirectUrl))))
              },
              data => {
                dataCacheService.cacheFormData[AgentEmail](agentEmailFormId, data) flatMap { _ =>
                  redirectUrl match {
                    case Some(redirect) => Future.successful(Redirect(redirect))
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

  private def getBackLink(service: String, redirectUrl: Option[String]):Option[String] = {
    redirectUrl match {
      case Some(x) => Some(x)
      case None => Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view().url)
    }
  }

}
