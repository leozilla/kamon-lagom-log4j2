import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.api.{
  LagomConfigComponent,
  ServiceAcl,
  ServiceInfo
}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.softwaremill.macwire._
import controllers._
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import router.Routes
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, Mode}
import play.filters.HttpFiltersComponents

import scala.collection.immutable
import scala.concurrent.ExecutionContext

abstract class SamplePlayApp(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AssetsComponents
    with HttpFiltersComponents
    with AhcWSComponents
    with LagomConfigComponent
    with LagomServiceClientComponents {

  override lazy val serviceInfo: ServiceInfo = ServiceInfo(
    "gateway",
    Map("gateway" -> immutable.Seq(ServiceAcl.forPathRegex("(?!/api/).*")))
  )
  override implicit lazy val executionContext: ExecutionContext =
    actorSystem.dispatcher

  override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }
}

class SamplePlayAppLoader extends ApplicationLoader {
  override def load(context: Context) = {
    val dev = context.environment.mode

    dev match {
      case Mode.Dev =>
        (new SamplePlayApp(context) with LagomDevModeComponents).application
      case _ =>
        (new SamplePlayApp(context) with AkkaDiscoveryComponents).application
    }
  }
}
