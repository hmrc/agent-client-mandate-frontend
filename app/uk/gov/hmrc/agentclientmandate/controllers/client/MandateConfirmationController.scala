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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentclientmandate.config.ConcreteAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.Mandate
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

object MandateConfirmationController extends MandateConfirmationController {
  // $COVERAGE-OFF$
  val dataCacheService: DataCacheService.type = DataCacheService
  override def authConnector: AuthConnector = ConcreteAuthConnector
  // $COVERAGE-ON$
}

trait MandateConfirmationController extends FrontendController with MandateConstants with AuthorisedWrappers {

  def dataCacheService: DataCacheService

  def view(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[Mandate](clientApprovedMandateId) map {
          case Some(x) => Ok(views.html.client.mandateConfirmation(x.agentParty.name, x.subscription.service.name))
          case None => Redirect(routes.ReviewMandateController.view())
        }
      }
  }

}
