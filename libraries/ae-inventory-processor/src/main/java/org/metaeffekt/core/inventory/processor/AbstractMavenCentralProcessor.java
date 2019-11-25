/**
 * Copyright 2009-2019 the original author or authors.
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
package org.metaeffekt.core.inventory.processor;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.Properties;

public abstract class AbstractMavenCentralProcessor extends AbstractInventoryProcessor {

    public static final String PROXY_HOST = "proxy.host";
    public static final String PROXY_PORT = "proxy.port";
    public static final String PROXY_USERNAME = "proxy.username";
    public static final String PROXY_PASSWORD = "proxy.password";

    public static final String ARTIFACTID_EXCLUDE_PATTERNS = "artifactid.exclude.patterns";

    private final String mavenCentralHost = "http://search.maven.org";

    protected final String URI_PATTERN_ARTIFACT_SEARCH =
            mavenCentralHost + "/solrsearch/select?q=a:%22${artifactId}%22+AND+v:%22${artifactVersion}%22&wt=xml";

    protected final String URI_PATTERN_ARTIFACT_RESOLVE =
            mavenCentralHost + "/solrsearch/select?q=g:%22${groupId}%22+AND+a:%22${artifactId}%22+AND+l:%22%22&wt=xml";

    protected final String URI_PATTERN_ARTIFACT_RESOLVE_METHOD2 =
            mavenCentralHost + "/solrsearch/select?q=g:%22${groupId}%22+AND+a:%22${artifactId}%22&wt=xml";


    public AbstractMavenCentralProcessor(Properties properties) {
        super(properties);
    }

    protected HttpGet createGetRequest() {
        String proxyHost = getProperties().getProperty(PROXY_HOST);
        String proxyPort = getProperties().getProperty(PROXY_PORT);
        // String proxyUsername = getProperties().getProperty(PROXY_USERNAME);
        // String proxyPassword = getProperties().getProperty(PROXY_PASSWORD);
        HttpGet request = new HttpGet("/");
        if (proxyHost != null && proxyPort != null) {
            HttpHost proxy = new HttpHost(proxyHost, Integer.valueOf(proxyPort), "http");
            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            request.setConfig(config);
        }
        return request;
    }

    protected CloseableHttpClient createHttpClient() {
        return HttpClients.createDefault();
    }

}
