/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.security.cvss;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.metaeffekt.core.security.cvss.CvssSource.CvssEntity;
import org.metaeffekt.core.security.cvss.CvssSource.ReportStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class KnownCvssEntities {

    private final static Logger LOG = LoggerFactory.getLogger(KnownCvssEntities.class);

    public static final Map<String, CvssEntity> ENTITIES_BY_KEYNAME = new HashMap<>();
    public static final Map<String, CvssEntity> ENTITIES_BY_NAME = new HashMap<>();
    public static final Map<String, CvssEntity> ENTITIES_BY_EMAIL = new HashMap<>();

    static {
        // NIST CNAs are listed on the:
        //  - official NVD site https://nvd.nist.gov/vuln/cvmap --> https://nvd.nist.gov/vuln/cvmap/search
        //  - on cve.org https://www.cve.org/PartnerInformation/ListofPartners
        // the below file cna.json has been generated automatically by extracting contents from the cve.org site.
        // if an update is required, please either let the authors of this file know or update the file directly by
        // using the schema file provided in the "resources" directory.
        parseEntitiesFromResource("/cvss/entities/cna.json", KnownCvssEntities.class);
        // assessment "status" files
        parseEntitiesFromResource("/cvss/entities/assessment.json", KnownCvssEntities.class);
        // others, such as advisory providers or ones that are not listed in the CNA list
        parseEntitiesFromResource("/cvss/entities/other.json", KnownCvssEntities.class);
    }

    public final static CvssEntity OTHER = ENTITIES_BY_KEYNAME.get("OTHER");

    public final static CvssEntity NVD = ENTITIES_BY_KEYNAME.get("NIST_NVD");
    public final static CvssEntity GHSA = ObjectUtils.firstNonNull(ENTITIES_BY_KEYNAME.get("CVE_CNA_GITHUB_M"), ENTITIES_BY_NAME.get("GitHub, Inc."));
    public final static CvssEntity CERT_SEI = ENTITIES_BY_KEYNAME.get("CERT_SEI");
    public final static CvssEntity MSRC = ENTITIES_BY_KEYNAME.get("CVE_CNA_MICROSOFT");

    public final static CvssEntity ASSESSMENT = ENTITIES_BY_KEYNAME.get("ASSESSMENT");
    public final static CvssEntity ASSESSMENT_LOWER = ENTITIES_BY_KEYNAME.get("ASSESSMENT_LOWER");
    public final static CvssEntity ASSESSMENT_HIGHER = ENTITIES_BY_KEYNAME.get("ASSESSMENT_HIGHER");
    public final static CvssEntity ASSESSMENT_ALL = ENTITIES_BY_KEYNAME.get("ASSESSMENT_ALL");

    public static Optional<CvssEntity> findByNameOrMail(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(ObjectUtils.firstNonNull(
                ENTITIES_BY_NAME.get(name),
                ENTITIES_BY_EMAIL.get(name),
                ENTITIES_BY_KEYNAME.get(name)
        ));
    }

    public static CvssEntity findByNameOrMailOrCreateNew(String name) {
        return findByNameOrMail(name)
                .orElseGet(() -> new CvssEntity(name));
    }

    private static void parseEntitiesFromResource(String resourcePath, Class<?> clazz) {
        try (InputStream is = clazz.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }

            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 final BufferedReader reader = new BufferedReader(isr)) {
                final JSONTokener tokener = new JSONTokener(reader);
                final JSONObject json = new JSONObject(tokener);

                parseEntitiesFromJson(json);
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource to parse as CVSS entities: " + resourcePath, e);
        }
    }

    public static void parseEntitiesFromJson(JSONObject json) {
        final Set<String> entityKeys = json.keySet();
        final Map<String, List<String>> dependencyGraph = new HashMap<>();
        final Set<String> roots = new HashSet<>();

        // build dependency graph and find roots
        for (String key : entityKeys) {
            final JSONObject entity = json.getJSONObject(key);

            String root = entity.optString("root", "");
            String topLevelRoot = entity.optString("topLevelRoot", "");

            if (!root.isEmpty()) {
                dependencyGraph.computeIfAbsent(root, k -> new ArrayList<>()).add(key);
            }
            if (!topLevelRoot.isEmpty() && !topLevelRoot.equals(root)) {
                dependencyGraph.computeIfAbsent(topLevelRoot, k -> new ArrayList<>()).add(key);
            }
            if (root.isEmpty() && topLevelRoot.isEmpty()) {
                roots.add(key); // entities without a root or topLevelRoot are roots themselves
            }
        }

        // topological sort
        final List<String> sortedKeys = new ArrayList<>();
        final Set<String> visited = new HashSet<>();
        for (String root : roots) {
            topologicalSort(root, dependencyGraph, visited, sortedKeys);
        }

        for (int i = sortedKeys.size() - 1; i >= 0; i--) {
            final String key = sortedKeys.get(i);
            final CvssEntity cvssEntity = parseEntityFromJson(json.getJSONObject(key));

            ENTITIES_BY_KEYNAME.put(key, cvssEntity);
            if (cvssEntity.getName() != null) {
                ENTITIES_BY_NAME.put(cvssEntity.getName(), cvssEntity);
            }
            if (cvssEntity.getEmail() != null) {
                ENTITIES_BY_EMAIL.put(cvssEntity.getEmail(), cvssEntity);
            }
        }
    }

    private static CvssEntity parseEntityFromJson(JSONObject json) {
        final String name = json.optString("name", null);
        final String email = json.optString("email", null);
        final String url = json.optString("url", null);
        final String cveOrgDetailsLink = json.optString("cveOrgDetailsLink", null);
        final String description = json.optString("description", null);
        final String country = json.optString("country", null);
        final String role = json.optString("role", null);
        final List<String> organizationTypes = json.has("organizationTypes")
                ? json.getJSONArray("organizationTypes").toList().stream()
                .map(Object::toString)
                .collect(Collectors.toList())
                : Collections.emptyList();

        final String topLevelRoot = json.optString("topLevelRoot", null);
        final String root = json.optString("root", null);

        final List<ReportStep> reportSteps = ReportStep.fromJson(json.optJSONArray("reportSteps"));

        final CvssEntity topLevelRootEntity = topLevelRoot != null ? ENTITIES_BY_KEYNAME.get(topLevelRoot) : null;
        final CvssEntity rootEntity = root != null ? ENTITIES_BY_KEYNAME.get(root) : null;

        if (topLevelRoot != null && topLevelRootEntity == null) {
            throw new IllegalStateException("Failed to find top level root entity [" + topLevelRoot + "] on entity [" + name + "]");
        } else if (root != null && rootEntity == null) {
            throw new IllegalStateException("Failed to find root entity [" + root + "] on entity [" + name + "]");
        }

        return new CvssEntity(name, email, parseUrl(url), parseUrl(cveOrgDetailsLink), description, country, role, organizationTypes, topLevelRootEntity, rootEntity, reportSteps);
    }

    private static URL parseUrl(String url) {
        if (StringUtils.isEmpty(url)) return null;
        try {
            return new URL(url);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse URL: " + url, e);
        }
    }

    private static void topologicalSort(String node, Map<String, List<String>> dependencyGraph, Set<String> visited, List<String> sortedKeys) {
        if (visited.contains(node)) return;
        visited.add(node);

        final List<String> dependencies = dependencyGraph.get(node);
        if (dependencies != null) {
            for (String dep : dependencies) {
                topologicalSort(dep, dependencyGraph, visited, sortedKeys);
            }
        }
        sortedKeys.add(node);
    }
}
