# HttpBuilder-NG: Easy HTTP Client for Groovy (and Java)

[![Bintray - Core](https://api.bintray.com/packages/http-builder-ng/dclark/httpbuilder-ng-core/images/download.svg)](https://bintray.com/http-builder-ng/dclark/httpbuilder-ng-core "Core Library")
[![Bintray - Apache](https://api.bintray.com/packages/http-builder-ng/dclark/httpbuilder-ng-apache/images/download.svg)](https://bintray.com/http-builder-ng/dclark/httpbuilder-ng-apache "Apache Library")
[![Bintray - OkHttp](https://api.bintray.com/packages/http-builder-ng/dclark/httpbuilder-ng-okhttp/images/download.svg)](https://bintray.com/http-builder-ng/dclark/httpbuilder-ng-okhttp "OkHttp Library")

[![Maven Central - Core](https://maven-badges.herokuapp.com/maven-central/io.github.http-builder-ng/http-builder-ng-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.http-builder-ng/http-builder-ng-core) 
[![Maven Central - Apache](https://maven-badges.herokuapp.com/maven-central/io.github.http-builder-ng/http-builder-ng-apache/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.http-builder-ng/http-builder-ng-apache) 
[![Maven Central - OkHttp](https://maven-badges.herokuapp.com/maven-central/io.github.http-builder-ng/http-builder-ng-okhttp/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.http-builder-ng/http-builder-ng-okhttp) 

[![Travis Build Status](https://travis-ci.org/http-builder-ng/http-builder-ng.svg?branch=master)](https://travis-ci.org/http-builder-ng/http-builder-ng)
[![Coverage Status](https://coveralls.io/repos/github/http-builder-ng/http-builder-ng/badge.svg?branch=master)](https://coveralls.io/github/http-builder-ng/http-builder-ng?branch=master)

[![Twitter Follow](https://img.shields.io/twitter/follow/httpbuilderng.svg?style=social&label=Follow)]()

> **Dormant** - The HttpBuilder-NG project is going dormant. Neither of us use the project any longer nor do we have the extra time to properly maintain it. Please feel free to fork it and move it forward, or contact us (with an issue) to discuss options for taking over the project.

## Quick Links for the Impatient

* Site: https://http-builder-ng.github.io/http-builder-ng/
* User Guide: https://http-builder-ng.github.io/http-builder-ng/asciidoc/html5/
* JavaDocs: https://http-builder-ng.github.io/http-builder-ng/docs/javadoc/
* Project: https://github.com/http-builder-ng/http-builder-ng
* Twitter: [@HttpBuilderNG](https://twitter.com/HttpBuilderNG)
* StackOverflow: [httpbuilder-ng](http://stackoverflow.com/questions/tagged/httpbuilder-ng)

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

Hopefully that gives you a general idea of how Http Builder NG works. Http Builder NG is designed to be compatible with Groovy code annotated with [@TypeChecked](http://docs.groovy-lang.org/latest/html/gapi/groovy/transform/TypeChecked.html) and [@CompileStatic](http://docs.groovy-lang.org/latest/html/gapi/groovy/transform/CompileStatic.html). It also makes use of the [@DelegatesTo](http://docs.groovy-lang.org/latest/html/gapi/groovy/lang/DelegatesTo.html) to provide better IDE support when writing code using Http Builder NG.

## Artifacts

Http Builder NG artifacts are available on [Bintray](https://bintray.com/http-builder-ng/dclark/http-builder-ng) and Maven Central, for Gradle you can add the following dependency to your `build.gradle` file `dependencies` closure:

    compile 'io.github.http-builder-ng:http-builder-ng-CLIENT:1.0.4'
    
or, for Maven add the following to your `pom.xml` file:

    <dependency>
      <groupId>io.github.http-builder-ng</groupId>
      <artifactId>http-builder-ng-CLIENT</artifactId>
      <version>1.0.4</version>
    </dependency>
    
where `CLIENT` is replaced with the client library name (`core`, `apache`, or `okhttp`).

## Build Instructions

HttpBuilder-NG is built using [gradle](https://gradle.org). To perform a complete build run the following:

    `./gradlew clean build`

Test reports are not automatically generated; if you need a generated test report (aggregated or per-project) use:

    `./gradlew clean build jacocoTestReport aggregateCoverage`
    
Note that the `aggregateCoverage` task may be dropped if the aggregated report is not desired. The reports will be generated in their respective `build/reports` directories, with the aggregated report being in the `build` directory of the project root.

You can also generate the documentation using one of the following commands:

* For the aggregated JavaDocs: `./gradlew aggregateJavaDoc`
* For the project User Guide: `./gradlew asciidoctor`

Overall project documentation may also be generated as the project web site, using the `site` task, discussed in the next section.

## Documentation

The documentation for the project consists of:

* [Web site](https://http-builder-ng.github.io/http-builder-ng/) - landing page and general introduction (`src/site` directory).
* [User Guide](https://http-builder-ng.github.io/http-builder-ng/asciidoc/html5/) - getting started, examples and detailed usage information (`src/docs/asciidoc` directory).
* [JavaDocs](https://http-builder-ng.github.io/http-builder-ng/docs/javadoc) - unified API documentation (throughout the codebase).
* [Test](https://http-builder-ng.github.io/http-builder-ng/reports/allTests), [Coverage](https://http-builder-ng.github.io/http-builder-ng/reports/jacoco/aggregateCoverage/html) & Quality reports - misc build and quality reports

The documentation is provided by a unified documentation web site, which can be generated using:

    ./gradlew site
    
This task uses the [com.stehno.gradle.site](http://cjstehno.github.io/gradle-site/) plugin to aggregate all the documentation sources and generate the project web site. Once it is built, you can verify the generated content by running a local server:

    ./gradlew startPreview
    
which will start a preview server (see [com.stehno.gradle.webpreview](http://cjstehno.github.io/gradle-webpreview-plugin/)) on a random port copied to your clipboard. 

To stop the preview server run:

    ./gradlew stopPreview`

Once you are ready to publish your site, simply run the following task:

    ./gradlew publishSite
    
This task will push the site contents into the `gh-pages` branch of the project, assuming you have permissions to push content into the repo.

## Artifact Release
    
When ready to release a new version of the project, perform the following steps starting in the `development` branch:

1. Ensure that the project version (in `build.gradle`) has been updated to the desired version.
1. Run `./gradlew updateVersion -Pfrom=OLD_VERSION` to update the documented version.
1. Create a Pull Request from `development` to `master` and accept it or have it reviewed.

Once the pull request has been merged into `master`, checkout the `master` branch and:

1. Run `./gradlew release` which will check the documented project version against the project version, publish the artifact and the documentation web site. 
   * You will need to provide (or have configured in your `HOME/.gradle/gradle.properties` file):
     * `user` - the Bintray username
     * `key` - the Bintray key/password
     * `sonotypeUser` - the Sonotype username (from API key)
     * `sonotypePass` - the Sonotype password (from API key)
   * This step may take some time (on the order of a minute or two depending on server response times).
2. Manually confirm the publication of the new artifact on the Bintray web site (or the publication will expire) - this step may no longer be needed, but verify anyway.
3. Run `./gradlew verifyRelease`  to ensure that the artifacts and site have been published (optional but recommended).
4. A Git tag should be created for the released version.

The `development` branch may now be used for the next round of development work.

> NOTE: Since the artifacts must be confirmed and the site may need some installation time, the `verifyRelease` task cannot be combined with the `release` task.

## Version Updates

When updating the version of the project, the documented version should also be updated using the `updateVersion` task. Modify the version in the project `build.gradle` file and make note of the existing version then run:

    ./gradlew updateVersion -Pfrom=OLD_VERSION
    
where `OLD_VERSION` is the pre-existing version of the project. This will update the current version mentioned in the documentation (e.g. README, User Guide and site).

## History

Http Builder NG was forked from the HTTPBuilder project originally developed by Thomas Nichols. It was later passed on to Jason Gritman who maintained it for several years.

The original intent of Http Builder NG was to fix a few bugs and add a slight enhancement to the original HTTPBuilder project. The slight enhancement was to make HTTPBuilder conform to more modern Groovy DSL designs. However, it was not possible to update the original code to have a more modern typesafe DSL while preserving backwards compatibility. I decided to just make a clean break and give the project a new name to make it clear that Http Builder NG is basically a complete re-write and re-architecture.

# License

```
Copyright 2017 HttpBuilder-Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

# Original Project

[jgritman/httpbuilder](https://github.com/jgritman/httpbuilder)
