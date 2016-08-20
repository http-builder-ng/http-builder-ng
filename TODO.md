- add simple site
    - coverage info
    - javadocs
    - test report
- javadoc 
    https://github.com/dwclark/http-builder-ng/blob/master/src/main/java/groovyx/net/http/ChainedHttpConfig.java
    https://github.com/dwclark/http-builder-ng/blob/master/src/main/java/groovyx/net/http/FromServer.java
    https://github.com/dwclark/http-builder-ng/blob/master/src/main/java/groovyx/net/http/HttpBuilder.java
    https://github.com/dwclark/http-builder-ng/blob/master/src/main/java/groovyx/net/http/HttpConfig.java
    https://github.com/dwclark/http-builder-ng/blob/master/src/main/java/groovyx/net/http/HttpObjectConfig.java
    https://github.com/dwclark/http-builder-ng/blob/master/src/main/java/groovyx/net/http/ToServer.java
- User Guide
    - examples of each verb
    - examples of HTTPS
    1) Examples for using http-builder-ng
    2) Description of architecture and a brief description of the main API classes
    3) Description of how to write encoders and parsers
    4) Description of how to integrate a new http client library
        
- code quality analysis and reports
    - findbugs, pmd, ?
    
- add CI support
    - need permissions to add build hooks

- document
    - how to gen docs
    - how to gen site
    
- roll simple server into a plugin

serve {
    port 8080
    contextPath '/'
    resourceBase '.'
    rootDir './build/jbake'
}
how to stop it