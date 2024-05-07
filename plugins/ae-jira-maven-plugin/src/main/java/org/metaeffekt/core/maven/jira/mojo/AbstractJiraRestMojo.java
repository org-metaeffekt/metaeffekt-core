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
package org.metaeffekt.core.maven.jira.mojo;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.maven.jira.util.JsonTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Abstract JIRA access mojo providing access to the JIRA REST API.
 */
public abstract class AbstractJiraRestMojo extends AbstractJiraMojo {

    public static final Base64.Encoder ENCODER = Base64.getEncoder();
    public static final String DEFAULT_FIELDS = "-iconUrl,-creator,-watches,-reporter,-project,-progress,-comment," +
            "-worklog,-subtasks,-assignee,-issuelinks,-timetracking";

    private transient CloseableHttpClient httpClient;

    @Parameter(defaultValue = "20")
    private int concurrentThreads = 20;

    @Parameter(defaultValue = DEFAULT_FIELDS)
    private String fields = DEFAULT_FIELDS;

    /**
     * Search with the JIRA REST API for the result of a JQL statement.
     *
     * @param jql the JIRA Query Language statement to be processed
     *
     * @return The search result
     *
     * @throws IOException IOExceptions may be thrown when accessing files on the filesystem.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Object getSearchResult(String jql) throws IOException {
        getLog().debug("searching for JQL: " + jql);
        String resultString = doSearch(jql);
        Object result = JsonTransformer.transform(resultString);
        final Map<String, Object> expandedIssues = Collections.synchronizedMap(new TreeMap<>());

        final ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);

        // expand issue data with individual REST requests
        if (result instanceof Map) {
            Object issues = ((Map<?, ?>) result).get("issues");
            if (issues instanceof List<?>) {
                for (Object issue : (List<?>) issues) {
                    if (issue instanceof Map<?, ?>) {
                        final Object key = ((Map<?, ?>) issue).get("key");
                        if (key instanceof String) {
                            executor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    expandedIssues.put((String) key, JsonTransformer.transform(getIssue((String) key)));
                                }
                            });
                        }
                    }
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(60, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                }

                // using the tree map preserves the issue order; at the end we only need the values
                ((Map) result).put("issues", expandedIssues.values());
            }
        }
        return result;
    }

    private String getIssue(String key) {
        try {
            getLog().debug("fetching REST issue for key: " + key);
            String url = serverUrl + "/rest/api/latest/issue/" + key + "?fields=" + fields;
            final HttpGet httpGet = new HttpGet(url);
            try (final CloseableHttpResponse response = getClient().execute(httpGet)) {
                checkResponse(response, url);
                final HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try (final InputStream in = entity.getContent()) {
                        return IOUtils.toString(in, getEncoding(entity));
                    }
                }
            }
            throw new IllegalStateException("Cannot query issue: " + key);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot query issue: " + key, e);
        }
    }

    private String doSearch(String jql) throws IOException {
        getLog().debug("executing REST request with JQL: " + jql);

        final String url = serverUrl + "/rest/api/latest/search";

        HashMap<String, Object> requestData = new HashMap<>();
        requestData.put("jql", jql);
        requestData.put("maxResults", "1000");
        final String[] fields = {"key"};
        requestData.put("fields", fields);
        String requestDataString = JsonTransformer.transform(requestData, false);

        final HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(requestDataString));
        final CloseableHttpResponse response = getClient().execute(httpPost);
        try {
            checkResponse(response, url);
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (InputStream in = entity.getContent()) {
                    return IOUtils.toString(in, getEncoding(entity));
                }
            }
        } finally {
            response.close();
        }
        throw new IllegalStateException("Cannot query: " + jql);
    }

    private String getEncoding(HttpEntity entity) {
        final Header contentEncoding = entity.getContentEncoding();
        return contentEncoding == null ? "UTF-8" : contentEncoding.getValue();
    }

    private CloseableHttpClient getClient() {
        if (httpClient == null) {
            try {
                final SSLContextBuilder builder = new SSLContextBuilder();
                builder.loadTrustMaterial(null, (x509Certificates, s) -> true);
                final SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(builder.build());

                List<Header> headers = new ArrayList<>();
                headers.add(new BasicHeader("Authorization", getAuth()));
                headers.add(new BasicHeader("Accept", "application/json"));
                headers.add(new BasicHeader("Content-Type", "application/json"));

                final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
                httpClient = HttpClients.custom().setConnectionManager(connManager).setMaxConnTotal(concurrentThreads).
                        setSSLSocketFactory(socketFactory).setDefaultHeaders(headers).build();
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

        }
        return httpClient;
    }

    protected void closeClient() {
        if (httpClient != null) {
            IOUtils.closeQuietly(httpClient);
            httpClient = null;
        }
    }

    private String getAuth() {
        return "Basic " + new String(ENCODER.encode((userName + ":" + userPassword).getBytes()));
    }

    private void checkResponse(CloseableHttpResponse response, String url) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            getLog().error("Error HTTP status code: " + statusCode);
            getLog().error("for web resource: " + url);
            throw new RuntimeException("Error HTTP status code: " + statusCode + " - " +
                    response.getStatusLine().getReasonPhrase());
        }
    }

}
