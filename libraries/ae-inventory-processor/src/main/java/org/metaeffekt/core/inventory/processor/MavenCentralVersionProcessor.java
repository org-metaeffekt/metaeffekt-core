/*
 * Copyright 2009-2021 the original author or authors.
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
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
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

public class MavenCentralVersionProcessor extends AbstractMavenCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MavenCentralVersionProcessor.class);

    public static final String GROUPID_EXCLUDE_PATTERNS = "groupid.exclude.patterns";
    public static final String OVERWRITE_EXISTING_VERSION = "overwrite.existing.version";

    public MavenCentralVersionProcessor(Properties properties) {
        super(properties);
    }

    public void process(Inventory inventory) {
        try {
            final LRUMap queryCache = new LRUMap(300);

            final boolean overwriteVersion =
                    Boolean.valueOf(getProperties().getProperty(OVERWRITE_EXISTING_VERSION, "true"));

            CloseableHttpClient client = createHttpClient();
            HttpGet request = createGetRequest();

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
                    if (!overwriteVersion && StringUtils.hasText(artifact.getLatestVersion())) {
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

                    updateLatestVersion(client, request, artifact, queryCache);
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

    private void updateLatestVersion(CloseableHttpClient client, HttpGet request, Artifact artifact, final LRUMap queryCache) throws URISyntaxException,
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
                    request.setURI(new URI(replacedUri));
                    CloseableHttpResponse response = client.execute(request);
                    try {
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
                    } finally {
                        response.close();
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
                    request.setURI(new URI(replacedUri2));
                    response = client.execute(request);
                    String latestVersionMethod2 = null;
                    try {
                        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            SAXReader reader = new SAXReader();
                            Document document = reader.read(response.getEntity().getContent());
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
                    } finally {
                        response.close();
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
            artifact.setLatestVersion(latestVersions);
        }
    }

}
