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

package uk.gov.hmrc.agentclientmandate.controllers.auth

import uk.gov.hmrc.agentclientmandate.utils.ValidateUri
import uk.gov.hmrc.agentclientmandate.models.StandardAuthRetrievals
import play.api.Logging
import play.api.mvc.{AnyContent, Request, Result}
import play.api.mvc.Results._
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.models.{AgentAuthRetrievals, ClientAuthRetrievals}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

trait AuthorisedWrappers extends AuthorisedFunctions with Logging {
  private val agentRefEnrolment = "HMRC-AGENT-AGENT"
  private val agentRefIdentifier = "AgentRefNumber"

  lazy private val origin: String = "agent-client-mandate-frontend"

  protected def continueUrl(isAnAgent: Boolean)(implicit appConfig: AppConfig): String = {
    if (isAnAgent) appConfig.loginCallbackAgent else appConfig.loginCallbackClient
  }

  protected def loginUrl(implicit appConfig: AppConfig): String = s"${appConfig.basGatewayHost}/${appConfig.loginPath}"

  private def loginParams(isAnAgent: Boolean)(implicit appConfig: AppConfig): Map[String, Seq[String]] = Map(
    "continue" -> Seq(continueUrl(isAnAgent)),
    "origin" -> Seq(origin)
  )

  private def authErrorHandling(isAnAgent: Boolean = true)
                               (implicit appConfig: AppConfig): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      Redirect(loginUrl, loginParams(isAnAgent))
    case InternalError(e)   =>
      logger.warn(s"[authErrorHandling] Call to auth failed - $e")
      InternalServerError
    case e: AuthorisationException =>
      logger.info(s"[authErrorHandling] Authorisation exception of type: - ${e.getClass.toString}")
      Redirect(loginUrl, loginParams(isAnAgent))
  }

  def agentAuthenticated[A](service: Option[String], retrieval: Retrieval[A])(body: A => Future[Result])
                           (implicit hc: HeaderCarrier, ec: ExecutionContext, appConfig: AppConfig): Future[Result] = {
    authorised(Enrolment(agentRefEnrolment) and AuthProviders(GovernmentGateway) and AffinityGroup.Agent).retrieve(retrieval) {
      body
    }.recover(authErrorHandling())
  }

  def clientAuthenticated[A](service: Option[String], retrieval: Retrieval[A])(body: A => Future[Result])
                            (implicit hc: HeaderCarrier, ec: ExecutionContext, appConfig: AppConfig): Future[Result] = {
    authorised(AffinityGroup.Organisation and AuthProviders(GovernmentGateway)).retrieve(retrieval) {
      body
    }.recover(authErrorHandling(isAnAgent = false))
  }

  def withAgentRefNumber(service: Option[String])(body: AgentAuthRetrievals => Future[Result])
                        (implicit hc: HeaderCarrier, ec: ExecutionContext, appConfig: AppConfig): Future[Result] = {
    agentAuthenticated(service,
      Retrievals.authorisedEnrolments and
        Retrievals.internalId and
        Retrievals.agentCode and
        Retrievals.agentInformation and
        Retrievals.credentials
    ) { retrieval =>
      val enrolments ~ optInternalId ~ optAgentCode ~ agentInformation ~ optCredentials = retrieval

      val optRefValue: Option[String] = enrolments
        .getEnrolment(agentRefEnrolment)
        .flatMap(_.getIdentifier(agentRefIdentifier).map(_.value))

      (optRefValue, optAgentCode, optCredentials, optInternalId) match {
        case (Some(refNumber), Some(agentCode), Some(credential), Some(internalId)) =>
          val agentAuthRetrievals = AgentAuthRetrievals(
            refNumber,
            agentCode,
            agentInformation.agentFriendlyName,
            credential.providerId,
            internalId
          )

          body(agentAuthRetrievals)
        case (None, _, _, _) =>
          logger.warn("[withAgentRefNumber] No agent reference number found for agent")
          Future.successful(InternalServerError)
        case (_, None, _, _) =>
          logger.warn("[withAgentRefNumber] No agent code found for agent")
          Future.successful(InternalServerError)
        case (_, _, None, _) =>
          logger.warn("[withAgentRefNumber] No provider ID found for agent")
          Future.successful(InternalServerError)
        case (_, _, _, None) =>
          logger.warn("[withAgentRefNumber] No internal ID found for agent")
          Future.successful(InternalServerError)
      }
    }
  }

  def withOrgCredId(service: Option[String])(body: ClientAuthRetrievals => Future[Result])
                   (implicit hc: HeaderCarrier, ec: ExecutionContext, appConfig: AppConfig): Future[Result] = {
    clientAuthenticated(service, Retrievals.credentials) {
      case Some(credentials) => body(ClientAuthRetrievals(OrgAuthUtil.hash(credentials.providerId)))
      case _                 =>
        logger.warn("[withOrgCredId] No credential ID found for organisation user")
        Future.successful(InternalServerError)
    }
  }

  private def isValidUrl(appConfig: AppConfig, serviceName: String): Boolean = {
    ValidateUri.isValid(appConfig.serviceList, serviceName)
  }

  private def recoverAuthorisedCalls(implicit appConfig: AppConfig, isAnAgent: Boolean): PartialFunction[Throwable, Result] = {
    case e: NoActiveSession        =>
      logger.warn(s"[recoverAuthorisedCalls] NoActiveSession: $e")
      Redirect(loginUrl, loginParams(isAnAgent))
    case e: AuthorisationException =>
      logger.error(s"[recoverAuthorisedCalls] Auth exception: $e")
      Redirect(controllers.routes.ApplicationController.unauthorised)
  }

  def authorisedFor(isAnAgent: Boolean, serviceName: String)(body: StandardAuthRetrievals => Future[Result])
                   (implicit req: Request[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, appConfig: AppConfig): Future[Result] = {
    if (!isValidUrl(appConfig, serviceName)) {
      logger.error(s"[authorisedFor] Given invalid service name of $serviceName")
      throw new NotFoundException("Service name not found")
    } else {
      authorised((AffinityGroup.Organisation or AffinityGroup.Agent or Enrolment("IR-SA")) and ConfidenceLevel.L50)
        .retrieve(allEnrolments and affinityGroup and credentials and groupIdentifier) {
          case Enrolments(enrolments) ~ affGroup ~ creds ~ groupId => body(StandardAuthRetrievals(enrolments, affGroup, creds, groupId))
        } recover recoverAuthorisedCalls(appConfig, isAnAgent)
    }
  }
}
