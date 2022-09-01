/*
 * Copyright 2020 HM Revenue & Customs
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

package helpers

import helpers.application.IntegrationApplication
import helpers.wiremock.WireMockSetup
import play.api.mvc.{Session, SessionCookieBaker}
import org.scalatest._
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.{WSCookie, DefaultWSCookie, WSRequest}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.crypto.PlainText

trait IntegrationSpec
  extends PlaySpec
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with IntegrationApplication
    with WireMockSetup
    with AssertionHelpers
    with LoginStub {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val BearerToken: String = "mock-bearer-token"
  lazy val cookieCrypto: SessionCookieCrypto = app.injector.instanceOf[SessionCookieCrypto]
  lazy val cookieBaker: SessionCookieBaker = app.injector.instanceOf[SessionCookieBaker]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWmServer()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetWmServer()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopWmServer()
  }

  def hitApplicationEndpoint(url: String): WSRequest = {
    val sessionId = HeaderNames.xSessionId -> SessionId
    val authorisation = HeaderNames.authorisation -> BearerToken
    val csrfToken = "Csrf-Token" -> "nocheck"
    val headers = List(sessionId, authorisation, csrfToken)

    val appendSlash = if(url.startsWith("/")) url else s"/$url"
    ws.url(s"$testAppUrl$appendSlash")
      .withCookies(mockSessionCookie)
      .withFollowRedirects(false)
      .withHttpHeaders(headers:_*)
  }

  def mockSessionCookie: WSCookie = {
    val sessionCookie = cookieBaker.encodeAsCookie(Session(Map(
      SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
      SessionKeys.authToken -> BearerToken,
      SessionKeys.sessionId -> SessionId
    )))

    DefaultWSCookie(
      sessionCookie.name,
      cookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value,
      sessionCookie.domain,
      Some(sessionCookie.path),
      sessionCookie.maxAge.map(_.toLong),
      sessionCookie.secure,
      sessionCookie.httpOnly
    )
  }

}
