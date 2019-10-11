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

import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentclientmandate.config.ConcreteAuthConnector
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object GGBreakingRelationshipController extends FrontendController with AuthorisedWrappers {

  def agentClientMandateConnector: AgentClientMandateConnector = AgentClientMandateConnector
  override val authConnector: ConcreteAuthConnector.type = ConcreteAuthConnector

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
