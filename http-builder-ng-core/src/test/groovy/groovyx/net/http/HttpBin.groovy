/*
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
package groovyx.net.http

import groovy.transform.InheritConstructors
import groovy.transform.TypeChecked

/**
 * Used as the argument of a Spock @Requires annotation to denote that the annotated test(s) require the presence of the
 * httpbin.org web site for testing. If this site is not available, the annotated test(s) will be skipped.
 *
 * While this checks for the existence of a specific site, it can be used as a general test for internet access.
 */
@InheritConstructors @TypeChecked
class HttpBin extends Closure<Boolean> {

    private static final URL HTTPBIN = 'http://httpbin.org/'.toURL()

    Boolean doCall() {
        try {
            HttpURLConnection conn = HTTPBIN.openConnection() as HttpURLConnection
            conn.setRequestMethod('HEAD')
            conn.setUseCaches(true)

            boolean status = conn.responseCode < 400
            conn.disconnect()

            status
        } catch (ex) {
            false
        }
    }
}
