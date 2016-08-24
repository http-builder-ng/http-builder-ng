! coverage images are missing
! need permissions to add hooks for CI

- javadoc 
    https://github.com/dwclark/http-builder-ng/blob/master/src/main/java/groovyx/net/http/ChainedHttpConfig.java
    https://github.com/dwclark/http-builder-ng/blob/master/src/main/java/groovyx/net/http/FromServer.java
    https://github.com/dwclark/http-builder-ng/blob/master/src/main/java/groovyx/net/http/ToServer.java
    
- User Guide
    - GET
    - HEAD
    - POST
    - PUT
    - DELETE
    - OPTIONS
    - TRACE
    - examples of HTTPS
    - examples with authentication
    - json/xml/binary body content handling
    1) Examples for using http-builder-ng
    2) Description of architecture and a brief description of the main API classes
    3) Description of how to write encoders and parsers
    4) Description of how to integrate a new http client library
        
- code quality analysis and reports
    - findbugs, pmd, other static analysis
    - https://docs.gradle.org/current/userguide/findbugs_plugin.html
    
- can I split the user guide into multiple pages and still maintain the TOC?

- consider: http://asciidoctor.org/news/2013/06/03/asciidoclet-announcement/
    - https://docs.gradle.org/current/dsl/org.gradle.api.tasks.javadoc.Javadoc.html
- maybe move more of the per-property details into the javadocs and have guide more usage and interesting stuff