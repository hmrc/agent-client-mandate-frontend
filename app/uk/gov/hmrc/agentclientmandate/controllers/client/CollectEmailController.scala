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

package uk.gov.hmrc.agentclientmandate.controllers.client

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.{AgentClientMandateUtils, DelegationUtils, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientCache
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientEmailForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CollectEmailController @Inject()(val dataCacheService: DataCacheService,
                                       val mcc: MessagesControllerComponents,
                                       val authConnector: AuthConnector,
                                       implicit val ec: ExecutionContext,
                                       implicit val appConfig: AppConfig,
                                       templateCollectEmail: views.html.client.collectEmail
                                      ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants with I18nSupport {

  def view(service: String, redirectUrl: Option[RedirectUrl]): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { _ =>
        redirectUrl match {
          case Some(providedUrl) =>
            AgentClientMandateUtils.getSafeLink(providedUrl, appConfig.environment) match {
              case Some(safeLink) =>
                saveBackLink(service, Some(safeLink)).flatMap { _ =>
                  showView(service, None)
              }
              case None => Future.successful(BadRequest("The return url is not correctly formatted"))
          }
          case _ =>
            saveBackLink(service, None).flatMap { _ =>
              showView(service, None)
          }
        }
     }
  }

  def edit(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { _ =>
        showView(service, Some("edit"))
      }
  }

  def back(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { _ =>
        showView(service, None)
      }
  }

  private def showView(service: String, mode: Option[String])(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      cachedData <- dataCacheService.fetchAndGetFormData[ClientCache](clientFormId)
      backLink <- getBackLink(mode)
    } yield {
      val filledForm = cachedData.flatMap(_.email) match {
        case Some(x) => clientEmailForm.fill(x)
        case None => clientEmailForm
      }
      Ok(templateCollectEmail(service, filledForm, mode, backLink))
    }
  }

  def submit(service: String, mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { _ =>
        clientEmailForm.bindFromRequest().fold(
          formWithError =>
            getBackLink(mode).map {
              backLink =>
                BadRequest(templateCollectEmail(service, formWithError, mode, backLink))
            },
          data => {
            dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) flatMap {
              case Some(x) => dataCacheService.cacheFormData[ClientCache](clientFormId, x.copy(email = Some(data))) flatMap { _ =>
                Future.successful(redirect(service, mode))
              }
              case None => dataCacheService.cacheFormData[ClientCache](clientFormId, ClientCache(email = Some(data))) flatMap { _ =>
                Future.successful(redirect(service, mode))
              }
            }
          }
        )
      }
  }

  private def redirect(service: String, mode: Option[String]): Result = {
    mode match {
      case Some("edit") => Redirect(routes.ReviewMandateController.view())
      case _ => Redirect(routes.SearchMandateController.view())
    }
  }

  val backLinkId = "CollectEmailController:BackLink"
  private def saveBackLink(service: String, redirectUrl: Option[String])(implicit hc: _root_.uk.gov.hmrc.http.HeaderCarrier): Future[String] = {
    dataCacheService.cacheFormData[String](backLinkId, redirectUrl.getOrElse(DelegationUtils.getDelegatedServiceRedirectUrl(service)))
  }

  private def getBackLink(mode: Option[String])(implicit hc: HeaderCarrier): Future[Option[String]] = {
    mode match {
      case Some("edit") => Future.successful(Some(routes.ReviewMandateController.view().url))
      case _ =>
        dataCacheService.fetchAndGetFormData[String](backLinkId).map {
          case backLink @ Some(x) if x.trim.nonEmpty => backLink
          case _ => None
        }
    }
  }
}
