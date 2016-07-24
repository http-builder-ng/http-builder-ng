package groovyx.net.http;

import java.util.Date;

public class Cookie {
    private final String name;
    private final String value;
    private final Date expires;

    public Cookie(final String name, final String value) {
        this(name, value, null);
    }
    
    public Cookie(final String name, final String value, final Date expires) {
        this.name = name;
        this.value = value;
        this.expires = expires;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Date getExpires() {
        return expires;
    }
}
