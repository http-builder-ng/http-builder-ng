@Grab(group='org.eclipse.jetty.aggregate', module='jetty-all', version='7.6.15.v20140411')

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.*
import groovy.servlet.*

def startJetty() {
    def server = new Server(args ? args[0] as int : 8080)

    def handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    handler.contextPath = '/'
    handler.resourceBase = '.'
    handler.addServlet(DefaultServlet, '/').setInitParameter('resourceBase', './build/jbake')

    server.handler = handler
    server.start()
}

println "Starting Jetty, press Ctrl+C to stop."
startJetty()