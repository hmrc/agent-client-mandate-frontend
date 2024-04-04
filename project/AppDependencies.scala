import sbt.*
import play.sbt.PlayImport.*

private object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30"            % "8.5.0",
    "uk.gov.hmrc"   %% "play-partials-play-30"                 % "9.1.0",
    "uk.gov.hmrc"   %% "domain-play-30"                        % "9.0.0",
    "uk.gov.hmrc"   %% "http-caching-client-play-30"           % "11.2.0",
    "uk.gov.hmrc"   %% "emailaddress-play-30"                  % "4.0.0",
    "uk.gov.hmrc"   %% "play-conditional-form-mapping-play-30" % "2.0.0",
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30"            % "8.5.0",
    "commons-codec" %  "commons-codec"                         % "1.16.1"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = Nil
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
      "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % "8.5.0"    % scope,
      "org.scalatestplus.play"       %% "scalatestplus-play"     % "7.0.1"    % scope,
      "org.jsoup"                    %  "jsoup"                  % "1.17.2"   % scope,
      "org.scalatestplus"            %% "scalacheck-1-17"        % "3.2.18.0" % scope,
      "org.mockito"                  %  "mockito-core"           % "5.11.0"   % scope,
      "org.scalatestplus"            %% "scalatestplus-mockito"  % "1.0.0-M2" % scope,
      "org.wiremock"                 %  "wiremock"               % "3.5.2"    % scope,
      "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.17.0"   % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
