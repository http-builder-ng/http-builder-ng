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

import java.util.function.Supplier;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.Collections;

public class Safe {

    
    public static boolean register(final HttpConfig config,
                                   final Supplier<Boolean> shouldRegister,
                                   final List<String> contentTypes,
                                   final Supplier<BiConsumer<ChainedHttpConfig,ToServer>> encoderSupplier,
                                   final Supplier<BiFunction<ChainedHttpConfig,FromServer,Object>> parserSupplier) {
        
        if(shouldRegister.get()) {
            config.getRequest().encoder(contentTypes, encoderSupplier.get());
            config.getResponse().parser(contentTypes, parserSupplier.get());
            return true;
        }

        return false;
    }
    
    public static boolean register(final HttpConfig config,
                                   final Supplier<Boolean> shouldRegister,
                                   final String contentType,
                                   final Supplier<BiConsumer<ChainedHttpConfig,ToServer>> encoderSupplier,
                                   final Supplier<BiFunction<ChainedHttpConfig,FromServer,Object>> parserSupplier) {
        return register(config, shouldRegister, Collections.singletonList(contentType), encoderSupplier, parserSupplier);
    }

    public static Supplier<Boolean> ifClassIsLoaded(final String className) {
        return () -> {
            try {
                Class.forName(className);
                return true;
            }
            catch(ClassNotFoundException cnfe) {
                return false;
            }
        };
    }
}
