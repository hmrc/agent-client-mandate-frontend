import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
  import TestPhases.oneForkedJvmPerTest
  import uk.gov.hmrc.SbtAutoBuildPlugin
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
  import uk.gov.hmrc.versioning.SbtGitVersioning

  val appName: String

  lazy val appDependencies: Seq[ModuleID] = ???
  lazy val plugins: Seq[Plugins] = Seq.empty
  lazy val playSettings: Seq[Setting[_]] = Seq.empty

  lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages :=
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
      ScoverageKeys.coverageMinimum := 90,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins: _*)
    .settings(playSettings: _*)
    .settings(majorVersion := 1)
    .configs(IntegrationTest)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(playSettings ++ scoverageSettings: _*)
    .settings(
      addTestReportOption(IntegrationTest, "int-test-reports"),
      inConfig(IntegrationTest)(Defaults.itSettings),
      scalaVersion := "2.11.11",
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesGenerator := InjectedRoutesGenerator,
      Keys.fork                  in Test            := true,
      Keys.fork                  in IntegrationTest :=  false,
      unmanagedSourceDirectories in IntegrationTest :=  (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
      testGrouping               in IntegrationTest :=  oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false
    )
    .settings(
      resolvers += Resolver.bintrayRepo("hmrc", "releases"),
      resolvers += Resolver.jcenterRepo
    )
}
