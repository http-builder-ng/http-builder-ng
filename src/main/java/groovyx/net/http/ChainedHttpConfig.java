/**
 * Copyright (C) 2016 David Clark
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

import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import static groovyx.net.http.Traverser.*;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap;
import java.nio.charset.Charset;
import groovy.lang.Closure;
import java.util.function.BiConsumer;

public interface ChainedHttpConfig extends HttpConfig {

    interface ChainedRequest extends Request {
        ChainedRequest getParent();
        List<Cookie> getCookies();
        Object getBody();
        String getContentType();
        Map<String,BiConsumer<ChainedHttpConfig,ToServer>> getEncoderMap();
        Charset getCharset();

        default Charset actualCharset() {
            return traverse(this, (cr) -> cr.getParent(), (cr) -> cr.getCharset(), Traverser::notNull);
        }
        
        default String actualContentType() {
            return traverse(this, (cr) -> cr.getParent(), (cr) -> cr.getContentType(), Traverser::notNull);
        }

        default Object actualBody() {
            return traverse(this, (cr) -> cr.getParent(), (cr) -> cr.getBody(), Traverser::notNull);
        }

        default Map<String,String> actualHeaders(final Map<String,String> map) {
            Predicate<Map<String,String>> addValues = (headers) -> { map.putAll(headers); return false; };
            traverse(this, (cr) -> cr.getParent(), (cr) -> cr.getHeaders(), addValues);
            return map;
        }

        default BiConsumer<ChainedHttpConfig,ToServer> actualEncoder(final String contentType) {
            final Function<ChainedRequest, BiConsumer<ChainedHttpConfig,ToServer>> theValue = (cr) -> {
                BiConsumer<ChainedHttpConfig,ToServer> ret = cr.encoder(contentType);
                if(ret != null) {
                    return ret;
                }
                else {
                    return cr.encoder(ContentTypes.ANY.get(0));
                }
            };
            
            return traverse(this, (cr) -> cr.getParent(), theValue, Traverser::notNull);
        }

        default Auth actualAuth() {
            final Predicate<Auth> choose = (a) -> a != null && a.getAuthType() != null;
            return traverse(this, (cr) -> cr.getParent(), (cr) -> cr.getAuth(), choose);
        }

        default List<Cookie> actualCookies(final List<Cookie> list) {
            Predicate<List<Cookie>> addAll = (cookies) -> { list.addAll(cookies); return false; };
            traverse(this, (cr) -> cr.getParent(), (cr) -> cr.getCookies(), addAll);
            return list;
        }
    }

    interface ChainedResponse extends Response {
        ChainedResponse getParent();

        default Closure<?> actualAction(final Integer code) {
            return traverse(this, (cr) -> cr.getParent(), (cr) -> cr.when(code), Traverser::notNull);
        }
        
        default BiFunction<ChainedHttpConfig,FromServer,Object> actualParser(final String contentType) {
            final Function<ChainedResponse, BiFunction<ChainedHttpConfig,FromServer,Object>> theValue = (cr) -> {
                BiFunction<ChainedHttpConfig,FromServer,Object> ret = cr.parser(contentType);
                if(ret != null) {
                    return ret;
                }
                else {
                    return cr.parser(ContentTypes.ANY.get(0));
                }
            };

            return traverse(this, (cr) -> cr.getParent(), theValue, Traverser::notNull);
        }
    }

    Map<Map.Entry<String,Object>,Object> getContextMap();
    
    default Object actualContext(final String contentType, final Object id) {
        final Map.Entry<String,Object> key = new AbstractMap.SimpleImmutableEntry(contentType, id);
        final Map.Entry<String,Object> anyKey = new AbstractMap.SimpleImmutableEntry(ContentTypes.ANY.get(0), id);
        
        final Function<ChainedHttpConfig,Object> theValue = (config) -> {
            Object ctx = config.getContextMap().get(key);
            if(ctx != null) {
                return ctx;
            }
            else {
                return config.getContextMap().get(anyKey);
            }
        };
        
        return traverse(this, (config) -> config.getParent(), theValue, Traverser::notNull);
    }

    ChainedResponse getChainedResponse();
    ChainedRequest getChainedRequest();
    ChainedHttpConfig getParent();

    default BiFunction<ChainedHttpConfig,FromServer,Object> findParser(final String contentType) {
        final BiFunction<ChainedHttpConfig,FromServer,Object> found = getChainedResponse().actualParser(contentType);
        return found == null ? NativeHandlers.Parsers::streamToBytes : found;
    }
    
    default BiConsumer<ChainedHttpConfig,ToServer> findEncoder() {
        final BiConsumer<ChainedHttpConfig,ToServer> encoder = getChainedRequest().actualEncoder(findContentType());
        if(encoder == null) {
            throw new IllegalStateException("Did not find encoder");
        }

        return encoder;
    }

    default String findContentType() {
        final String contentType = getChainedRequest().actualContentType();
        if(contentType == null) {
            throw new IllegalStateException("Found request body, but content type is undefined");
        }

        return contentType;
    }

    static Object[] closureArgs(final Closure<?> closure, final FromServer fromServer, final Object o) {
        final int size = closure.getMaximumNumberOfParameters();
        final Object[] args = new Object[size];
        if(size >= 1) {
            args[0] = fromServer;
        }
        
        if(size >= 2) {
            args[1] = o;
        }
        
        return args;
    }
}
