package groovyx.net.http;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
    public void head_request() throws Exception{
        String result = http.head(String.class, config -> {
            config.getRequest().getUri().setPath("/foo");
//            config.getResponse().success();  FIXME: finish when I get success converted
        });

        assertNull(result);
        assertEquals(result, CONTENT);

//        CompletableFuture<String> future = http.headAsync(String.class, config -> {
//            config.getRequest().getUri().setPath("/foo");
//        });
//
//        assertEquals(future.get(), CONTENT);
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
}
