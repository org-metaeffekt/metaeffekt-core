/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.itest.common.download;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WebAccess {

    private final static Logger LOG = LoggerFactory.getLogger(WebAccess.class);

    private HttpHost proxy;

    private CredentialsProvider credentialsProvider;

    /**
     * Set up a proxy in between the host to access (host, port, scheme, username, password).<br>
     * Username and password are optional, only used if authentication is requested by the proxy.
     *
     * @param scheme The proxy scheme (ex. http).
     * @param host The proxy host.
     * @param port The proxy port.
     * @param username Proxy user (if authentication is required).
     * @param password Proxy password (if authentication is required).
     */
    public void setDownloaderProxyCredentials(String scheme, String host, int port, String username, String password) {
        LOG.debug("Set up proxy credentials for Downloader");
        this.proxy = new HttpHost(host, port, scheme);
        this.credentialsProvider = new BasicCredentialsProvider();
        this.credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
    }

    private CloseableHttpClient createHttpClient() {
        final HttpClientBuilder httpClient = HttpClients.custom();

        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(60 * 1000) // max time to establish a connection with remote host/server
                .setConnectionRequestTimeout(60 * 1000) // time to wait for getting a connection from the connection manager/pool
                .setSocketTimeout(60 * 1000) // max time gap between two consecutive data packets while transferring data from server to client
                .build();
        httpClient.setDefaultRequestConfig(requestConfig);

        if (credentialsProvider != null) {
            httpClient.setDefaultCredentialsProvider(credentialsProvider);
        }

        return httpClient.build();
    }

    private RequestConfig createProxyRequestConfig() {
        if (proxy != null) {
            return RequestConfig.custom()
                    .setProxy(proxy)
                    .build();
        } else {
            return RequestConfig.custom()
                    .build();
        }
    }

    public InputStream fetchResponseBodyFromUrlAsInputStream(URL url, Map<String, String> requestHeaders) {
        LOG.info("Performing request to {}", url);

        return new Retry<>(() -> {
            try (CloseableHttpClient httpClient = createHttpClient()) {
                final HttpGet httpGet = new HttpGet(url.toURI());

                httpGet.setConfig(createProxyRequestConfig());

                if (requestHeaders != null && !requestHeaders.isEmpty()) {
                    for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                        httpGet.addHeader(header.getKey(), header.getValue());
                    }
                }

                final HttpResponse response = httpClient.execute(httpGet);

                if (response.getStatusLine().getStatusCode() != 200) throw new IllegalArgumentException();
                final HttpEntity entity = response.getEntity();

                return IOUtils.toBufferedInputStream(entity.getContent());
            } catch (Exception e) {
                throw new RuntimeException("Unable to fetch response body from URL " + url.toString(), e);
            }
        })
                .retryCount(3)
                .withDelay(1000)
                .run();
    }

    public List<String> fetchResponseBodyFromUrlAsList(URL url, Map<String, String> requestHeaders) {
        final InputStream inputStream = fetchResponseBodyFromUrlAsInputStream(url, requestHeaders);
        final LineIterator lineIterator;
        try {
            lineIterator = IOUtils.lineIterator(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read response body from URL " + url.toString(), e);
        }

        final List<String> lines = new ArrayList<>();
        while (lineIterator.hasNext()) {
            lines.add(lineIterator.next());
        }

        return lines;
    }

    public List<String> fetchResponseBodyFromUrlAsList(URL url) {
        return fetchResponseBodyFromUrlAsList(url, null);
    }

    public void fetchResponseBodyFromUrlToFile(URL url, File file, Map<String, String> requestHeaders) {
        final InputStream inputStream = fetchResponseBodyFromUrlAsInputStream(url, requestHeaders);

        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            try (final FileOutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to write response from " + url.toString() + " to " + file.getAbsolutePath(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public void fetchResponseBodyFromUrlToFile(URL url, File file) {
        fetchResponseBodyFromUrlToFile(url, file, null);
    }

}

