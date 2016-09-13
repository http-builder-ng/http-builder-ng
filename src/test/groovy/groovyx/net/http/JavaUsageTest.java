package groovyx.net.http;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Demonstration (and test) of using {@link HttpBuilder} via standard Java.
 */
public class JavaUsageTest {

    @Rule
    public MockServerRule serverRule = new MockServerRule(this);

    private static final String CONTENT = "This is CONTENT!!!";
    private MockServerClient server;
    private HttpBuilder http;

    @Before
    public void before() {
        http = HttpBuilder.configure(config -> {
            config.getRequest().setUri(format("http://localhost:%d", serverRule.getPort()));
        });

        HttpResponse contentResponse = response().withBody(CONTENT).withHeader("Content-Type", "text/plain");

        server.when(request().withMethod("GET").withPath("/foo")).respond(contentResponse);
        server.when(request().withMethod("HEAD").withPath("/foo")).respond(response().withHeader("Content-Type", "text/plain"));
        server.when(request().withMethod("POST").withPath("/foo").withBody(CONTENT).withHeader("Content-Type", "text/plain")).respond(contentResponse);
        server.when(request().withMethod("PUT").withPath("/foo").withBody(CONTENT).withHeader("Content-Type", "text/plain")).respond(contentResponse);
        server.when(request().withMethod("DELETE").withPath("/foo")).respond(contentResponse);
    }

    @Test
    @SuppressWarnings({"unchecked", "Convert2Lambda"})
    public void head_request() throws Exception {
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

        assertEquals(3, headers.size());
        assertEquals(headers.get(0).toString(), "Content-Type: text/plain");
        assertEquals(headers.get(1).toString(), "Content-Length: 0");
        assertEquals(headers.get(2).toString(), "Connection: keep-alive");

        CompletableFuture<List> future = http.headAsync(List.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getResponse().success((from, o) -> {
                assertFalse(from.getHasBody());
                return from.getHeaders();
            });
        });

        headers = (List<FromServer.Header>) future.get();

        assertEquals(3, headers.size());
        assertEquals(headers.get(0).toString(), "Content-Type: text/plain");
        assertEquals(headers.get(1).toString(), "Content-Length: 0");
        assertEquals(headers.get(2).toString(), "Connection: keep-alive");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void head_request_with_raw_function() throws Exception {
        BiFunction<FromServer, Object, Object> successFunction = (from, body) -> {
            assertFalse(from.getHasBody());
            return from.getHeaders();
        };

        List<FromServer.Header> headers = (List<FromServer.Header>) http.head(List.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getResponse().success(successFunction);
        });

        assertEquals(3, headers.size());
        assertEquals(headers.get(0).toString(), "Content-Type: text/plain");
        assertEquals(headers.get(1).toString(), "Content-Length: 0");
        assertEquals(headers.get(2).toString(), "Connection: keep-alive");
    }

    @Test
    public void get_request() throws Exception {
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
        String result = http.delete(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
        });

        assertEquals(result, CONTENT);

        CompletableFuture<String> future = http.deleteAsync(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
        });

        assertEquals(future.get(), CONTENT);
    }
}