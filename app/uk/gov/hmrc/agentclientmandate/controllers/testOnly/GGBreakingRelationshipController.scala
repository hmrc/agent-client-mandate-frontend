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

package uk.gov.hmrc.agentclientmandate.controllers.testOnly

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GGBreakingRelationshipController @Inject()(
                                                  mcc: MessagesControllerComponents,
                                                  agentClientMandateConnector: AgentClientMandateConnector,
                                                  val authConnector: AuthConnector,
                                                  implicit val appConfig: AppConfig,
                                                  implicit val ec: ExecutionContext
                                                ) extends FrontendController(mcc) with AuthorisedWrappers {

  def view(): Action[AnyContent] = Action.async {implicit request =>
    withAgentRefNumber(None) { _ =>
      val result = Ok(views.html.testOnly.checkBreakingRelationships())
      Future.successful(result)
    }
  }

  def submit(): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(None) { agentAuthRetrievals =>
        agentClientMandateConnector.remove(
          request.body.asFormUrlEncoded.get.apply("mandateId").head,
          agentAuthRetrievals
        ).map { x =>
          Logger.info("********" + x.body + "*************")
          Redirect(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view())
        }
      }
  }

}
