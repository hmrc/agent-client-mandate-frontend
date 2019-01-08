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

package uk.gov.hmrc.agentclientmandate.utils

import uk.gov.hmrc.play.frontend.auth.AuthContext

object AuthUtils {

  def isRegisteredAgent(implicit ac: AuthContext): Boolean = ac.principal.accounts.agent.exists(_.agentBusinessUtr.isDefined)

  def isOrgUser(implicit ac: AuthContext): Boolean = ac.principal.accounts.org.isDefined

  def getAgentLink(implicit ac: AuthContext): String = ac.principal.accounts.agent.map(_.link).getOrElse(throw new RuntimeException("Not an agent"))

  def getOrgLink(implicit ac: AuthContext): String = ac.principal.accounts.org.map(_.link).getOrElse(throw new RuntimeException("Not an Org user"))

  def getAuthLink(implicit ac: AuthContext): String = {
    if(isRegisteredAgent) getAgentLink
    else if(isOrgUser) getOrgLink
    else throw new RuntimeException("invalid user type")
  }

  def getArn(implicit ac: AuthContext): String = ac.principal.accounts.agent.flatMap(_.agentBusinessUtr).
    map(_.utr).getOrElse(throw new RuntimeException("invalid authority"))

}
