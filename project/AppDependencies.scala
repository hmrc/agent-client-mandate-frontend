import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion._

private object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % "7.21.0",
    "uk.gov.hmrc"       %% "play-partials"                 % "8.4.0-play-28",
    "uk.gov.hmrc"       %% "domain"                        % "8.3.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"           % "10.0.0-play-28",
    "uk.gov.hmrc"       %% "emailaddress"                  % "3.8.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.13.0-play-28",
    "com.typesafe.play" %% "play-json-joda"                % "2.9.4",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "7.19.0-play-28",
    "commons-codec"     %  "commons-codec"                 % "1.16.0"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = Nil
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "org.scalatestplus.play"       %% "scalatestplus-play"     % "5.1.0"    % scope,
        "org.jsoup"                    %  "jsoup"                  % "1.16.1"   % scope,
        "org.scalatestplus"            %% "scalacheck-1-17"        % "3.2.16.0" % scope,
        "org.mockito"                  %  "mockito-core"           % "5.4.0"    % scope,
        "org.scalatestplus"            %% "scalatestplus-mockito"  % "1.0.0-M2" % scope,
        "com.typesafe.play"            %% "play-test"              % current    % scope,
        "com.github.tomakehurst"       %  "wiremock-jre8"          % "2.35.0"   % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.15.2"   % scope,
        "uk.gov.hmrc"                  %% "bootstrap-test-play-28" % "7.21.0"   % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
