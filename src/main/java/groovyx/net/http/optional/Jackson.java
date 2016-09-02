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
package groovyx.net.http.optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovyx.net.http.ChainedHttpConfig;
import groovyx.net.http.CharSequenceInputStream;
import groovyx.net.http.ContentTypes;
import groovyx.net.http.FromServer;
import groovyx.net.http.HttpConfig;
import groovyx.net.http.ToServer;

import java.io.IOException;
import java.io.StringWriter;

import static groovyx.net.http.NativeHandlers.Encoders.handleRawUpload;

public class Jackson {

    public static final String OBJECT_MAPPER_ID = "0w4XJJnlTNK8dvISuCDTlsusPQE=";
    public static final String RESPONSE_TYPE = "GWW35uTkrHwonPt5odeUqBdR3EU=";

    public static Object parse(final ChainedHttpConfig config, final FromServer fromServer) {
        try {
            final ObjectMapper mapper = (ObjectMapper) config.actualContext(fromServer.getContentType(), OBJECT_MAPPER_ID);
            final Class type = (Class) config.actualContext(fromServer.getContentType(), RESPONSE_TYPE);
            return mapper.readValue(fromServer.getReader(), type);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void encode(final ChainedHttpConfig config, final ToServer ts) {
        try {
            if(handleRawUpload(config, ts)) {
                return;
            }

            final ChainedHttpConfig.ChainedRequest request = config.getChainedRequest();
            final ObjectMapper mapper = (ObjectMapper) config.actualContext(request.actualContentType(), OBJECT_MAPPER_ID);
            final Class type = (Class) config.actualContext(request.actualContentType(), RESPONSE_TYPE);
            final StringWriter writer = new StringWriter();
            mapper.writeValue(writer, request.actualBody());
            ts.toServer(new CharSequenceInputStream(writer.toString(), request.actualCharset()));
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void mapper(final HttpConfig config, final ObjectMapper mapper) {
        config.context(ContentTypes.JSON, OBJECT_MAPPER_ID, mapper);
    }

    public static void use(final HttpConfig config) {
        config.getRequest().encoder(ContentTypes.JSON, Jackson::encode);
        config.getResponse().parser(ContentTypes.JSON, Jackson::parse);
    }
    
    public static void toType(final HttpConfig config, final Class type) {
        use(config);
        config.context(ContentTypes.JSON, RESPONSE_TYPE, type);
    }
}
