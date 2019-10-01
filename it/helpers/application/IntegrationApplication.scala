
package helpers.application

import helpers.wiremock.WireMockConfig
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}

trait IntegrationApplication extends GuiceOneServerPerSuite with WireMockConfig {
  self: TestSuite =>

  val currentAppBaseUrl: String = "agent-client-mandate-frontend"
  val testAppUrl: String        = s"http://localhost:$port/mandate"

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  val appConfig: Map[String, Any] = Map(
    "application.router"                                  -> "testOnlyDoNotUseInAppConf.Routes",
    "mongo.uri"                                           -> "mongodb://localhost:27017/test-agent-client-mandate-frontend",
    "microservice.metrics.graphite.host"                  -> "localhost",
    "microservice.metrics.graphite.port"                  -> 2003,
    "microservice.metrics.graphite.prefix"                -> "play.agent-client-mandate-frontend.",
    "microservice.metrics.graphite.enabled"               -> true,
    "microservice.services.auth.host"                     -> wireMockHost,
    "microservice.services.auth.port"                     -> wireMockPort,
    "microservice.services.session-cache.host"            -> wireMockHost,
    "microservice.services.session-cache.port"            -> wireMockPort,
    "microservice.services.cachable.session-cache.host"   -> wireMockHost,
    "microservice.services.cachable.session-cache.port"   -> wireMockPort,
    "microservice.services.etmp-hod.host"                 -> wireMockHost,
    "microservice.services.etmp-hod.port"                 -> wireMockPort,
    "microservice.services.auth.company-auth.host"        -> wireMockHost,
    "metrics.name"                                        -> "agent-client-mandate-frontend",
    "metrics.rateUnit"                                    -> "SECONDS",
    "metrics.durationUnit"                                -> "SECONDS",
    "metrics.showSamples"                                 -> true,
    "metrics.jvm"                                         -> true,
    "metrics.enabled"                                     -> true
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(appConfig)
    .build()
}
