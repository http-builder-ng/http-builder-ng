<%include "header.gsp"%>

<%include "menu.gsp"%>


<a href="https://github.com/dwclark/http-builder-ng"><img style="position: absolute; top: 60px !important; right: 0; border: 0;"
                                                   src="https://camo.githubusercontent.com/e7bbb0521b397edbd5fe43e7f760759336b5e05f/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f677265656e5f3030373230302e706e67"
                                                   alt="Fork me on GitHub"
                                                   data-canonical-src="https://s3.amazonaws.com/github/ribbons/forkme_right_green_007200.png"></a>
<h1>Http Builder NG</h1>

<p>Http Builder NG is a modern Groovy DSL for making http requests. It requires Java 8 and a modern Groovy. It is built against Groovy 2.4.x, but it
doesn't make any assumptions about which version of Groovy you are using. The main goal of Http Builder NG is to allow you to make http requests in a
natural and readable way. See the <a href="javadoc">API Docs</a> or <a href="guide/html5" target="_blank">User Guide</a> for more details.</p>

<h2>Artifacts</h2>

<p>Http Builder NG artifacts are available on <a href="https://bintray.com/davidwclark/dclark/http-builder-ng">Bintay</a>, for Gradle you can add the
following dependency to your <code>build.gradle</code> file <code>dependencies</code> closure:</p>

<pre>compile 'org.codehaus.groovy.modules:http-builder-ng:0.9.13'</pre>

<p>For Maven, add the following to your <code>pom.xml</code> file:</p>

<pre>
&lt;dependency&gt;
    &lt;groupId&gt;org.codehaus.groovy.modules&lt;/groupId&gt;
    &lt;artifactId&gt;http-builder-ng&lt;/artifactId&gt;
    &lt;version&gt;0.9.13&lt;/version&gt;
    &lt;type&gt;pom&lt;/type&gt;
&lt;/dependency&gt;
</pre>

<%include "footer.gsp"%>