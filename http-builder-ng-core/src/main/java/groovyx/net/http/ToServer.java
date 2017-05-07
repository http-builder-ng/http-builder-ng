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

import java.io.InputStream;

/**
 * Adapter interface used to translate request content from the {@link HttpBuilder} API to the specific format required by the underlying client
 * implementation.
 */
public interface ToServer {

    /**
     * Translates the request content appropriately for the underlying client implementation. The contentType will be determined by the request.
     *
     * @param inputStream the request input stream to be translated.
     */
    void toServer(InputStream inputStream);
}
