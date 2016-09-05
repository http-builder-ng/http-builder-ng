/**
 * Copyright (C) 2016 David Clark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.net.http;

import groovy.lang.Closure;
import groovy.time.BaseDuration;
import groovyx.net.http.fn.FunctionClosureAdapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Provides the public interface used for the {@link HttpBuilder} shared and per-verb configuration.
 */
public interface HttpConfig {

    /**
     * Defines the an enumeration of the overall HTTP response status categories.
     */
    enum Status { SUCCESS, FAILURE };

    /**
     * Defines the allowed values of the HTTP authentication type.
     */
    enum AuthType { BASIC, DIGEST };

    /**
     *  Defines the configurable HTTP request authentication properties.
     */
    interface Auth {

        /**
         * Retrieve the authentication type for the request.
         *
         * @return the {@link AuthType} for the Request
         */
        AuthType getAuthType();

        /**
         * Retrieves the configured user for the request.
         *
         * @return the configured user
         */
        String getUser();

        /**
         * Retrieves the configured password for the request.
         *
         * @return the configured password for the request
         */
        String getPassword();

        /**
         * Configures the request to use BASIC authentication with the given `username` and `password`. The authentication will not be preemptive. This
         * method is an alias for calling: `basic(String, String, false)`.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.auth.basic 'admin', '$3cr3t'
         * }
         * ----
         *
         * @param user the username
         * @param password the user's password
         */
        default void basic(String user, String password) {
            basic(user, password, false);
        }

        /**
         * Configures the request to use BASIC authentication with the given `username` and `password`.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.auth.basic 'admin', '$3cr3t', true
         * }
         * ----
         *
         * @param user the username
         * @param password the user's password
         * @param preemptive whether or not this call will override similar configuration in the chain
         */
        void basic(String user, String password, boolean preemptive);

        /**
         * Configures the request to use DIGEST authentication with the given username and password. The authentication will not be preemptive. This
         * method is an alias for calling: `digest(String, String, false)`.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.auth.digest 'admin', '$3cr3t'
         * }
         * ----
         *
         * @param user the username
         * @param password the user's password
         */
        default void digest(String user, String password) {
            digest(user, password, false);
        }

        /**
         * Configures the request to use DIGEST authentication with the given information.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.auth.digest 'admin', '$3cr3t', true
         * }
         * ----
         *
         * @param user the username
         * @param password the user's password
         * @param preemptive whether or not this call will override similar configuration in the chain
         */
        void digest(String user, String password, boolean preemptive);
    }

    /**
     * Defines the configurable HTTP request properties.
     *
     * The `uri` property is the only one that must be defined either in the {@link HttpBuilder} or in the verb configuration.
     */
    interface Request {

        /**
         * Retrieves the authentication information for the request.
         *
         * @return the authentication information for the request.
         */
        Auth getAuth();

        /**
         * The `contentType` property is used to specify the `Content-Type` header value for the request.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.contentType = 'text/json'
         * }
         *
         * http.post {
         *      request.uri.path = '/bar'
         *      request.contentType = 'text/csv'
         * }
         * ----
         *
         * By default, the value will be `text/plain`. The {@link ContentTypes} class provides a helper for some of the more common content type values.
         *
         * @param val the content type value to be used
         */
        void setContentType(String val);

        /**
         * The `charset` property is used to specify the character set (as a String) used by the request.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.charset = 'utf-16'
         * }
         *
         * http.post {
         *      request.uri.path = '/bar'
         *      request.charset = 'utf-8'
         * }
         * ----
         *
         * @param val the content type character set value to be used
         */
        void setCharset(String val);

        /**
         * The `charset` property is used to specify the character set (as a {@link Charset}) used by the request.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.charset = 'utf-16'
         * }
         *
         * http.post {
         *      request.uri.path = '/bar'
         *      request.charset = 'utf-8'
         * }
         * ----
         *
         * @param val the content type character set value to be used
         */
        void setCharset(Charset val);

        /**
         * Retrieves the {@link UriBuilder} for the request, which provides methods for more fine-grained URI specification.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         * }
         *
         * http.get {
         *      request.uri.path = '/foo'
         * }
         * ----
         *
         * @return the {@link UriBuilder} for the request
         */
        UriBuilder getUri();

