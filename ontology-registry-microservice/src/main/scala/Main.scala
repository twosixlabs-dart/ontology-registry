import com.typesafe.config.ConfigFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object Main {

    def main( args : Array[ String ] ) : Unit = {
        val appConfig = ConfigFactory.load().resolve()

        val port = appConfig.getInt( "ontology.registry.port" )
        println( "------------------------------------ ${port}" )
        val server = new Server( port )
        val context = new WebAppContext()

        context.setContextPath( "/" )
        context.setResourceBase( "src/main/webapp" )
        context.setInitParameter( ScalatraListener.LifeCycleKey, "com.twosixlabs.dart.ScalatraInit" ) // scalatra uses some magic defaults I don't like
        context.addEventListener( new ScalatraListener )

        server.setHandler( context )
        server.start()
        server.join()
    }

}
