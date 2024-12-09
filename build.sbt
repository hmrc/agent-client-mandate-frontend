import sbt.Keys.*
import sbt.*
import uk.gov.hmrc.DefaultBuildSettings.*
import play.sbt.routes.*
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName: String = "agent-client-mandate-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.15"

lazy val appDependencies : Seq[ModuleID] = AppDependencies()
lazy val plugins : Seq[Plugins] = Seq.empty
lazy val playSettings : Seq[Setting[_]] = Seq.empty

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages  :=
      "<empty>;" +
      "Reverse.*;" +
      "app.Routes.*;" +
      "internal.Routes.*;" +
      "prod.*;" +
      "testOnlyDoNotUseInAppConf.*;" +
      "uk.gov.hmrc.agentclientmandate.ServiceBindings;" +
      "uk.gov.hmrc.agentclientmandate.config.*;" +
      "uk.gov.hmrc.agentclientmandate.views.*;" +
      "uk.gov.hmrc.BuildInfo*;" +
      "uk.gov.hmrc.agentclientmandate.viewModelsAndForms.*;" +
      "uk.gov.hmrc.agentclientmandate.controllers.testOnly.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins((Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins) *)
  .settings((playSettings ++ scoverageSettings) *)
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
    scalacOptions ++= Seq("-Wconf:src=target/.*:s", "-Wconf:cat=unused-imports&src=routes/.*:s", "-Wconf:cat=unused-imports&src=html/.*:s")
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.components.implicits._"
    ),
    libraryDependencies ++= appDependencies,
    retrieveManaged := true
  )

  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))
  .disablePlugins(JUnitXmlReportPlugin)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.itDependencies)
