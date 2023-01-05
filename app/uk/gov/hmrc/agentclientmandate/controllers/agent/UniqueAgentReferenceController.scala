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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientMandateDisplayDetails
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class UniqueAgentReferenceController @Inject()(
                                                val authConnector: AuthConnector,
                                                val dataCacheService: DataCacheService,
                                                val mcc: MessagesControllerComponents,
                                                implicit val ec: ExecutionContext,
                                                implicit val appConfig: AppConfig,
                                                templateUniqueAgentReference: views.html.agent.uniqueAgentReference
                                              ) extends FrontendController(mcc) with MandateConstants with AuthorisedWrappers {

  def view(service: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      dataCacheService.fetchAndGetFormData[ClientMandateDisplayDetails](agentRefCacheId) map {
        case Some(x) => Ok(templateUniqueAgentReference(x, service))
        case None => Redirect(routes.SelectServiceController.view)
      }
    }
  }

}
