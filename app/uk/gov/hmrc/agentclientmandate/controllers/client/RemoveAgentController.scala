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

package uk.gov.hmrc.agentclientmandate.controllers.client

import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.agentclientmandate.config.{ConcreteAuthConnector, FrontendAppConfig}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.MandateAuthRetrievals
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object RemoveAgentController extends RemoveAgentController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = ConcreteAuthConnector
  val acmService: AgentClientMandateService = AgentClientMandateService
  val dataCacheService: DataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait RemoveAgentController extends FrontendController with AuthorisedWrappers {

  def acmService: AgentClientMandateService

  def dataCacheService: DataCacheService


  def view(service: String, mandateId: String, returnUrl: ContinueUrl): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { authRetrievals =>
        if (!returnUrl.isRelativeOrDev(FrontendAppConfig.env)) {
          Future.successful(BadRequest("The return url is not correctly formatted"))
        }
        else {
          dataCacheService.cacheFormData[String]("RETURN_URL", returnUrl.url).flatMap { _ =>
            showView(service, mandateId, Some(returnUrl.url), authRetrievals)
          }
        }
      }
  }

  private def showView(service: String,
                       mandateId: String,
                       backLink: Option[String],
                       authRetrievals: MandateAuthRetrievals)(implicit request: Request[AnyContent]): Future[Result] = {

    acmService.fetchClientMandate(mandateId, authRetrievals).map {
      case Some(mandate) => Ok(views.html.client.removeAgent(
        service = service,
        removeAgentForm = new YesNoQuestionForm("client.remove-agent.error").yesNoQuestionForm,
        agentName = mandate.agentParty.name,
        mandateId = mandateId,
        backLink = backLink))
      case _ => throw new RuntimeException("No Mandate returned")
    }
  }

  def submit(service: String, mandateId: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { authRetrievals =>
        val form = new YesNoQuestionForm("client.remove-agent.error")
        form.yesNoQuestionForm.bindFromRequest.fold(
          formWithError =>
            acmService.fetchClientMandateAgentName(mandateId, authRetrievals).flatMap(
              agentName =>
                dataCacheService.fetchAndGetFormData[String]("RETURN_URL").map { returnUrl =>
                  BadRequest(views.html.client.removeAgent(service, formWithError, agentName, mandateId, returnUrl))
                }
            ),
          data => {
            val removeAgent = data.yesNo.getOrElse(false)
            if (removeAgent) {
              acmService.removeAgent(mandateId, authRetrievals).map { removedAgent =>
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
  }

  def confirmation(service: String, mandateId: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { authRetrievals =>
        acmService.fetchClientMandateAgentName(mandateId, authRetrievals).map(
          agentName =>
            Ok(views.html.client.removeAgentConfirmation(service, agentName, mandateId))
        )
      }
  }

  def returnToService: Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(None) { _ =>
        dataCacheService.fetchAndGetFormData[String]("RETURN_URL").map {
          case Some(x) => Redirect(x)
          case _ => throw new RuntimeException("Cache Retrieval Failed")
        }
      }
  }

}