        /**
         * The `request.uri` is the URI of the HTTP endpoint for the request, specified as a `String` in this case.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         * }
         *
         * http.get {
         *      request.uri.path = '/foo'
         * }
         *
         * http.post {
         *      request.uri.path = '/bar'
         * }
         * ----
         *
         * Which allows multiple verb requests to be configured against the same {@link HttpBuilder}. See the {@link UriBuilder} documentation for
         * more details.
         *
         * The `uri` is the only required configuration property.
         *
         * @param val the URI to be used for the request, as a String
         * @throws IllegalArgumentException if there is a problem with the URI syntax
         */
        void setUri(String val);

        /**
         * The `request.uri` is the URI of the HTTP endpoint for the request, specified as a `URI` in this case.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = new URI('http://localhost:10101')
         * }
         *
         * http.get {
         *      request.uri.path = '/foo'
         * }
         *
         * http.post {
         *      request.uri.path = '/bar'
         * }
         * ----
         *
         * Which allows multiple verb requests to be configured against the same {@link HttpBuilder}. See the {@link UriBuilder} documentation for
         * more details.
         *
         * The `uri` is the only required configuration property.
         *
         * @param val the URI to be used for the request, as a URI
         */
        void setUri(URI val);

        /**
         * The `request.uri` is the URI of the HTTP endpoint for the request, specified as a `URL` in this case.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = new URL('http://localhost:10101')
         * }
         *
         * http.get {
         *      request.uri.path = '/foo'
         * }
         *
         * http.post {
         *      request.uri.path = '/bar'
         * }
         * ----
         *
         * Which allows multiple verb requests to be configured against the same {@link HttpBuilder}. See the {@link UriBuilder} documentation for
         * more details.
         *
         * The `uri` is the only required configuration property.
         *
         * @param val the URI to be used for the request, as a URL
         */
        void setUri(URL val) throws URISyntaxException;

        /**
         * Used to retrieve the request headers.
         *
         * @return the `Map` of request headers
         */
        Map<String, String> getHeaders();

        /**
         * The `headers` property allows the direct specification of the request headers as a `Map<String,String>`. Be aware that `Content-Type` and
         * `Accept` are actually header values and it is up to the implementation to determine which configuration will win out if both are configured.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.headers = [
         *          ClientId: '987sdfsdf9uh'
         *      ]
         * }
         *
         * http.post {
         *      request.uri.path = '/bar'
         *      request.headers = [
         *          AccessCode: '99887766'
         *      ]
         * }
         * ----
         *
         * WARNING: The headers are additive; however, a header specified in the verb configuration may overwrite one defined in the global configuration.
         *
         * @param toAdd the headers to be added to the request headers
         */
        void setHeaders(Map<String, String> toAdd);

        /**
         * The `accept` property allows configuration of the request `Accept` header, which may be used to specify certain media types which are
         * acceptable for the response.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.accept = ['image/jpeg']
         * }
         *
         * http.post {
         *      request.uri.path = '/bar'
         *      request.accept = ['image/tiff', 'image/png']
         * }
         * ----
         *
         * @param values the accept header values as a String array
         */
        void setAccept(String[] values);

        /**
         * The `accept` property allows configuration of the request `Accept` header, which may be used to specify certain media types which are
         * acceptable for the response.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.accept = ['image/jpeg']
         * }
         *
         * http.post {
         *      request.uri.path = '/bar'
         *      request.accept = ['image/tiff', 'image/png']
         * }
         * ----
         *
         * @param values the accept header values as a List
         */
        void setAccept(Iterable<String> values);

        /**
         * The `body` property is used to configure the body content for the request. The request body content may be altered by configured encoders
         * internally or may be passed on unmodified. See {@link HttpConfig} and {@link HttpObjectConfig} for content-altering methods (encoders,
         * decoders and interceptors).
         *
         * @param val the request body content
         */
        void setBody(Object val);

        /**
         * The `cookie` configuration options provide a means of adding HTTP Cookies to the request. Cookies are defined with a `name` and `value`.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.cookie 'seen-before', 'true'
         * }
         * ----
         *
         * WARNING: Cookies are additive, once a Cookie is defined (e.g. in the global configuration), you cannot overwrite it in per-verb configurations.
         *
         * As noted in the {@link groovyx.net.http.HttpObjectConfig.Client} configuration, the default Cookie version supported is `0`, but this may
         * be modified.
         *
         * @param name the cookie name
         * @param value the cookie value
         */
        default void cookie(String name, String value) {
            cookie(name, value, (Date) null);
        }

