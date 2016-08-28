! need permissions to add hooks for CI
! add site url to GH info (next to description)
- go through my TODO and FIXME tags

- javadoc 
    ChainedHttpConfig
   
- User Guide
    - json/xml/binary body content handling
    - Description of architecture and a brief description of the main API classes
    
- test the encoders
- integration tests with real http services (http://lornajane.net/posts/2013/endpoints-for-http-testing)
       
- Guides/Recipe/Examples
    - GET: get web page as HTML (JSoup) and scrape info out of page 
    - HEAD: use to retrieve last modified date of remote resource
    - POST: body as object with response as typed object
    - REST service example
    - Description of how to write encoders and parsers
    - Description of how to integrate a new http client library
    - configuration for HTTPS interactions (is there anything?)
    - examples with authentication.
    
    REST client on top of HttpBuilder (ala Spring RestTemplate)