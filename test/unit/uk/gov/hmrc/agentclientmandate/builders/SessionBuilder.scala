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

package unit.uk.gov.hmrc.agentclientmandate.builders

import java.util.UUID

import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson, Headers}
import play.api.test.FakeRequest

object SessionBuilder {

  val TOKEN = "token"

  def updateRequestWithSession(fakeRequest: FakeRequest[AnyContentAsJson], userId: String): FakeRequest[AnyContentAsJson] = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(
      "sessionId" -> sessionId,
      TOKEN -> "RANDOMTOKEN",
      "userId" -> userId
    ).withHeaders(Headers("Authorization" -> "value"))
  }

  def updateRequestFormWithSession(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], userId: String): FakeRequest[AnyContentAsFormUrlEncoded] = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(
      "sessionId" -> sessionId,
      TOKEN -> "RANDOMTOKEN",
      "userId" -> userId
    ).withHeaders(Headers("Authorization" -> "value"))
  }

  def buildRequestWithSession(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId,
      TOKEN -> "RANDOMTOKEN",
      "userId" -> userId
    ).withHeaders(Headers("Authorization" -> "value"))
  }

  def buildRequestWithSessionDelegation(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId,
      TOKEN -> "RANDOMTOKEN",
      "delegationState" -> "On",
      "userId" -> userId
    ).withHeaders(Headers("Authorization" -> "value"))
  }

  def buildRequestWithSessionNoUser = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId,
      TOKEN -> "RANDOMTOKEN"
    ).withHeaders(Headers("Authorization" -> "value"))
  }

}
