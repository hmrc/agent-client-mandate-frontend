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

import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.agentclientmandate.config.{FrontendAppConfig, FrontendAuthConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object RemoveAgentController extends RemoveAgentController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val acmService = AgentClientMandateService
  val dataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait RemoveAgentController extends FrontendController with Actions {

  def acmService: AgentClientMandateService

  def dataCacheService: DataCacheService


  def view(service: String, mandateId: String, returnUrl: ContinueUrl) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>

      if (!returnUrl.isRelativeOrDev(FrontendAppConfig.env)) {
        Future.successful(BadRequest("The return url is not correctly formatted"))
      }
      else {
        dataCacheService.cacheFormData[String]("RETURN_URL", returnUrl.url).flatMap { cache =>
          showView(service, mandateId, Some(returnUrl.url))
        }
      }
  }

  private def showView(service: String,
                       mandateId: String,
                       backLink: Option[String])(implicit ac: AuthContext, request: Request[AnyContent]) = {

    acmService.fetchClientMandate(mandateId).map {
      case Some(mandate) => Ok(views.html.client.removeAgent(
        service = service,
        removeAgentForm = new YesNoQuestionForm("client.remove-agent.error").yesNoQuestionForm,
        agentName = mandate.agentParty.name,
        mandateId = mandateId,
        backLink = backLink))
      case _ => throw new RuntimeException("No Mandate returned")
    }
  }

  def submit(service: String, mandateId: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      val form = new YesNoQuestionForm("client.remove-agent.error")
      form.yesNoQuestionForm.bindFromRequest.fold(
        formWithError =>
          acmService.fetchClientMandateAgentName(mandateId).flatMap(
            agentName =>
              dataCacheService.fetchAndGetFormData[String]("RETURN_URL").map { returnUrl =>
                BadRequest(views.html.client.removeAgent(service, formWithError, agentName, mandateId, returnUrl))
              }
          ),
        data => {
          val removeAgent = data.yesNo.getOrElse(false)
          if (removeAgent) {
            acmService.removeAgent(mandateId).map { removedAgent =>
              if (removedAgent) Redirect(routes.ChangeAgentController.view(mandateId))
              else throw new RuntimeException("Agent Removal Failed")
            }
          }
          else {
            dataCacheService.fetchAndGetFormData[String]("RETURN_URL").map {
              case Some(x) => Redirect(x)
              case _ => throw new RuntimeException(s"Cache Retrieval Failed with id $mandateId")
            }
          }
        }
      )
  }

  def confirmation(service: String, mandateId: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      acmService.fetchClientMandateAgentName(mandateId).map(
        agentName =>
          Ok(views.html.client.removeAgentConfirmation(service, agentName))
      )
  }

  def returnToService = AuthorisedFor(ClientRegime(), GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheService.fetchAndGetFormData[String]("RETURN_URL").map {
        case Some(x) => Redirect(x)
        case _ => throw new RuntimeException("Cache Retrieval Failed")
      }
  }

}
