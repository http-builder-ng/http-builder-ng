# Http Builder NG, The Easy Http Client for Groovy

## Quick Links for the Impatient

* Site: https://dwclark.github.io/http-builder-ng/
* Project: https://github.com/dwclark/http-builder-ng
* JavaDocs: https://dwclark.github.io/http-builder-ng/javadoc/
* User Guide: https://dwclark.github.io/http-builder-ng/guide/html5/

## Quick Overview

Http Builder NG is a modern Groovy DSL for making http requests. It requires Java 8 and a modern Groovy. It is built against Groovy 2.4.x, but it doesn't make any assumptions about which version of Groovy you are using. The main goal of Http Builder NG is to allow you to make http requests in a natural and readable way. For example:

```groovy
//let's configure an http client to make calls to httpbin.org using the default http library
def httpBin = HttpBuilder.configure {
    request.uri = 'http://httpbin.org/'
}

//now let's GET /get endpoint at httpbin.
//This will return a JSON formatted response with an origin property.
def result = httpBin.get {
    request.uri.path = '/get'
}
    
println("Your ip address is: ${result.origin}")

//Finally lets post a standard http form to httpbin
httpBin.post {
    request.uri.path = '/post'
    request.body = [ input1: 'the first input', input2: 'the second input' ]
    request.contentType = 'application/x-www-form-urlencoded'
}

```

Hopefully that gives you a flavor or how Http Builder NG works. Http Builder NG is designed to be compatible with Groovy code annotated with [@TypeChecked](http://docs.groovy-lang.org/latest/html/gapi/groovy/transform/TypeChecked.html) and [@CompileStatic](http://docs.groovy-lang.org/latest/html/gapi/groovy/transform/CompileStatic.html). It also makes use of the [@DelegatesTo](http://docs.groovy-lang.org/latest/html/gapi/groovy/lang/DelegatesTo.html) to provide better IDE support when writing code using Http Builder NG.

## Artifacts

Http Builder NG artifacts are available on [Bintay](https://bintray.com/davidwclark/dclark/http-builder-ng), for Gradle you can add the following dependency to your `build.gradle` file `dependencies` closure:

    compile 'org.codehaus.groovy.modules:http-builder-ng:0.9.13'
    
For Maven, add the following to your `pom.xml` file:

    <dependency>
      <groupId>org.codehaus.groovy.modules</groupId>
      <artifactId>http-builder-ng</artifactId>
      <version>0.9.13</version>
      <type>pom</type>
    </dependency>

## Build Instructions

Http Builder NG is built using [gradle](https://gradle.org). To perform a complete build and install it locally use the following incantation:

`$ ./gradlew clean build install`

You can also generate the documentation using one of the following commands:

    ./gradlew javadoc
    ./gradlew asciidoctor
    ./gradlew site

which will generate the API Documentation, User Guide and Documentation web site respectively.

## History

Http Builder NG was forked from the HTTPBuilder project originally developed by Thomas Nichols. It was later passed on to Jason Gritman who maintained it for several years.

The original intent of Http Builder NG was to fix a few bugs and add a slight enhancement to the original HTTPBuilder project. The slight enhancement was to make HTTPBuilder conform to more modern Groovy DSL designs. However, it was not possible to update the original code to have a more modern typesafe DSL while preserving backwards compatibility. I decided to just make a clean break and give the project a new name to make it clear that Http Builder NG is basically a complete re-write and re-architecture.

# License

Copyright 2016 David Clark

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

# Original Project

[jgritman/httpbuilder](https://github.com/jgritman/httpbuilder)
