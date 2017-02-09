package groovyx.net.http.util;

import static java.util.concurrent.ThreadLocalRandom.current;
import static java.lang.System.getProperty;

public class Misc {

    private static final char[] theChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    
    public static boolean isPropertySet(final String prop) {
        final String value = getProperty(prop, "false").toLowerCase();
        return (value.equals("true") ||
                value.equals("yes") ||
                value.equals("t") ||
                value.equals("on") ||
                value.equals("1"));
    }

    public static String randomString(final int size) {
        final StringBuilder buffer = new StringBuilder(size);
        for(int i = 0; i < size; ++i) {
            buffer.append(theChars[current().nextInt(theChars.length)]);
        }

        return buffer.toString();
    }
}
