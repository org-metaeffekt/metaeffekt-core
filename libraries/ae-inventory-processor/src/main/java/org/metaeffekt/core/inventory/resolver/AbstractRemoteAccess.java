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

import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Setter
@Getter
public abstract class AbstractRemoteAccess {

    public static final String PROXY_HOST = "proxy.host";
    public static final String PROXY_PORT = "proxy.port";

    private Properties properties;
    
    private List<ServerCredential> credentials = new ArrayList<>();

    public AbstractRemoteAccess(Properties properties) {
        this.properties = properties;
    }

    protected HttpGet createGetRequest(String uri) {
        String proxyHost = getProperties().getProperty(PROXY_HOST);
        String proxyPort = getProperties().getProperty(PROXY_PORT);
        HttpGet request = new HttpGet(uri);
        if (proxyHost != null && proxyPort != null) {
            String scheme = deriveScheme(uri);
            HttpHost proxy = new HttpHost(proxyHost, Integer.valueOf(proxyPort), scheme);
            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            request.setConfig(config);
        }

        if (credentials != null) {
            for (ServerCredential credential : credentials) {
                if (credential.getMatchUrl() != null && uri.startsWith(credential.getMatchUrl())) {
                    if (credential.getToken() != null && !credential.getToken().trim().isEmpty()) {
                        request.addHeader("Authorization", "Bearer " + credential.getToken());
                        break;
                    } else if (credential.getUsername() != null && credential.getPassword() != null) {
                        String auth = credential.getUsername() + ":" + credential.getPassword();
                        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
                        request.addHeader("Authorization", "Basic " + new String(encodedAuth));
                        break;
                    }
                }
            }
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
