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

import java.util.List;
import java.util.Arrays;

public class ContentTypes {
    public static final List<String> ANY = Arrays.asList("*/*");
    public static final List<String> TEXT = Arrays.asList("text/plain");
    public static final List<String> JSON = Arrays.asList("application/json","application/javascript","text/javascript");
    public static final List<String> XML = Arrays.asList("application/xml","text/xml","application/xhtml+xml","application/atom+xml");
    public static final List<String> HTML = Arrays.asList("text/html");
    public static final List<String> URLENC = Arrays.asList("application/x-www-form-urlencoded");
    public static final List<String> BINARY = Arrays.asList("application/octet-stream");
}
