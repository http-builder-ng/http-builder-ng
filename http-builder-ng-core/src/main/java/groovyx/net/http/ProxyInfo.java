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

import java.net.*;

public class ProxyInfo {
    private final Proxy proxy;
    private final boolean secure;
    
    public ProxyInfo(final Proxy proxy, final boolean secure) {
        this.proxy = proxy;
        this.secure = secure;
    }
    
    public ProxyInfo(final String host, final int port, final Proxy.Type type, final boolean secure) throws UnknownHostException {
        this.secure = secure;
        final InetAddress address = InetAddress.getByName(host);
        final InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        this.proxy = new Proxy(type, socketAddress);
    }

    public boolean isSecure() {
        return secure;
    }

    public InetAddress getAddress() {
        return getSocketAddress().getAddress();
    }
    
    public InetSocketAddress getSocketAddress() {
        return (InetSocketAddress) proxy.address();
    }

    public int getPort() {
        return getSocketAddress().getPort();
    }

    public Proxy getProxy() {
        return proxy;
    }
}