        /**
         * The `cookie` configuration options provide a means of adding HTTP Cookies to the request. Cookies are defined with a `name`, `value`, and
         * `expires` Date.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         *      request.cookie 'seen-before', 'true'
         * }
         *
         * http.post {
         *      request.uri.path = '/bar'
         *      request.cookie 'last-page', 'item-list', Date.parse('MM/dd/yyyy', '12/31/2016')
         * }
         * ----
         *
         * WARNING: Cookies are additive, once a Cookie is defined (e.g. in the global configuration), you cannot overwrite it in per-verb configurations.
         *
         * As noted in the {@link groovyx.net.http.HttpObjectConfig.Client} configuration, the default Cookie version supported is `0`, but this may
         * be modified.
         *
         * @param name the cookie name
         * @param value the cookie value
         * @param expires the cookie expiration date
         */
        void cookie(String name, String value, Date expires);

        /**
         * The `cookie` configuration options provide a means of adding HTTP Cookies to the request. Cookies are defined with a `name`, `value`, and
         * an expiration date as {@link LocalDateTime}.
         *
         * [source,groovy]
         * ----
         * HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         * }.post {
         *      request.uri.path = '/bar'
         *      request.cookie 'last-page', 'item-list', LocalDateTime.now().plus(1, ChronoUnit.MONTHS)
         * }
         * ----
         *
         * WARNING: Cookies are additive, once a Cookie is defined (e.g. in the global configuration), you cannot overwrite it in per-verb configurations.
         *
         * As noted in the {@link groovyx.net.http.HttpObjectConfig.Client} configuration, the default Cookie version supported is `0`, but this may
         * be modified.
         *
         * @param name the cookie name
         * @param value the cookie value
         * @param expires the cookie expiration date
         */
        void cookie(String name, String value, LocalDateTime expires);

        /**
         * Specifies the request encoder ({@link ToServer} instance) to be used when encoding the given content type.
         *
         * @param contentType the content type
         * @param val the request encoder (wrapped in a {@link BiConsumer} function)
         */
        void encoder(String contentType, BiConsumer<ChainedHttpConfig, ToServer> val);

        /**
         * Specifies the request encoder ({@link ToServer} instance) to be used when encoding the given list of content types.
         *
         * @param contentTypes the content types
         * @param val the request encoder (wrapped in a {@link BiConsumer} function)
         */
        void encoder(Iterable<String> contentTypes, BiConsumer<ChainedHttpConfig, ToServer> val);

        /**
         * Retrieves the request encoder ({@link ToServer} instance) for the specified content type wrapped in a {@link BiConsumer} function.
         *
         * @param contentType the content type of the encoder to be retrieved
         * @return the encoder for the specified content type (wrapped in a {@link BiConsumer} function)
         */
        BiConsumer<ChainedHttpConfig, ToServer> encoder(String contentType);
    }

    /**
     * Defines the configurable HTTP response properties.
     */
    interface Response {

        /**
         * Configures the execution of the provided closure "when" the given status occurs in the response. The `closure` will be called with an instance
         * of the response as a `FromServer` instance. The value returned from the closure will be used as the result value of the request; this allows
         * the closure to modify the captured response.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         * }
         *
         * http.get {
         *      request.uri.path = '/foo'
         *      response.when(Status.SUCCESS){
         *          // executed when a successful response is received
         *      }
         * }
         * ----
         *
         * This method is the same as calling either the `success(Closure)` or `failure(Closure)` methods. Only one closure may be mapped to each
         * status.
         *
         * @param status the response {@link Status} enum
         * @param closure the closure to be executed
         */
        void when(Status status, Closure<?> closure);

        /**
         * FIXME: document
         * @param status
         * @param function
         */
        default void when(Status status, Function<FromServer, Object> function){
            when(status, (Closure<?>) new FunctionClosureAdapter<>(function));
        }

        /**
         * Configures the execution of the provided closure "when" the given status code occurs in the response. The `closure` will be called with an instance
         * of the response as a `FromServer` instance. The value returned from the closure will be used as the result value of the request; this allows
         * the closure to modify the captured response.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         * }
         *
         * http.get {
         *      request.uri.path = '/foo'
         *      response.when(404){
         *          // executed when a 'not found' response is received
         *      }
         * }
         * ----
         *
         * @param code the response code to be caught
         * @param closure the closure to be executed
         */
        void when(Integer code, Closure<?> closure);

