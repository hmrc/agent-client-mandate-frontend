/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{bind => playBind}
import uk.gov.hmrc.agentclientmandate.repositories.SessionCacheRepository

trait TestApplicationBuilder extends GuiceFakeApplicationFactory with MockitoSugar {

  lazy val mockSessionCacheRepo: SessionCacheRepository = mock[SessionCacheRepository]

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .disable[uk.gov.hmrc.mongo.play.PlayMongoModule]
    .configure(
      "metrics.enabled"  -> false,
      "metrics.jvm"      -> false,
      "auditing.enabled" -> false
    )
    .overrides(
      playBind[SessionCacheRepository].toInstance(mockSessionCacheRepo)
    )
    .build()
}
