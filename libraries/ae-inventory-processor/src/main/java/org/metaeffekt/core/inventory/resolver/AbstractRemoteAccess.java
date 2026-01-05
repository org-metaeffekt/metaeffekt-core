/*
 * Copyright 2009-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.inventory.resolver;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.Properties;

public abstract class AbstractRemoteAccess {

    public static final String PROXY_HOST = "proxy.host";
    public static final String PROXY_PORT = "proxy.port";
    public static final String PROXY_USERNAME = "proxy.username";
    public static final String PROXY_PASSWORD = "proxy.password";

    private Properties properties;

    public AbstractRemoteAccess(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    protected HttpGet createGetRequest(String uri) {
        String proxyHost = getProperties().getProperty(PROXY_HOST);
        String proxyPort = getProperties().getProperty(PROXY_PORT);
        // String proxyUsername = getProperties().getProperty(PROXY_USERNAME);
        // String proxyPassword = getProperties().getProperty(PROXY_PASSWORD);
        HttpGet request = new HttpGet(uri);
        if (proxyHost != null && proxyPort != null) {
            String scheme = deriveScheme(uri);
            HttpHost proxy = new HttpHost(proxyHost, Integer.valueOf(proxyPort), scheme);
            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            request.setConfig(config);
        }
        return request;
    }

    private String deriveScheme(String uri) {
        String scheme = "http";
        if (uri.startsWith("https:")) {
            scheme = "https";
        }
        if (uri.startsWith("ftp:")) {
            scheme = "ftp";
        }
        if (uri.startsWith("ftps:")) {
            scheme = "ftps";
        }
        return scheme;
    }

    protected CloseableHttpClient createHttpClient() {
        return HttpClients.createDefault();
    }

}
