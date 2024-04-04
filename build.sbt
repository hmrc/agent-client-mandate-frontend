import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import play.sbt.routes._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName: String = "agent-client-mandate-frontend"

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
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins : _*)
  .settings(playSettings ++ scoverageSettings : _*)
  .settings(majorVersion := 1)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.components.implicits._"
    ),
    scalaVersion := "2.13.12",
    libraryDependencies ++= appDependencies,
    retrieveManaged := true
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest  / parallelExecution := false,
    scalacOptions ++= Seq("-Wconf:src=target/.*:s", "-Wconf:src=routes/.*:s", "-Wconf:cat=unused-imports&src=html/.*:s")
  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))
  .disablePlugins(JUnitXmlReportPlugin)