        /**
         * FIXME: document
         */
        default void when(Integer code, Function<FromServer,?> function){
            when(code, (Closure<?>)new FunctionClosureAdapter<>(function));
        }

        /**
         * Configures the execution of the provided closure "when" the given status code (as a String) occurs in the response. The `closure` will be
         * called with an instance of the response as a `FromServer` instance. The value returned from the closure will be used as the result value
         * of the request; this allows the closure to modify the captured response.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         * }
         *
         * http.get {
         *      request.uri.path = '/foo'
         *      response.when('404'){
         *          // executed when a 'not found' response is received
         *      }
         * }
         * ----
         *
         * @param code the response code to be caught
         * @param closure the closure to be executed
         */
        void when(String code, Closure<?> closure);

        /**
         * FIXME: document
         */
        default void when(String code, Function<FromServer,?> function){
            when(code, (Closure<?>)new FunctionClosureAdapter<>(function));
        }

        /**
         * Used to retrieve the "when" closure associated with the given status code.
         *
         * @param code the status code
         * @return the mapped closure
         */
        Closure<?> when(Integer code);

        /**
         * Configures the execution of the provided closure "when" a successful response is received (code < 400). The `closure` will be called with
         * an instance of the response as a `FromServer` instance. The value returned from the closure will be used as the result value of the request;
         * this allows the closure to modify the captured response.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         * }
         *
         * http.get {
         *      request.uri.path = '/foo'
         *      response.success(){
         *          // executed when a successful response is received
         *      }
         * }
         * ----
         *
         * This method is the same as calling either the `when(Status.SUCCESS, Closure)` method.
         *
         * @param closure the closure to be executed
         */
        void success(Closure<?> closure);

        /**
         * FIXME: document
         * @param function
         */
        default void success(Function<FromServer,?> function){
            success((Closure<?>) new FunctionClosureAdapter<>(function));
        }

        /**
         * Configures the execution of the provided closure "when" a failure response is received (code >= 400). The `closure` will be called with
         * an instance of the response as a `FromServer` instance. The value returned from the closure will be used as the result value of the request;
         * this allows the closure to modify the captured response.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         * }
         *
         * http.get {
         *      request.uri.path = '/foo'
         *      response.failure(){
         *          // executed when a failure response is received
         *      }
         * }
         * ----
         *
         * This method is the same as calling either the `when(Status.FAILURE, Closure)` method.
         *
         * @param closure the closure to be executed
         */
        void failure(Closure<?> closure);

        /**
         * FIXME: document
         * @param function
         */
        default void failure(Function<FromServer,?> function){
            failure((Closure<?>) new FunctionClosureAdapter<>(function));
        }

        /**
         * Used to specify a response parser ({@link FromServer} instance) for the specified content type, wrapped in a {@link BiFunction}.
         *
         * @param contentType the content type where the parser will be applied
         * @param val the parser wrapped in a function object
         */
        void parser(String contentType, BiFunction<ChainedHttpConfig, FromServer, Object> val);

        /**
         * Used to specify a response parser ({@link FromServer} instance) for the specified content types, wrapped in a {@link BiFunction}.
         *
         * @param contentTypes the contents type where the parser will be applied
         * @param val the parser wrapped in a function object
         */
        void parser(Iterable<String> contentTypes, BiFunction<ChainedHttpConfig, FromServer, Object> val);

        /**
         * Used to retrieve the parser configured for the specified content type.
         *
         * @param contentType the content type
         * @return the mapped parser as a {@link FromServer} instance wrapped in a function object
         */
        BiFunction<ChainedHttpConfig, FromServer, Object> parser(String contentType);
    }

    /**
     * FIXME: document
     */
    void context(String contentType, Object id, Object obj);

    /**
     * FIXME: document
     */
    default void context(final Iterable<String> contentTypes, final Object id, final Object obj) {
        for (String contentType : contentTypes) {
            context(contentType, id, obj);
        }
    }

    /**
     * Used to retrieve configuration information about the HTTP request.
     *
     * @return the HTTP request
     */
    Request getRequest();

    /**
     * Used to retrieve configuration information about the HTTP response.
     *
     * @return the HTTP response
     */
    Response getResponse();
}
