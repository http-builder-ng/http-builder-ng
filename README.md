# Http Builder NG, The Easy Http Client for Groovy (and Java)

[![Bintray](https://api.bintray.com/packages/http-builder-ng/dclark/http-builder-ng/images/download.svg)](https://bintray.com/http-builder-ng/dclark/http-builder-ng)
[![Travis Build Status](http://img.shields.io/travis/http-builder-ng/http-builder-ng.svg)](https://travis-ci.org/http-builder-ng/http-builder-ng)
[![Coverage Status](https://coveralls.io/repos/github/http-builder-ng/http-builder-ng/badge.svg?branch=master)](https://coveralls.io/github/http-builder-ng/http-builder-ng?branch=master)

## Quick Links for the Impatient

* Site: https://http-builder-ng.github.io/http-builder-ng/
* Project: https://github.com/http-builder-ng/http-builder-ng
* JavaDocs (core): https://http-builder-ng.github.io/http-builder-ng/core/javadoc/
* JavaDocs (apache): https://http-builder-ng.github.io/http-builder-ng/apache/javadoc/
* User Guide: https://http-builder-ng.github.io/http-builder-ng/guide/html5/

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

Http Builder NG artifacts are available on [Bintray](https://bintray.com/http-builder-ng/dclark/http-builder-ng), for Gradle you can add the following dependency to your `build.gradle` file `dependencies` closure:

    compile 'org.codehaus.groovy.modules:http-builder-ng-core:0.11.0'
    
For Maven, add the following to your `pom.xml` file:

    <dependency>
      <groupId>org.codehaus.groovy.modules</groupId>
      <artifactId>http-builder-ng-core</artifactId>
      <version>0.11.0</version>
      <type>pom</type>
    </dependency>

The coordinates shown are for the "core" library, if you want to use the Apache client implementation, replace "core" with "apache" in the artifact coordinates shown above.

## Build Instructions

Http Builder NG is built using [gradle](https://gradle.org). To perform a complete build and install it locally use the following incantation:

`$ ./gradlew clean build install`

You can also generate the documentation using one of the following commands:

    ./gradlew javadoc
    ./gradlew asciidoctor
    ./gradlew site

which will generate the API Documentation, User Guide and Documentation web site respectively.

## Documentation Site

The project provides a unified documentation web site. You can build the documentation site with:

    ./gradlew site

Once it is built, you can verify the generated content by running a local server:

    ./gradlew startPreview
    
which will start a preview server on a random port copied to your clipboard. Run `./gradlew stopPreview` to stop the server. Once you are ready to 
publish your site, simply run the following task:

    ./gradlew publishSite
    
This task will push the site contents into the `gh-pages` branch of the project, assuming you have permissions to push content into the repo.

## Artifact Release
    
When ready to release a new version of the project, perform the following steps starting in the `development` branch:

1. Ensure that the project version (in `build.gradle`) has been updated to the desired version.
1. Run `./gradlew updateVersion -Pfrom=OLD_VERSION` to update the documented version.
1. Create a Pull Request from `development` to `master` and accept it or have it reviewed.

Once the pull request has been merged into `master`, checkout the `master` branch and:

1. Run `./gradlew release` which will check the documented project version against the project version, publish the artifact and the documentation web site.
1. Confirm the publication of the new artifact on the Bintray web site. 
1. Run `./gradlew verifyRelease`  to ensure that the artifacts and site have been published (optional but recommended).
1. A Git tag should be created for the released version.

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
