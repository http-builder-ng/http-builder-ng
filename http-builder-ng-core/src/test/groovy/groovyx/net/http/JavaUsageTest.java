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

import com.stehno.ersatz.Decoders;
import com.stehno.ersatz.ErsatzServer;
import com.stehno.ersatz.RequestDecoders;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static com.stehno.ersatz.ContentType.TEXT_PLAIN;
import static org.junit.Assert.*;

/**
 * Demonstration (and test) of using {@link HttpBuilder} via standard Java.
 */
public class JavaUsageTest {

    private static final String CONTENT = "This is CONTENT!!!";
    private ErsatzServer ersatzServer;
    private RequestDecoders decoders;
    private HttpBuilder http;

    @Before
    public void before() {
        ersatzServer = new ErsatzServer();

        decoders = new RequestDecoders();
        decoders.register(TEXT_PLAIN, Decoders.getUtf8String());
    }

    @Test
    @SuppressWarnings({"unchecked", "Convert2Lambda"})
    public void head_request() throws Exception {
        ersatzServer.expectations(ex -> ex.head("/foo").responds().contentType("text/plain")).start();

        http = HttpBuilder.configure(config -> {
            config.getRequest().setUri(ersatzServer.getHttpUrl());
        });

        List<FromServer.Header> headers = (List<FromServer.Header>) http.head(List.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getResponse().success(new BiFunction<FromServer, Object, Object>() {
                @Override
                public Object apply(final FromServer from, final Object o) {
                    assertFalse(from.getHasBody());
                    return from.getHeaders();
                }
            });
        });

        assertEquals(4, headers.size());
        assertEquals(headers.get(0).toString(), "Connection: keep-alive");
        assertEquals(headers.get(1).toString(), "Content-Type: text/plain");
        assertEquals(headers.get(2).toString(), "Content-Length: 0");
        assertTrue(headers.get(3).toString().startsWith("Date: "));

        CompletableFuture<List> future = http.headAsync(List.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getResponse().success((from, o) -> {
                assertFalse(from.getHasBody());
                return from.getHeaders();
            });
        });

        headers = (List<FromServer.Header>) future.get();

        assertEquals(4, headers.size());
        assertEquals(headers.get(0).toString(), "Connection: keep-alive");
        assertEquals(headers.get(1).toString(), "Content-Type: text/plain");
        assertEquals(headers.get(2).toString(), "Content-Length: 0");
        assertTrue(headers.get(3).toString().startsWith("Date: "));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void head_request_with_raw_function() throws Exception {
        ersatzServer.expectations(ex -> ex.head("/foo").responds().contentType("text/plain")).start();

        http = HttpBuilder.configure(config -> {
            config.getRequest().setUri(ersatzServer.getHttpUrl());
        });

        BiFunction<FromServer, Object, Object> successFunction = (from, body) -> {
            assertFalse(from.getHasBody());
            return from.getHeaders();
        };

        List<FromServer.Header> headers = (List<FromServer.Header>) http.head(List.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getResponse().success(successFunction);
        });

        assertEquals(4, headers.size());
        assertEquals(headers.get(0).toString(), "Connection: keep-alive");
        assertEquals(headers.get(1).toString(), "Content-Type: text/plain");
        assertEquals(headers.get(2).toString(), "Content-Length: 0");
        assertTrue(headers.get(3).toString().startsWith("Date: "));
    }

    @Test
    public void get_request() throws Exception {
        ersatzServer.expectations(ex -> ex.get("/foo").responds().content(CONTENT, "text/plain")).start();

        http = HttpBuilder.configure(config -> {
            config.getRequest().setUri(ersatzServer.getHttpUrl());
        });

        String result = http.get(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
        });

        assertEquals(result, CONTENT);

        CompletableFuture<String> future = http.getAsync(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
        });

        assertEquals(future.get(), CONTENT);
    }

    @Test
    public void post_request() throws Exception {
        ersatzServer.expectations(ex -> ex.post("/foo").decoders(decoders).body(CONTENT, TEXT_PLAIN).responds().content(CONTENT, TEXT_PLAIN)).start();

        http = HttpBuilder.configure(config -> {
            config.getRequest().setUri(ersatzServer.getHttpUrl());
        });

        String result = http.post(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getRequest().setBody(CONTENT);
            config.getRequest().setContentType("text/plain");
        });

        assertEquals(result, CONTENT);

        CompletableFuture<String> future = http.postAsync(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getRequest().setBody(CONTENT);
            config.getRequest().setContentType("text/plain");
        });

        assertEquals(future.get(), CONTENT);
    }

    @Test
    public void put_request() throws Exception {
        ersatzServer.expectations(ex -> ex.put("/foo").body(CONTENT, TEXT_PLAIN).decoders(decoders).responds().content(CONTENT, TEXT_PLAIN)).start();

        http = HttpBuilder.configure(config -> {
            config.getRequest().setUri(ersatzServer.getHttpUrl());
        });

        String result = http.put(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getRequest().setBody(CONTENT);
            config.getRequest().setContentType("text/plain");
        });

        assertEquals(result, CONTENT);

        CompletableFuture<String> future = http.putAsync(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getRequest().setBody(CONTENT);
            config.getRequest().setContentType("text/plain");
        });

        assertEquals(future.get(), CONTENT);
    }

    @Test
    public void delete_request() throws Exception {
        ersatzServer.expectations(ex -> ex.delete("/foo").responds().content(CONTENT, "text/plain")).start();

        http = HttpBuilder.configure(config -> {
            config.getRequest().setUri(ersatzServer.getHttpUrl());
        });

        String result = http.delete(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
        });

        assertEquals(result, CONTENT);

        CompletableFuture<String> future = http.deleteAsync(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
        });

        assertEquals(future.get(), CONTENT);
    }

    @After
    public void after() {
        ersatzServer.stop();
    }
}