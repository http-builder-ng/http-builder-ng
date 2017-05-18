import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.status.OnConsoleStatusListener

// always a good idea to add an on console status listener
statusListener(OnConsoleStatusListener)

appender('CONSOLE', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = '%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n'
    }
}

logger 'groovy.net.http.JavaHttpBuilder', INFO
logger 'groovy.net.http.JavaHttpBuilder.content', INFO
logger 'groovy.net.http.JavaHttpBuilder.headers', INFO

root INFO, ['CONSOLE']