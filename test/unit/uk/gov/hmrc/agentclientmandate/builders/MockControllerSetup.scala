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

package unit.uk.gov.hmrc.agentclientmandate.builders

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.{Application, Environment}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait MockControllerSetup {
  self: MockitoSugar =>

  val app: Application
  val mockAppConfig: AppConfig = mock[AppConfig]

  when(mockAppConfig.companyAuthHost)
    .thenReturn("")
  when(mockAppConfig.loginPath)
    .thenReturn("gg/sign-in")
  when(mockAppConfig.analyticsHost)
    .thenReturn("")
  when(mockAppConfig.analyticsToken)
    .thenReturn("")
  when(mockAppConfig.urBannerToggle)
    .thenReturn(true)
  when(mockAppConfig.getIsoCodeTupleList)
    .thenReturn(List())
  when(mockAppConfig.addNonUkClientCorrespondenceUri(ArgumentMatchers.any(), ArgumentMatchers.any()))
    .thenReturn(
      "http://localhost:9933/ated-subscription/registered-business-address" +
        "?backLinkUrl=" +
        "" +
        "http://localhost:9959/mandate/agent/search-previous/callingPage")
  when(mockAppConfig.nonUkUri(ArgumentMatchers.any(), ArgumentMatchers.any()))
    .thenReturn(
      "http://localhost:9923/business-customer/agent/register/non-uk-client/ated" +
        "?backLinkUrl=" +
        "" +
        "http://localhost:9959/mandate/agent/client-registered-previously/callPage")
  when(mockAppConfig.servicesConfig)
    .thenReturn(app.injector.instanceOf[ServicesConfig])
  when(mockAppConfig.environment)
    .thenReturn(app.injector.instanceOf[Environment])
}
