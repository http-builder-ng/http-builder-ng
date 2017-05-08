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
