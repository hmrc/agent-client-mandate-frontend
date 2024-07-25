import sbt.*
import play.sbt.PlayImport.*

private object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30"            % "9.1.0",
    "uk.gov.hmrc"   %% "play-partials-play-30"                 % "10.0.0",
    "uk.gov.hmrc"   %% "domain-play-30"                        % "10.0.0",
    "uk.gov.hmrc"   %% "http-caching-client-play-30"           % "12.1.0",
    "uk.gov.hmrc"   %% "emailaddress-play-30"                  % "4.0.0",
    "uk.gov.hmrc"   %% "play-conditional-form-mapping-play-30" % "2.0.0",
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30"            % "10.1.0",
    "commons-codec" %  "commons-codec"                         % "1.17.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % "9.1.0",
    "org.scalatestplus.play"       %% "scalatestplus-play"     % "7.0.1",
    "org.scalatestplus"            %% "scalacheck-1-17"        % "3.2.18.0"
  ).map(_ % Test)


  val itDependencies: Seq[ModuleID] = Seq()

  def apply(): Seq[ModuleID] = compile ++ test
}
