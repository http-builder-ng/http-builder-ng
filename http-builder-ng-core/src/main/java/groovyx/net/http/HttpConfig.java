/**
 * Copyright (C) 2017 HttpBuilder-NG Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.net.http;

import groovy.lang.Closure;
import groovyx.net.http.fn.ClosureBiFunction;
import groovyx.net.http.fn.ClosureFunction;

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
    enum Status {
        SUCCESS, FAILURE
    }

    /**
     * Defines the allowed values of the HTTP authentication type.
     */
    enum AuthType {
        BASIC, DIGEST
    }

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
         * The `charset` property is used to specify the character set (as a {@link Charset}) used by the request. This value will be reflected in
         * the `Content-Type` header value (e.g. `Content-Type: text/plain; charset=utf-8`). A content-type value must be specified in order for this
         * value to be applied.
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
         * The `request.raw` is the means of specifying a "raw" URI as the HTTP endpoint for the request, specified as a `String`. No encoding or decoding is performed on a "raw" URI. Any such
         * encoding or decoding of URI content must be done in the provided string itself, as it will be used "as is" in the resulting URI. This functionality is useful in the case where
         * there are encoded entities in the URI path, since the standard `uri` method will decode these on building the `URI`.
         *
         * @param val the raw URI string
         */
        void setRaw(String val);

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
        Map<String, CharSequence> getHeaders();

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
        void setHeaders(Map<String, CharSequence> toAdd);

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
         * of the response as a `FromServer` instance and the response body as an `Object` (if there is one). The value returned from the closure will be
         * used as the result value of the request; this allows the closure to modify the captured response.
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
        default void when(Status status, Closure<?> closure) {
            when(status, new ClosureBiFunction<>(closure));
        }

        /**
         * Configures the execution of the provided function "when" the given status occurs in the response. The `function` will be called with an instance
         * of the response as a `FromServer` instance and the response body as an `Object` (if there is one). The value returned from the closure will be
         * used as the result value of the request; this allows the closure to modify the captured response.
         *
         * This method is generally used for Java-based configuration.
         *
         * [source,java]
         * ----
         * HttpBuilder http = HttpBuilder.configure(config -> {
         *     config.getRequest().setUri("http://localhost:10101");
         * });
         * http.get( config -> {
         *     config.getRequest().getUri().setPath("/foo");
         *     config.getResponse().when(Status.SUCCESS, new BiFunction<FromServer, Object, Object>() {
         *          // executed when a successful response is received
         *     });
         * });
         * ----
         *
         * This method is the same as calling either the `success(BiFunction)` or `failure(BiFunction)` methods. Only one function may be mapped to each
         * status.
         *
         * @param status the response {@link Status} enum
         * @param function the function to be executed
         */
        void when(Status status, BiFunction<FromServer, Object, ?> function);

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
        default void when(Integer code, Closure<?> closure) {
            when(code, new ClosureBiFunction<>(closure));
        }

        /**
         * Configures the execution of the provided function "when" the given status code occurs in the response. The `function` will be called with an instance
         * of the response as a `FromServer` instance and the response body as an `Object` (if there is one). The value returned from the closure will be
         * used as the result value of the request; this allows the closure to modify the captured response.
         *
         * This method is generally used for Java-based configuration.
         *
         * [source,java]
         * ----
         * HttpBuilder http = HttpBuilder.configure(config -> {
         *     config.getRequest().setUri("http://localhost:10101");
         * });
         * http.get( config -> {
         *     config.getRequest().getUri().setPath("/foo");
         *     config.getResponse().when(404, new BiFunction<FromServer, Object, Object>() {
         *          // executed when a successful response is received
         *     });
         * });
         * ----
         *
         * This method is the same as calling either the `success(BiFunction)` or `failure(BiFunction)` methods. Only one function may be mapped to each
         * status.
         *
         * @param code the response code
         * @param function the function to be executed
         */
        void when(Integer code, BiFunction<FromServer, Object, ?> function);

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
        default void when(String code, Closure<?> closure) {
            when(code, new ClosureBiFunction<>(closure));
        }

        /**
         * Configures the execution of the provided function "when" the given status code (as a `String`) occurs in the response. The `function` will be
         * called with an instance of the response as a `FromServer` instance and the response body as an `Object` (if there is one). The value returned
         * from the function will be used as the result value of the request; this allows the function to modify the captured response.
         *
         * This method is generally used for Java-based configuration.
         *
         * [source,java]
         * ----
         * HttpBuilder http = HttpBuilder.configure(config -> {
         *     config.getRequest().setUri("http://localhost:10101");
         * });
         * http.get( config -> {
         *     config.getRequest().getUri().setPath("/foo");
         *     config.getResponse().when("404", new BiFunction<FromServer, Object, Object>() {
         *          // executed when a successful response is received
         *     });
         * });
         * ----
         *
         * @param code the response code as a `String`
         * @param function the function to be executed
         */
        void when(String code, BiFunction<FromServer, Object, ?> function);

        /**
         * Used to retrieve the "when" function associated with the given status code.
         *
         * @param code the status code
         * @return the mapped closure
         */
        BiFunction<FromServer, Object, ?> when(Integer code);

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
        default void success(Closure<?> closure) {
            success(new ClosureBiFunction<>(closure));
        }

        /**
         * Configures the execution of the provided function when a success response is received (code < 400). The `function` will be called with
         * an instance of the response as a `FromServer` instance and the body content as an `Object` (if present). The value returned from the function
         * will be used as the result value of the request; this allows the function to modify the captured response.
         *
         * This method is generally used for Java-specific configuration.
         *
         * [source,java]
         * ----
         * HttpBuilder http = HttpBuilder.configure(config -> {
         *     config.getRequest().setUri("http://localhost:10101");
         * });
         * http.get( config -> {
         *     config.getRequest().getUri().setPath("/foo");
         *     config.getResponse().success(new BiFunction<FromServer, Object, Object>() {
         *          // executed when a success response is received
         *     });
         * });
         * ----
         *
         * This method is the same as calling either the `when(Status.SUCCESS, BiFunction)` method.
         *
         * @param function the closure to be executed
         */
        void success(BiFunction<FromServer, Object, ?> function);

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
         *      response.failure {
         *          // executed when a failure response is received
         *      }
         * }
         * ----
         *
         * This method is the same as calling either the `when(Status.FAILURE, Closure)` method.
         *
         * @param closure the closure to be executed
         */
        default void failure(Closure<?> closure) {
            failure(new ClosureBiFunction<>(closure));
        }

        /**
         * Configures the execution of the provided function "when" a failure response is received (code >= 400). The `function` will be called with
         * an instance of the response as a `FromServer` instance and the body content as an `Object` (if present). The value returned from the function
         * will be used as the result value of the request; this allows the function to modify the captured response.
         *
         * This method is generally used for Java-specific configuration.
         *
         * [source,java]
         * ----
         * HttpBuilder http = HttpBuilder.configure(config -> {
         *     config.getRequest().setUri("http://localhost:10101");
         * });
         * http.get( config -> {
         *     config.getRequest().getUri().setPath("/foo");
         *     config.getResponse().failure(new BiFunction<FromServer, Object, Object>() {
         *          // executed when a failure response is received
         *     });
         * });
         * ----
         *
         * This method is the same as calling either the `when(Status.FAILURE, BiFunction)` method.
         *
         * @param function the closure to be executed
         */
        void failure(BiFunction<FromServer, Object, ?> function);

        /**
         * Configures the execution of the provided closure to handle exceptions during request/response processing. This is
         * different from a failure condition because there is no response, no status code, no headers, etc. The `closure` will be called with
         * the best guess as to what was the original exception. Some attempts will be made to unwrap exceptions that are of type
         * {@link groovyx.net.http.TransportingException} or {@link java.lang.reflect.UndeclaredThrowableException}. The `closure`
         * should have a single {@link java.lang.Throwable} argument.
         *
         * The value returned from the closure will be used as the result value of the request. Since there is no response
         * body for the closure to process, this usually means that the closure should do one of three things: re-throw the exception or 
         * throw a wrapped version of the exception, return null, or return a predefined empty value.
         *
         * [source,groovy]
         * ----
         * def http = HttpBuilder.configure {
         *      request.uri = 'http://localhost:10101'
         * }
         *
         * http.get {
         *      request.uri.path = '/foo'
         *      response.exception { Throwable t ->
         *          t.printStackTrace();
         *          throw new RuntimeException(t);
         *      }
         * }
         * ----
         *
         * The default exception method wraps the exception in a {@link java.lang.RuntimeException} (if it is
         * not already of that type) and rethrows.
         *
         * @param closure the closure to be executed
         */
        default void exception(Closure<?> closure) {
            exception(new ClosureFunction<>(closure));
        }

        /**
         * Configures the execution of the provided `function` to handle exceptions during request/response processing. This is
         * different from a failure condition because there is no response, no status code, no headers, etc. The `function` will be called with
         * the best guess as to what was the original exception. Some attempts will be made to unwrap exceptions that are of type
         * {@link groovyx.net.http.TransportingException} or {@link java.lang.reflect.UndeclaredThrowableException}.
         *
         * The value returned from the function will be used as the result value of the request. Since there is no response
         * body for the function to process, this usually means that the function should do one of three things: re-throw the exception or 
         * throw a wrapped version of the exception, return null, or return a predefined empty value.

         * This method is generally used for Java-specific configuration.
         *
         * [source,java]
         * ----
         * HttpBuilder http = HttpBuilder.configure(config -> {
         *     config.getRequest().setUri("http://localhost:10101");
         * });
         * http.get( config -> {
         *     config.getRequest().getUri().setPath("/foo");
         *     config.getResponse().exception((t) -> {
         *          t.printStackTrace();
         *          throw new RuntimeException(t);
         *     });
         * });
         * ----
         *
         * The built in exception method wraps the exception in a {@link java.lang.RuntimeException} (if it is
         * not already of that type) and rethrows.
         *
         * @param function the function to be executed
         */
        void exception(Function<Throwable,?> function);
        
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
     * Registers a context-level content-type specific object.
     *
     * @param contentType the content type scope of the mapping
     * @param id the mapping key
     * @param obj the mapping value
     */
    void context(String contentType, Object id, Object obj);

    /**
     * Used to register a content-type-scoped object on the context in the specified content-type scopes.
     *
     * @param contentTypes the content-types where the mapping is scoped
     * @param id the mapping key
     * @param obj the mapping value
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
