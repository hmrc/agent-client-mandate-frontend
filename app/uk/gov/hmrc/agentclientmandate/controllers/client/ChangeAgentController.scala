/*
 * Copyright 2017 HM Revenue & Customs
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

import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{YesNoQuestion, YesNoQuestionForm}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.Future


object ChangeAgentController extends ChangeAgentController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val acmService = AgentClientMandateService
  val dataCacheService = DataCacheService
  // $COVERAGE-ON$
}


trait ChangeAgentController extends FrontendController with Actions{

  def acmService: AgentClientMandateService
  def dataCacheService: DataCacheService

  def view(service: String, mandateId: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      acmService.fetchClientMandateAgentName(mandateId).map(
        agentName =>
          Ok(views.html.client.changeAgent(service, new YesNoQuestionForm("client.agent-change.error").yesNoQuestionForm, agentName))
      )
  }

  def submit(service: String, mandateId: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      val form = new YesNoQuestionForm("client.agent-change.error")
      form.yesNoQuestionForm.bindFromRequest.fold(
        formWithError =>
          acmService.fetchClientMandateAgentName(mandateId).map(
            agentName =>
              BadRequest(views.html.client.changeAgent(service, formWithError, agentName))
          ),
        data => {
          val changeAgent = data.yesNo.getOrElse(false)
          if (changeAgent) {
            Future.successful(Redirect(routes.CollectEmailController.view(service)))
          }
          else {
            Future.successful(Redirect(routes.RemoveAgentController.confirmation(service, mandateId)))
          }
        }
      )
  }
}
