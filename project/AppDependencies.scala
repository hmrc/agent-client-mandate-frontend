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

import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion

private object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-27"    % "3.4.0",
    "uk.gov.hmrc"       %% "auth-client"                   % "3.3.0-play-27",
    "uk.gov.hmrc"       %% "play-partials"                 % "7.1.0-play-27",
    "uk.gov.hmrc"       %% "domain"                        % "5.10.0-play-27",
    "uk.gov.hmrc"       %% "http-caching-client"           % "9.2.0-play-27",
    "uk.gov.hmrc"       %% "emailaddress"                  % "3.5.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.5.0-play-27",
    "uk.gov.hmrc"       %% "play-ui"                       % "8.21.0-play-27",
    "com.typesafe.play" %% "play-json-joda"                % "2.9.2",
    "uk.gov.hmrc"       %% "govuk-template"                % "5.61.0-play-27"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = Nil
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "uk.gov.hmrc"            %% "hmrctest"           % "3.10.0-play-26"     % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3"             % scope,
        "org.pegdown"            %  "pegdown"            % "1.6.0"             % scope,
        "org.jsoup"              %  "jsoup"              % "1.13.1"            % scope,
        "org.scalacheck"         %% "scalacheck"         % "1.15.2" % scope,
        "org.mockito"            %  "mockito-core"       % "3.7.7" % scope,
        "com.typesafe.play"      %% "play-test"          % PlayVersion.current % scope,
        "com.github.tomakehurst" %  "wiremock-jre8"      % "2.27.2"            % scope,
        "uk.gov.hmrc"            %% "bootstrap-test-play-27"  % "3.4.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
