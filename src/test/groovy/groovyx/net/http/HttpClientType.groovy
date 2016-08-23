package groovyx.net.http

enum HttpClientType {

    APACHE, JAVA
}

class CountedClosure {

    int count

    boolean getCalled() { count > 0 }

    final Closure<Object> closure = { ->
        count++
    }
}