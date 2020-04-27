import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "agent-client-mandate-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion
  
  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.7.0",
    "uk.gov.hmrc" %% "auth-client" % "2.35.0-play-26",
    "uk.gov.hmrc" %% "play-partials" % "6.10.0-play-26",
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-26",
    "uk.gov.hmrc" %% "http-caching-client" % "9.0.0-play-26",
    "uk.gov.hmrc" %% "emailaddress" % "3.4.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.2.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.9.0-play-26",
    "com.typesafe.play" %% "play-json-joda" % "2.7.4",
    "uk.gov.hmrc" %% "govuk-template" % "5.54.0-play-26"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.12.2" % scope,
        "org.scalacheck" %% "scalacheck" % "1.14.3" % scope,
        "org.mockito" % "mockito-core" % "3.3.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.tomakehurst" % "wiremock-jre8" % "2.26.3" % "test,it",
        "uk.gov.hmrc" %% "bootstrap-play-26" % "1.7.0" % scope classifier "tests"
      )
    }.test
  }

  def apply() = compile ++ Test()
}


