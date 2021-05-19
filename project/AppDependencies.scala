import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion

private object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-27"    % "5.2.0",
    "uk.gov.hmrc"       %% "play-partials"                 % "8.1.0-play-27",
    "uk.gov.hmrc"       %% "domain"                        % "5.11.0-play-27",
    "uk.gov.hmrc"       %% "http-caching-client"           % "9.5.0-play-27",
    "uk.gov.hmrc"       %% "emailaddress"                  % "3.5.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.9.0-play-27",
    "com.typesafe.play" %% "play-json-joda"                % "2.9.2",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "0.64.0-play-27"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = Nil
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3"             % scope,
        "org.pegdown"            %  "pegdown"            % "1.6.0"             % scope,
        "org.jsoup"              %  "jsoup"              % "1.13.1"            % scope,
        "org.scalacheck"         %% "scalacheck"         % "1.15.4" % scope,
        "org.mockito"            %  "mockito-core"       % "3.10.0" % scope,
        "com.typesafe.play"      %% "play-test"          % PlayVersion.current % scope,
        "com.github.tomakehurst" %  "wiremock-jre8"      % "2.28.0"            % scope,
        "uk.gov.hmrc"            %% "bootstrap-test-play-27"  % "5.2.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
