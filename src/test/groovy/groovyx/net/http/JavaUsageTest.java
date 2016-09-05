package groovyx.net.http;

import groovyx.net.http.fn.FunctionClosure;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

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

        server.when(request().withMethod("GET").withPath("/foo")).respond(response().withBody(CONTENT).withHeader("Content-Type", "text/plain"));
        server.when(request().withMethod("HEAD").withPath("/foo")).respond(response().withHeader("Content-Type", "text/plain"));
    }

    @Test
    public void head_request() throws Exception {
        FunctionClosure<FromServer, Object> successFunction = new FunctionClosure<FromServer, Object>() {
            @Override
            public Object apply(final FromServer from) {
                assertFalse(from.getHasBody());
                return from.getHeaders();
            }
        };

        List<FromServer.Header> headers = (List<FromServer.Header>) http.head(List.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getResponse().success(successFunction);
        });

        assertEquals(3, headers.size());
        assertEquals(headers.get(0).toString(), "Content-Type: text/plain");
        assertEquals(headers.get(1).toString(), "Content-Length: 0");
        assertEquals(headers.get(2).toString(), "Connection: keep-alive");

        CompletableFuture<List> future = http.headAsync(List.class, config -> {
            config.getRequest().getUri().setPath("/foo");
            config.getResponse().success(successFunction);
        });

        headers = (List<FromServer.Header>) future.get();

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

    @Test public void post_request() throws Exception {

    }
}