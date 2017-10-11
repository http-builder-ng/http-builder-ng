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

import java.net.HttpCookie;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static groovyx.net.http.Traverser.traverse;

public interface ChainedHttpConfig extends HttpConfig {

    interface ChainedRequest extends Request {
        ChainedRequest getParent();

        List<HttpCookie> getCookies();

        Object getBody();

        String getContentType();

        Map<String, BiConsumer<ChainedHttpConfig, ToServer>> getEncoderMap();

        Charset getCharset();

        default Charset actualCharset() {
            return traverse(this, ChainedRequest::getParent, ChainedRequest::getCharset, Traverser::notNull);
        }

        default String actualContentType() {
            return traverse(this, ChainedRequest::getParent, ChainedRequest::getContentType, Traverser::notNull);
        }

        default Object actualBody() {
            return traverse(this, ChainedRequest::getParent, ChainedRequest::getBody, Traverser::notNull);
        }

        default Map<String, CharSequence> actualHeaders(final Map<String, CharSequence> map) {
            Predicate<Map<String, CharSequence>> addValues = (headers) -> {
                map.putAll(headers);
                return false;
            };
            traverse(this, ChainedRequest::getParent, Request::getHeaders, addValues);
            return map;
        }

        default BiConsumer<ChainedHttpConfig, ToServer> actualEncoder(final String contentType) {
            final Function<ChainedRequest, BiConsumer<ChainedHttpConfig, ToServer>> theValue = (cr) -> {
                BiConsumer<ChainedHttpConfig, ToServer> ret = cr.encoder(contentType);
                if (ret != null) {
                    return ret;
                } else {
                    return cr.encoder(ContentTypes.ANY.getAt(0));
                }
            };

            return traverse(this, ChainedRequest::getParent, theValue, Traverser::notNull);
        }

        default Auth actualAuth() {
            final Predicate<Auth> choose = (a) -> a != null && a.getAuthType() != null;
            return traverse(this, ChainedRequest::getParent, Request::getAuth, choose);
        }

        default List<HttpCookie> actualCookies(final List<HttpCookie> list) {
            Predicate<List<HttpCookie>> addAll = (cookies) -> {
                list.addAll(cookies);
                return false;
            };
            traverse(this, ChainedRequest::getParent, ChainedRequest::getCookies, addAll);
            return list;
        }

        HttpVerb getVerb();

        void setVerb(HttpVerb verb);
    }

    interface ChainedResponse extends Response {
        ChainedResponse getParent();

        Class<?> getType();

        Function<Throwable,?> getException();

        default BiFunction<FromServer, Object, ?> actualAction(final Integer code) {
            return traverse(this, ChainedResponse::getParent, (cr) -> cr.when(code), Traverser::notNull);
        }

        default Function<Throwable,?> actualException() {
            return traverse(this, ChainedResponse::getParent, ChainedResponse::getException, Traverser::notNull);
        }

        default BiFunction<ChainedHttpConfig, FromServer, Object> actualParser(final String contentType) {
            final Function<ChainedResponse, BiFunction<ChainedHttpConfig, FromServer, Object>> theValue = (cr) -> {
                BiFunction<ChainedHttpConfig, FromServer, Object> ret = cr.parser(contentType);
                if (ret != null) {
                    return ret;
                } else {
                    return cr.parser(ContentTypes.ANY.getAt(0));
                }
            };

            return traverse(this, ChainedResponse::getParent, theValue, Traverser::notNull);
        }
    }

    Map<Map.Entry<String, Object>, Object> getContextMap();

    default Object actualContext(final String contentType, final Object id) {
        final Map.Entry<String, Object> key = new AbstractMap.SimpleImmutableEntry<>(contentType, id);
        final Map.Entry<String, Object> anyKey = new AbstractMap.SimpleImmutableEntry<>(ContentTypes.ANY.getAt(0), id);

        final Function<ChainedHttpConfig, Object> theValue = (config) -> {
            Object ctx = config.getContextMap().get(key);
            if (ctx != null) {
                return ctx;
            } else {
                return config.getContextMap().get(anyKey);
            }
        };

        return traverse(this, ChainedHttpConfig::getParent, theValue, Traverser::notNull);
    }

    ChainedResponse getChainedResponse();

    ChainedRequest getChainedRequest();

    ChainedHttpConfig getParent();

    default BiFunction<ChainedHttpConfig, FromServer, Object> findParser(final String contentType) {
        final BiFunction<ChainedHttpConfig, FromServer, Object> found = getChainedResponse().actualParser(contentType);
        return found == null ? NativeHandlers.Parsers::streamToBytes : found;
    }

    /**
     * Used to find the encoder configured to encode the current resolved content-type.
     *
     * @return the configured encoder
     * @throws IllegalStateException if no coder was found
     */
    default BiConsumer<ChainedHttpConfig, ToServer> findEncoder() {
        return findEncoder(findContentType());
    }

    /**
     * Used to find the encoder configured to encode the specified content-type.
     *
     * @param contentType the content-type to be encoded
     * @return the configured encoder
     * @throws IllegalStateException if no coder was found
     */
    default BiConsumer<ChainedHttpConfig, ToServer> findEncoder(final String contentType) {
        final BiConsumer<ChainedHttpConfig, ToServer> encoder = getChainedRequest().actualEncoder(contentType);
        if (encoder == null) {
            throw new IllegalStateException("Could not find encoder for content-type (" + contentType + ")");
        }

        return encoder;
    }

    default String findContentType() {
        final String contentType = getChainedRequest().actualContentType();
        if (contentType == null) {
            throw new IllegalStateException("Found request body, but content type is undefined");
        }

        return contentType;
    }

    default Charset findCharset(){
        return getChainedRequest().actualCharset();
    }
}
