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

package uk.gov.hmrc.agentclientmandate.controllers.client

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.connectors.DelegationConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.MandateAuthRetrievals
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveAgentController @Inject()(
                                       acmService: AgentClientMandateService,
                                       dataCacheService: DataCacheService,
                                       delegationConnector: DelegationConnector,
                                       mcc: MessagesControllerComponents,
                                       val authConnector: AuthConnector,
                                       implicit val ec: ExecutionContext,
                                       implicit val appConfig: AppConfig,
                                       templateRemoveAgent: views.html.client.removeAgent,
                                       templateRemoveAgentConfirmation: views.html.client.removeAgentConfirmation,
                                     ) extends FrontendController(mcc) with AuthorisedWrappers with I18nSupport {

  def view(service: String, mandateId: String, returnUrl: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { authRetrievals =>
        dataCacheService.cacheFormData[String]("RETURN_URL", returnUrl).flatMap { _ =>
          showView(service, mandateId, Some(returnUrl), authRetrievals)
        }
      }
  }

  private def showView(service: String,
                       mandateId: String,
                       backLink: Option[String],
                       authRetrievals: MandateAuthRetrievals)(implicit request: Request[AnyContent]): Future[Result] = {

    acmService.fetchClientMandate(mandateId, authRetrievals).map {
      case Some(mandate) => Ok(templateRemoveAgent(
        service = service,
        removeAgentForm = new YesNoQuestionForm("yes-no.error.mandatory.removeAgent").yesNoQuestionForm,
        agentName = mandate.agentParty.name,
        mandateId = mandateId,
        backLink = backLink))
      case _ => throw new RuntimeException("No Mandate returned")
    }
  }

  def submit(service: String, mandateId: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { authRetrievals =>
        val form = new YesNoQuestionForm("yes-no.error.mandatory.removeAgent")
        form.yesNoQuestionForm.bindFromRequest.fold(
          formWithError =>
            acmService.fetchClientMandateAgentName(mandateId, authRetrievals).flatMap(
              agentName =>
                dataCacheService.fetchAndGetFormData[String]("RETURN_URL").map { returnUrl =>
                  BadRequest(templateRemoveAgent(service, formWithError, agentName, mandateId, returnUrl))
                }
            ),
          data => {
            if (data.yesNo) {
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
            Ok(templateRemoveAgentConfirmation(service, agentName, mandateId))
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
