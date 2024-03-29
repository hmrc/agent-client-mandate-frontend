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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.DelegationUtils
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChangeAgentController @Inject()(
                                       val acmService: AgentClientMandateService,
                                       val dataCacheService: DataCacheService,
                                       val mcc: MessagesControllerComponents,
                                       val authConnector: AuthConnector,
                                       implicit val ec: ExecutionContext,
                                       implicit val appConfig: AppConfig,
                                       templateChangeAgent: views.html.client.changeAgent
                                     ) extends FrontendController(mcc) with AuthorisedWrappers {

  def view(service: String, mandateId: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { _ =>
        val result = Ok(templateChangeAgent(service, new YesNoQuestionForm("yes-no.error.mandatory.changeAgent").yesNoQuestionForm,
          mandateId,
          Some(DelegationUtils.getDelegatedServiceRedirectUrl(service))))
        Future.successful(result)
      }
  }

  def submit(service: String, mandateId: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { _ =>
        val form = new YesNoQuestionForm("yes-no.error.mandatory.changeAgent")
        form.yesNoQuestionForm.bindFromRequest().fold(
          formWithError =>
            Future.successful(BadRequest(templateChangeAgent(service, formWithError,
              mandateId,
              Some(DelegationUtils.getDelegatedServiceRedirectUrl(service))))
            ),
          data => {
            if (data.yesNo) {
              val backLink = routes.ChangeAgentController.view(mandateId).url
              Future.successful(Redirect(routes.CollectEmailController.view(Some(RedirectUrl(backLink)))))
            }
            else {
              Future.successful(Redirect(routes.RemoveAgentController.confirmation(mandateId)))
            }
          }
        )
      }
  }

}
