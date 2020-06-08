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
    "uk.gov.hmrc"       %% "bootstrap-play-26"             % "1.8.0",
    "uk.gov.hmrc"       %% "auth-client"                   % "3.0.0-play-26",
    "uk.gov.hmrc"       %% "play-partials"                 % "6.11.0-play-26",
    "uk.gov.hmrc"       %% "domain"                        % "5.9.0-play-26",
    "uk.gov.hmrc"       %% "http-caching-client"           % "9.0.0-play-26",
    "uk.gov.hmrc"       %% "emailaddress"                  % "3.4.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.2.0-play-26",
    "uk.gov.hmrc"       %% "play-ui"                       % "8.10.0-play-26",
    "com.typesafe.play" %% "play-json-joda"                % "2.7.4",
    "uk.gov.hmrc"       %% "govuk-template"                % "5.55.0-play-26"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = Nil
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "uk.gov.hmrc"            %% "hmrctest"           % "3.9.0-play-26"     % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3"             % scope,
        "org.pegdown"            %  "pegdown"            % "1.6.0"             % scope,
        "org.jsoup"              %  "jsoup"              % "1.13.1"            % scope,
        "org.scalacheck"         %% "scalacheck"         % "1.14.3"            % scope,
        "org.mockito"            %  "mockito-core"       % "3.3.3"             % scope,
        "com.typesafe.play"      %% "play-test"          % PlayVersion.current % scope,
        "com.github.tomakehurst" %  "wiremock-jre8"      % "2.26.3"            % scope,
        "uk.gov.hmrc"            %% "bootstrap-play-26"  % "1.7.0"             % scope classifier "tests"
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
