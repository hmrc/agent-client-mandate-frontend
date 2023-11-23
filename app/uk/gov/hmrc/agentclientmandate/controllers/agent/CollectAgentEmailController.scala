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

import play.api.i18n.Messages

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.{DelegationUtils, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentEmailForm._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientMandateDisplayDetails}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CollectAgentEmailController @Inject()(
                                           mcc: MessagesControllerComponents,
                                           val authConnector: AuthConnector,
                                           dataCacheService: DataCacheService,
                                           implicit val ec: ExecutionContext,
                                           implicit val appConfig: AppConfig,
                                           templateAgentEnterEmail: views.html.agent.agentEnterEmail
                                           ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  def addClient(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[ClientMandateDisplayDetails](agentRefCacheId) map {
          case Some(clientMandateDisplayDetails) => Ok(templateAgentEnterEmail(
            agentEmailForm.fill(AgentEmail(clientMandateDisplayDetails.agentLastUsedEmail)), service, None, getBackLink(None)))
          case None => Ok(templateAgentEnterEmail(agentEmailForm, service, None, getBackLink(None)))
        }
      }
  }

  def view(service: String, redirectUrl: Option[RedirectUrl]): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        for {
          agentEmailCached <- dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId)
        } yield {
            redirectUrl match {
              case Some(providedUrl) =>
                DelegationUtils.getSafeLink(providedUrl, appConfig.environment) match {
                  case Some(safeLink) =>
                    processViewRequest(service, agentEmailCached, redirectUrl, Some(safeLink))
                  case None => BadRequest("The return url is not correctly formatted")
                }
              case _ => processViewRequest(service, agentEmailCached)
          }
        }
      }
 }

  private def processViewRequest(service: String, agentEmailCached : Option[AgentEmail],
                                 redirectUrl: Option[RedirectUrl] = None, safeLink: Option[String] = None)
           (implicit request: Request[_], messages: Messages) = {
    agentEmailCached match {
      case Some(email) => Ok(templateAgentEnterEmail(agentEmailForm.fill(email), service, redirectUrl, getBackLink(safeLink)))
      case None => Ok(templateAgentEnterEmail(agentEmailForm, service, redirectUrl, getBackLink(safeLink)))
    }
  }

  def editFromSummary(service: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      for {
        agentEmail <- dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId)
        callingPage <- dataCacheService.fetchAndGetFormData[String](callingPageCacheId)
      } yield {
        agentEmail match {
          case Some(agentEmail) => Ok(templateAgentEnterEmail(agentEmailForm.fill(AgentEmail(agentEmail.email)), service, None, getBackLink(
            Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.MandateDetailsController.view(callingPage.getOrElse("")).url))))
          case None => Ok(templateAgentEnterEmail(agentEmailForm, service, None, getBackLink(None)))
        }
      }
    }
  }

  def submit(service: String, redirectUrl: Option[RedirectUrl]): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        redirectUrl match {
          case Some(providedUrl) =>
            DelegationUtils.getSafeLink(providedUrl, appConfig.environment) match {
              case Some(safeLink) =>
                processSubmitRequest(service, redirectUrl, Some(safeLink))
              case None => Future.successful(BadRequest("The return url is not correctly formatted"))
            }
          case None => processSubmitRequest(service)
        }
      }
  }

  private def processSubmitRequest(service : String, redirectUrl : Option[RedirectUrl] = None, safeLink: Option[String] = None)
                            (implicit request : Request[_], messages : Messages) = {
    agentEmailForm.bindFromRequest().fold(
      formWithError => {
        Future.successful(BadRequest(templateAgentEnterEmail(formWithError, service, redirectUrl, getBackLink(safeLink))))
      },
      data => {
        dataCacheService.cacheFormData[AgentEmail](agentEmailFormId, data) flatMap { _ =>
          redirectUrl match {
            case Some(_) => Future.successful(Redirect(safeLink.getOrElse("")))
            case None => Future.successful(Redirect(routes.ClientDisplayNameController.view()))
          }
        }
      })
  }

  def getAgentEmail(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId).map { agentEmail =>
          Ok(Json.toJson(agentEmail))
        }
      }
  }

  private def getBackLink(redirectUrl: Option[String]):Option[String] = {
    redirectUrl match {
      case Some(x) => Some(x)
      case None => Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view().url)
    }
  }

}
