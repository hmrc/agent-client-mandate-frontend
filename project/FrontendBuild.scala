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
    "uk.gov.hmrc" %% "frontend-bootstrap" % "12.9.0",
    "uk.gov.hmrc" %% "auth-client" % "2.27.0-play-25",
    "uk.gov.hmrc" %% "play-partials" % "6.9.0-play-25",
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-25",
    "uk.gov.hmrc" %% "http-caching-client" % "8.5.0-play-25",
    "uk.gov.hmrc" %% "emailaddress" % "3.2.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.1.0-play-25"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.9.2" % scope,
        "org.scalacheck" %% "scalacheck" % "1.14.0" % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.tomakehurst" % "wiremock" % "2.6.0" % "it"
      )
    }.test
  }

  def apply() = compile ++ Test()
}


