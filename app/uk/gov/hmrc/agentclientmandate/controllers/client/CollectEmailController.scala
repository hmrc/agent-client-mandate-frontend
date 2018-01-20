/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.i18n.Messages
import play.api.libs.json.Format
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.agentclientmandate.config.{FrontendAppConfig, FrontendAuthConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.service.{DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.utils.{DelegationUtils, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, YesNoQuestionForm}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientEmailForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.binders.ContinueUrl

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object CollectEmailController extends CollectEmailController {
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  val emailService: EmailService = EmailService
}

trait CollectEmailController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def emailService: EmailService

  def view(service: String, redirectUrl: Option[ContinueUrl]) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      redirectUrl match {
        case Some(x) if !x.isRelativeOrDev(FrontendAppConfig.env) => Future.successful(BadRequest("The return url is not correctly formatted"))
        case Some(x) =>
          saveBackLink(service, Some(x.url)).flatMap { cache =>
            showView(service, None)
          }
        case _ =>
          saveBackLink(service, None).flatMap { cache =>
            showView(service, None)
          }
      }
  }

  def edit(service: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      showView(service, Some("edit"))
  }

  def back(service: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      showView(service, None)
  }

  private def showView(service: String, mode: Option[String])(implicit ac: AuthContext, request: Request[AnyContent]) = {

    for {
      cachedData <- dataCacheService.fetchAndGetFormData[ClientCache](clientFormId)
      backLink <- getBackLink(service, mode)
    } yield {
      val filledForm = cachedData.flatMap(_.email) match {
        case Some(x) => clientEmailForm.fill(x)
        case None => clientEmailForm
      }
      Ok(views.html.client.collectEmail(service, filledForm, mode, backLink))
    }
  }

  def submit(service: String, mode: Option[String]) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      clientEmailForm.bindFromRequest.fold(
        formWithError =>
          getBackLink(service, mode).map{
            backLink =>
              BadRequest(views.html.client.collectEmail(service, formWithError, mode, backLink))
          },
        data => {
          emailService.validate(data.email) flatMap { isValidEmail =>
            if (isValidEmail) {
              dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) flatMap {
                case Some(x) => dataCacheService.cacheFormData[ClientCache](clientFormId, x.copy(email = Some(data))) flatMap { cachedData =>
                  Future.successful(redirect(service, mode))
                }
                case None => dataCacheService.cacheFormData[ClientCache](clientFormId, ClientCache(email = Some(data))) flatMap { cachedData =>
                  Future.successful(redirect(service, mode))
                }
              }
            } else {
              val errorMsg = Messages("client.collect-email.error.email.invalid-by-email-service")
              val errorForm = clientEmailForm.withError(key = "client-collect-email-form", message = errorMsg).fill(data)
              getBackLink(service, mode).map{
                backLink =>
                  BadRequest(views.html.client.collectEmail(service, errorForm, mode, backLink))
              }
            }
          }
        }
      )
  }

  private def redirect(service: String, mode: Option[String]) = {
    mode match {
      case Some("edit") => Redirect(routes.ReviewMandateController.view())
      case _ => Redirect(routes.SearchMandateController.view())
    }
  }

  val backLinkId = "CollectEmailController:BackLink"
  private def saveBackLink(service: String, redirectUrl: Option[String])(implicit hc: _root_.uk.gov.hmrc.http.HeaderCarrier) = {
    dataCacheService.cacheFormData[String](backLinkId, redirectUrl.getOrElse(DelegationUtils.getDelegatedServiceRedirectUrl(service)))
  }

  private def getBackLink(service: String, mode: Option[String])(implicit hc: HeaderCarrier, ac: AuthContext, request: Request[AnyContent]) :Future[Option[String]]= {
    mode match {
      case Some("edit") => Future.successful(Some(routes.ReviewMandateController.view().url))
      case _ => {
        dataCacheService.fetchAndGetFormData[String](backLinkId).map(backLink =>
          backLink match {
            case Some(x) if (!x.trim.isEmpty) => backLink
            case _ => None
          }
        )
      }
    }
  }
}
