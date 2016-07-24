package groovyx.net.http.libspecific;

import groovyx.net.http.FromServer;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

public class ApacheFromServer implements FromServer {

    private final HttpResponse response;
    private final HttpEntity entity;
    private final List<Header> headers;
    private final InputStream inputStream;
    
    public ApacheFromServer(final HttpResponse response) {
        this.response = response;
        this.entity = response.getEntity();

        if(entity != null) {
            try {
                this.inputStream = entity.getContent();
            }
            catch(IOException e) {
                throw new RuntimeException("Could not get input stream from apache http client", e);
            }
        }
        else {
            this.inputStream = null;
        }
        
        this.headers = new ArrayList<>(response.getAllHeaders().length);
        for(org.apache.http.Header header : response.getAllHeaders()) {
            headers.add(Header.keyValue(header.getName(), header.getValue()));
        }
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public boolean getHasBody() {
        return entity != null;
    }

    public int getStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    public String getMessage() {
        return response.getStatusLine().getReasonPhrase();
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public void finish() {
        EntityUtils.consumeQuietly(response.getEntity());
    }
}
