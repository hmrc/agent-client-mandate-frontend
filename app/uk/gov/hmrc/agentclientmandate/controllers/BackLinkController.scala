/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.controllers

import uk.gov.hmrc.agentclientmandate.connectors.BackLinkCacheConnector
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait BackLinkController {
  val controllerId: String
  val backLinkCacheConnector: BackLinkCacheConnector

  def setBackLink(pageId: String, returnUrl: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
    backLinkCacheConnector.saveBackLink(pageId, returnUrl)

  def currentBackLink(implicit hc: HeaderCarrier, ec: ExecutionContext):Future[Option[String]] =
    backLinkCacheConnector.fetchAndGetBackLink(controllerId)

  def redirectToExternal(redirectCall: String, returnUrl: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    setBackLink("ExternalLinkController", returnUrl) map (_ => Redirect(redirectCall))

  def forwardBackLinkToNextPage(nextPageId: String, redirectCall: Call)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    for {
      currentBackLink <- currentBackLink
      _ <- setBackLink(nextPageId, currentBackLink)
    } yield{
      Redirect(redirectCall)
    }
  }

  def redirectWithBackLink(nextPageId: String, redirectCall: Call, backCall: Option[String])
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    setBackLink(nextPageId, backCall) map (_ => Redirect(redirectCall))
}
