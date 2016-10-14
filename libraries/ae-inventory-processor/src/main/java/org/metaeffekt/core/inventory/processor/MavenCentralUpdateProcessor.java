/**
 * Copyright 2009-2016 the original author or authors.
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

import org.apache.commons.collections.map.LRUMap;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class MavenCentralUpdateProcessor extends AbstractInventoryProcessor {

    public static final String PROXY_HOST = "proxy.host";
    public static final String PROXY_PORT = "proxy.port";
    public static final String PROXY_USERNAME = "proxy.username";
    public static final String PROXY_PASSWORD = "proxy.password";
    public static final String OVERWRITE_EXISTING_VERSION = "overwrite.existing.version";
    public static final String GROUPID_EXCLUDE_PATTERNS = "groupid.exclude.patterns";
    public static final String ARTIFACTID_EXCLUDE_PATTERNS = "artifactid.exclude.patterns";
    private static final Logger LOG = LoggerFactory.getLogger(MavenCentralUpdateProcessor.class);
    final String URI_PATTERN_ARTIFACT_SEARCH =
            "http://search.maven.org/solrsearch/select?q=a:%22${artifactId}%22+AND+v:%22${artifactVersion}%22&wt=xml";

    final String URI_PATTERN_ARTIFACT_RESOLVE =
            "http://search.maven.org/solrsearch/select?q=g:%22${groupId}%22+AND+a:%22${artifactId}%22+AND+l:%22%22&wt=xml";

    final String URI_PATTERN_ARTIFACT_RESOLVE_METHOD2 =
            "http://search.maven.org/solrsearch/select?q=g:%22${groupId}%22+AND+a:%22${artifactId}%22&wt=xml";


    public MavenCentralUpdateProcessor() {
    }

    public MavenCentralUpdateProcessor(Properties properties) {
        super(properties);
    }

    public void process(Inventory inventory) {
        try {
            final LRUMap queryCache = new LRUMap(300);

            final boolean overwriteVersion =
                    Boolean.valueOf(getProperties().getProperty(OVERWRITE_EXISTING_VERSION, "true"));

            String proxyHost = getProperties().getProperty(PROXY_HOST);
            String proxyPort = getProperties().getProperty(PROXY_PORT);
            String proxyUsername = getProperties().getProperty(PROXY_USERNAME);
            String proxyPassword = getProperties().getProperty(PROXY_PASSWORD);

            DefaultHttpClient client = new DefaultHttpClient();
            if (StringUtils.hasText(proxyHost)) {
                int proxyPortInt = Integer.parseInt(proxyPort);
                HttpHost proxy = new HttpHost(proxyHost, proxyPortInt);
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                if (StringUtils.hasText(proxyUsername)) {
                    Credentials proxyCreds = new UsernamePasswordCredentials(proxyUsername, proxyPassword);
                    AuthScope proxyAuthScope = new AuthScope(proxyHost, proxyPortInt);
                    client.getCredentialsProvider().setCredentials(proxyAuthScope, proxyCreds);
                }
            }

            HttpGet getMethod = new HttpGet();

            String[] groupIdFilters =
                    getProperties().getProperty(GROUPID_EXCLUDE_PATTERNS, "").split(",");
            String[] artifactIdFilters =
                    getProperties().getProperty(ARTIFACTID_EXCLUDE_PATTERNS, "").split(",");

            for (Artifact artifact : inventory.getArtifacts()) {
                String artifactId = artifact.getId();

                if (StringUtils.hasText(artifactId)) {
                    int index = artifactId.indexOf("-" + artifact.getVersion());
                    if (index != -1) {
                        artifactId = artifactId.substring(0, index);
                    }
                    if (!overwriteVersion &&
                            StringUtils.hasText(artifact.getLatestAvailableVersion())) {
                        continue;
                    }

                    boolean skip = false;

                    if (StringUtils.hasText(artifact.getGroupId())) {
                        for (int i = 0; i < groupIdFilters.length; i++) {
                            if (artifact.getGroupId().matches(groupIdFilters[i])) {
                                skip = true;
                            }
                        }
                    }

                    if (!skip) {
                        for (int i = 0; i < artifactIdFilters.length; i++) {
                            if (artifactId.matches(artifactIdFilters[i])) {
                                skip = true;
                            }
                        }
                    }

                    if (skip) {
                        continue;
                    }

                    updateGroupId(client, getMethod, artifact);
                    updateLatestVersion(client, getMethod, artifact, queryCache);
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (HttpException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateLatestVersion(HttpClient client,
                                     HttpGet getMethod, Artifact artifact, final LRUMap queryCache) throws URISyntaxException,
            IOException, HttpException, DocumentException {

        String artifactId = artifact.getId();
        int index = artifactId.indexOf("-" + artifact.getVersion());
        if (index != -1) {
            artifactId = artifactId.substring(0, index);
        }
        if (StringUtils.hasText(artifact.getGroupId())) {
            String[] groupIds = artifact.getGroupId().split("\\|");
            String latestVersions = null;
            for (int i = 0; i < groupIds.length; i++) {
                String replacedUri = URI_PATTERN_ARTIFACT_RESOLVE.
                        replaceAll("\\$\\{artifactId\\}", artifactId.replace(" ", "-"));
                replacedUri = replacedUri.
                        replaceAll("\\$\\{groupId\\}", groupIds[i].replace(" ", ""));

                LOG.info("Querying: {}", replacedUri);
                String latestVersion = (String) queryCache.get(replacedUri);
                String latestVersionMethod1 = null;
                if (latestVersion == null) {
                    getMethod.setURI(new URI(replacedUri));
                    HttpResponse response = client.execute(getMethod);
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        SAXReader reader = new SAXReader();
                        Document document =
                                reader.read(response.getEntity().getContent());
                        Node node;
                        LinkedHashMap<String, String> map =
                                new LinkedHashMap<String, String>();
                        List<String> timestamps = new ArrayList<String>();
                        @SuppressWarnings("unchecked")
                        List<Node> docNodes =
                                document.selectNodes("//response/result/doc");
                        if (docNodes != null) {
                            for (Object result : docNodes) {
                                Node resultNode = (Node) result;
                                node = resultNode.selectSingleNode("str[@name='v']");
                                String version = node.getText();
                                node = resultNode.selectSingleNode("long[@name='timestamp']");
                                String timestamp = node.getText();

                                // the timestamp alone may have duplicated
                                // therefore we append the version
                                timestamp = timestamp + version;

                                map.put(timestamp, version);
                                timestamps.add(timestamp);
                            }
                            if (!timestamps.isEmpty()) {
                                Collections.sort(timestamps);
                                latestVersionMethod1 =
                                        map.get(timestamps.get(timestamps.size() - 1));
                            }
                        }
                    }

                    // method2
                    String replacedUri2 =
                            URI_PATTERN_ARTIFACT_RESOLVE_METHOD2.
                                    replaceAll("\\$\\{artifactId\\}",
                                            artifactId.replace(" ", "-"));
                    replacedUri2 =
                            replacedUri2.
                                    replaceAll("\\$\\{groupId\\}",
                                            groupIds[i].replace(" ", ""));
                    getMethod.setURI(new URI(replacedUri2));
                    HttpResponse response2 = client.execute(getMethod);

                    String latestVersionMethod2 = null;
                    if (response2.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        SAXReader reader = new SAXReader();
                        Document document = reader.read(response2.getEntity().getContent());
                        int numEntries = Integer.parseInt(document.
                                selectSingleNode("//response/result").valueOf("@numFound"));

                        if (numEntries == 1) {
                            Node node = document.
                                    selectSingleNode("//response/result/doc/str[@name='latestVersion']");
                            if (node != null) {
                                latestVersionMethod2 = node.getText();
                            }
                        }
                    }

                    if (latestVersionMethod1 != null) {
                        latestVersion = latestVersionMethod1;

                        if (!latestVersionMethod1.equals(latestVersionMethod2)) {
                            latestVersion = latestVersion + "|" + latestVersionMethod2;
                        }
                    } else {
                        latestVersion = latestVersionMethod2;
                    }

                    if (latestVersion == null) {
                        latestVersion = "n.a.";
                    }
                    if (latestVersions == null) {
                        latestVersions = latestVersion;
                    } else {
                        latestVersions += "|" + latestVersion;
                    }
                    queryCache.put(replacedUri, latestVersions);
                } else {
                    latestVersions = latestVersion;
                }
            }
            LOG.info("Latest version: {}", latestVersions);
            artifact.setLatestAvailableVersion(latestVersions);
        }
    }

    private void updateGroupId(HttpClient client, HttpGet getMethod, Artifact artifact)
            throws IOException, HttpException, DocumentException, URISyntaxException {

        if (!StringUtils.hasText(artifact.getGroupId())
                && StringUtils.hasText(artifact.getVersion())) {

            String replacedUri = URI_PATTERN_ARTIFACT_SEARCH.
                    replaceAll("\\$\\{artifactId\\}", artifact.getId().replace(" ", "-"));
            replacedUri = replacedUri.
                    replaceAll("\\$\\{artifactVersion\\}", artifact.getVersion().replace(" ", ""));
            getMethod.setURI(new URI(replacedUri));
            HttpResponse response = client.execute(getMethod);

            SAXReader reader = new SAXReader();
            Document document = reader.read(response.getEntity().getContent());

            @SuppressWarnings("unchecked")
            List<Node> node = document.selectNodes("//response/result/doc/str[@name='g']");

            String groupIds = null;
            for (Node node2 : node) {
                if (groupIds == null) {
                    groupIds = node2.getText();
                } else {
                    groupIds += "|" + node2.getText();
                }
            }

            artifact.setGroupId(groupIds);
            LOG.info("Updated groupId: {}", groupIds);
        }
    }

}
