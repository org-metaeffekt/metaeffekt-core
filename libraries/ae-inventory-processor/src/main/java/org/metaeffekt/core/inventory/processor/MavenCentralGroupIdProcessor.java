/**
 * Copyright 2009-2020 the original author or authors.
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

import org.apache.http.HttpException;
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
import java.util.List;
import java.util.Properties;

public class MavenCentralGroupIdProcessor extends AbstractMavenCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MavenCentralGroupIdProcessor.class);

    public MavenCentralGroupIdProcessor(Properties properties) {
        super(properties);
    }

    public void process(Inventory inventory) {
        try {
            CloseableHttpClient client = createHttpClient();
            HttpGet request = createGetRequest();

            String[] artifactIdFilters = getProperties().getProperty(ARTIFACTID_EXCLUDE_PATTERNS, "").split(",");

            for (Artifact artifact : inventory.getArtifacts()) {
                String artifactId = artifact.getId();
                if (StringUtils.hasText(artifactId)) {
                    boolean skip = false;
                    if (StringUtils.hasText(artifact.getGroupId())) {
                        skip = true;
                    }
                    if (artifactId.contains(" ")) {
                        skip = true;
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

                    try {
                        updateGroupId(client, request, artifact);
                    } catch (Exception e) {
                        LOG.error("Cannot query group id for {}.", artifactId, e);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Cannot access maven central.", e);
        }

    }


    private void updateGroupId(CloseableHttpClient client, HttpGet request,  Artifact artifact)
            throws IOException, HttpException, DocumentException, URISyntaxException {

        final String id = artifact.getId();
        String version = artifact.getVersion();

        if (!StringUtils.hasText(version)) {
            // we need to make a good guess:
            version = artifact.deriveVersionFromId();
        }

        if (!StringUtils.hasText(version)) {
            return;
        }

        String artifactId = artifact.extractArtifactId(id, version);

        if (!StringUtils.hasText(artifactId)) {
            return;
        }

        String replacedUri = URI_PATTERN_ARTIFACT_SEARCH.replaceAll("\\$\\{artifactId\\}", artifactId);

        replacedUri = replacedUri.replaceAll("\\$\\{artifactVersion\\}", version.replace(" ", ""));
        request.setURI(new URI(replacedUri));

        LOG.info(replacedUri);

        CloseableHttpResponse response = client.execute(request);
        try {
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

            if (groupIds != null) {
                if (!groupIds.contains("|")) {
                    artifact.setVersion(version);
                    artifact.setGroupId(groupIds);
                    artifact.setArtifactId(null);
                    artifact.deriveArtifactId();
                    LOG.info("Updated groupId for artifact {}: {}", id, groupIds);
                } else {
                    LOG.info("Update of groupId skipped. Found groupId ambiguous: {}", groupIds);
                }
            }
        } finally {
            response.close();
        }
    }

}
