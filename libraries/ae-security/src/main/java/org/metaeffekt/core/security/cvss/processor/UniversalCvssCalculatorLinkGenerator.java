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
package org.metaeffekt.core.security.cvss.processor;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.KnownCvssEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Accepts optionally named {@link org.metaeffekt.core.security.cvss.CvssVector} instances to generate a link to the
 * <a href="https://metaeffekt.com/security/cvss/calculator">Universal CVSS Calculator</a>.
 */
public class UniversalCvssCalculatorLinkGenerator {

    private final static Logger LOG = LoggerFactory.getLogger(UniversalCvssCalculatorLinkGenerator.class);

    private String baseUrl = "https://metaeffekt.com/security/cvss/calculator";

    private final List<UniversalCvssCalculatorEntry<?>> entries = new ArrayList<>();
    private final Set<String> openSections = new LinkedHashSet<>();
    private final Set<String> cves = new LinkedHashSet<>();
    private CvssVector selectedVector;

    public UniversalCvssCalculatorLinkGenerator() {
    }

    public <V extends CvssVector> UniversalCvssCalculatorEntry<V>  addVectorNullThrowing(V cvssVector, String name, boolean visible) {
        if (cvssVector == null) {
            throw new IllegalArgumentException("CVSS vector to be added must not be null.");
        }
        final UniversalCvssCalculatorEntry<V> entry = new UniversalCvssCalculatorEntry<>(cvssVector, name, visible);
        entries.add(entry);
        return entry;
    }

    public <V extends CvssVector> UniversalCvssCalculatorEntry<V>  addVectorNullThrowing(V cvssVector) {
        return addVectorNullThrowing(cvssVector, cvssVector == null ? "unknown" : cvssVector.getCombinedCvssSource(true).replace("CVSS:", ""), true);
    }

    public <V extends CvssVector> UniversalCvssCalculatorEntry<V>  addVector(V cvssVector, String name, boolean visible) {
        if (cvssVector == null) {
            return new UniversalCvssCalculatorEntry<>(null, name, visible);
        }
        final UniversalCvssCalculatorEntry<V> entry = new UniversalCvssCalculatorEntry<>(cvssVector, name, visible);
        entries.add(entry);
        return entry;
    }

    public <V extends CvssVector> UniversalCvssCalculatorEntry<V>  addVector(V cvssVector) {
        return addVector(cvssVector, cvssVector == null ? "unknown" : cvssVector.getCombinedCvssSource(true).replace("CVSS:", ""), true);
    }

    public <V extends CvssVector> UniversalCvssCalculatorEntry<V> addVectorForVulnerability(V cvssVector, String vulnerabilityName) {
        if (StringUtils.isEmpty(vulnerabilityName)) {
            return addVector(cvssVector);
        }

        // 3.1 2020-1234 (nist.gov)
        final StringBuilder nameBuilder = new StringBuilder();

        if (cvssVector == null) {
            nameBuilder.append("unknown ");
        } else {
            nameBuilder.append(cvssVector.getName().replace("CVSS:", "")).append(" ");
        }

        if (vulnerabilityName.startsWith("CVE-")) {
            nameBuilder.append(vulnerabilityName.replace("CVE-", "")).append(" ");
        } else {
            nameBuilder.append(vulnerabilityName).append(" ");
        }

        // get all sources, add them in brackets split by comma. use the email source if available and only use the part after the @
        if (cvssVector != null) {
            final List<CvssSource> sources = cvssVector.getCvssSources();
            if (!sources.isEmpty()) {
                final StringJoiner sourceJoiner = new StringJoiner(", ", "(", ")");
                for (CvssSource source : sources) {
                    if (source.getIssuingEntity() != null && KnownCvssEntities.ASSESSMENT != source.getHostingEntity()) {
                        final String[] parts = StringUtils.firstNonEmpty(source.getIssuingEntity().getEmail(), source.getIssuingEntity().getName()).split("@");
                        sourceJoiner.add(parts[parts.length - 1]);
                    } else if (source.getHostingEntity() != null) {
                        final String[] parts = StringUtils.firstNonEmpty(source.getHostingEntity().getEmail(), source.getHostingEntity().getName()).split("@");
                        sourceJoiner.add(parts[parts.length - 1]);
                    }
                }
                nameBuilder.append(sourceJoiner);
            }
        }

        return addVector(cvssVector, nameBuilder.toString().trim(), true);
    }

    public UniversalCvssCalculatorLinkGenerator addOpenSection(String section) {
        openSections.add(section);
        return this;
    }

    public UniversalCvssCalculatorLinkGenerator addOpenSections(String... section) {
        for (String s : section) {
            addOpenSection(s);
        }
        return this;
    }

    public UniversalCvssCalculatorLinkGenerator addOpenSections(Collection<String> sections) {
        openSections.addAll(sections);
        return this;
    }

    public UniversalCvssCalculatorLinkGenerator setSelectedVectorNullThrowing(CvssVector selectedVector) {
        if (findVectorEntryByVector(selectedVector) == null) {
            throw new IllegalArgumentException("Selected vector must be added to the link generator first.");
        }
        this.selectedVector = selectedVector;
        return this;
    }

    public UniversalCvssCalculatorLinkGenerator setSelectedVectorNullThrowing(String selectedVector) {
        final UniversalCvssCalculatorEntry<?> foundVector = findVectorEntryByName(selectedVector);
        if (foundVector == null) {
            throw new IllegalArgumentException("Selected vector must be added to the link generator first.");
        }
        this.selectedVector = foundVector.getCvssVector();
        return this;
    }

    public UniversalCvssCalculatorLinkGenerator setSelectedVector(CvssVector selectedVector) {
        if (findVectorEntryByVector(selectedVector) != null) {
            this.selectedVector = selectedVector;
        }
        return this;
    }

    public UniversalCvssCalculatorLinkGenerator setSelectedVector(String selectedVector) {
        final UniversalCvssCalculatorEntry<?> foundVector = findVectorEntryByName(selectedVector);
        if (foundVector != null) {
            this.selectedVector = foundVector.getCvssVector();
        }
        return this;
    }

    public UniversalCvssCalculatorLinkGenerator setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public UniversalCvssCalculatorLinkGenerator addCve(String cve) {
        cves.add(cve);
        return this;
    }

    public boolean isEmpty() {
        return entries.isEmpty() && cves.isEmpty();
    }

    protected <V extends CvssVector> UniversalCvssCalculatorEntry<V> findVectorEntryByVector(V searchVector) {
        if (searchVector == null) {
            return null;
        }
        for (UniversalCvssCalculatorEntry<?> entry : entries) {
            if (entry.getCvssVector().equals(searchVector)) {
                return (UniversalCvssCalculatorEntry<V>) entry;
            }
        }
        return null;
    }

    protected UniversalCvssCalculatorEntry<?> findVectorEntryByName(String searchVectorName) {
        if (searchVectorName == null) {
            return null;
        }
        for (UniversalCvssCalculatorEntry<?> entry : entries) {
            if (entry.getName().equals(searchVectorName)) {
                return entry;
            }
        }
        return null;
    }

    public String generateOptimizedLink() {
        final List<String> candidates = Arrays.asList(
                generateBas64EncodedGzipCompressedLink(),
                generateLink()
        );
        final Optional<String> optimizedLink = candidates.stream()
                .filter(Objects::nonNull)
                .filter(link -> link.length() < 2000)
                .min(Comparator.comparingInt(String::length));
        return optimizedLink.orElse(generateLink());
    }

    public String generateLink() {
        final StringBuilder linkBuilder = new StringBuilder();
        linkBuilder.append(baseUrl);

        final Map<String, String> parameters = generateUrlParameters(true);

        if (!parameters.isEmpty()) {
            linkBuilder.append("?");
            final List<String> parameterStrings = new ArrayList<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                parameterStrings.add(entry.getKey() + "=" + entry.getValue());
            }
            linkBuilder.append(String.join("&", parameterStrings));
        }

        return linkBuilder.toString();
    }

    public String generateBas64EncodedGzipCompressedLink() {
        final StringBuilder linkBuilder = new StringBuilder();
        linkBuilder.append(baseUrl);

        final Map<String, String> parameters = generateUrlParameters(false);

        if (!parameters.isEmpty()) {
            linkBuilder.append("?b64gzip=");
            final List<String> parameterStrings = new ArrayList<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                parameterStrings.add(entry.getKey() + "=" + entry.getValue());
            }
            final String dataToBeGZipped = String.join("&", parameterStrings);

            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
                gzipOutputStream.write(dataToBeGZipped.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                LOG.error("Failed to compress data: {}", dataToBeGZipped, e);
                return generateLink();
            }

            final String base64Encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOutputStream.toByteArray());
            String urlEncoded;
            try {
                urlEncoded = URLEncoder.encode(base64Encoded, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LOG.error("Failed encode parameter value, resuming with default platform encoding: {}", base64Encoded, e);
                urlEncoded = URLEncoder.encode(base64Encoded);
            }
            linkBuilder.append(urlEncoded);
        }

        return linkBuilder.toString();
    }

    private Map<String, String> generateUrlParameters(boolean urlEncode) {
        final Map<String, String> parameters = new LinkedHashMap<>();

        if (!entries.isEmpty()) {
            final JSONArray vectorArray = new JSONArray();
            entries.stream().map(UniversalCvssCalculatorEntry::generateCvssVectorArrayEntry).forEach(vectorArray::put);
            parameters.put("vector", vectorArray.toString());
        }

        if (!openSections.isEmpty()) {
            parameters.put("open", String.join(",", openSections));
        }

        if (selectedVector != null) {
            final UniversalCvssCalculatorEntry<?> selectedVectorEntry = findVectorEntryByVector(selectedVector);
            if (selectedVectorEntry != null) {
                parameters.put("selected", selectedVectorEntry.getName());
            }
        }

        if (!cves.isEmpty()) {
            parameters.put("cve", String.join(",", cves));
        }

        if (urlEncode) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                try {
                    entry.setValue(URLEncoder.encode(entry.getValue(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    LOG.error("Failed encode parameter value, resuming with default platform encoding: {}", entry.getValue(), e);
                    entry.setValue(URLEncoder.encode(entry.getValue()));
                }
            }
        }

        return parameters;
    }

    @Override
    public String toString() {
        return generateLink();
    }

    public static class UniversalCvssCalculatorEntry<V extends CvssVector> {
        private final V cvssVector;
        private V initialCvssVector;
        private String name;
        private boolean visible;

        public UniversalCvssCalculatorEntry(V cvssVector, String name, boolean visible) {
            this.cvssVector = cvssVector;
            this.name = name;
            this.visible = visible;
        }

        public String getName() {
            return name;
        }

        public UniversalCvssCalculatorEntry<V> setName(String name) {
            this.name = name;
            return this;
        }

        public UniversalCvssCalculatorEntry<V> setInitialCvssVector(V initialCvssVector) {
            this.initialCvssVector = initialCvssVector;
            return this;
        }

        public UniversalCvssCalculatorEntry<V> setInitialCvssVectorUnchecked(CvssVector initialCvssVectorUnchecked) {
            this.initialCvssVector = (V) initialCvssVectorUnchecked;
            return this;
        }

        public V getCvssVector() {
            return cvssVector;
        }

        public V getInitialCvssVector() {
            return initialCvssVector;
        }

        public boolean isVisible() {
            return visible;
        }

        public UniversalCvssCalculatorEntry<V> setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public JSONArray generateCvssVectorArrayEntry() {
            final JSONArray vectorEntry = new JSONArray();

            vectorEntry.put(this.name);
            vectorEntry.put(this.isVisible());
            vectorEntry.put(this.cvssVector == null ? "" : this.cvssVector.toString());
            vectorEntry.put(this.cvssVector == null ? "CVSS:2.0" : this.cvssVector.getName());

            if (this.initialCvssVector != null) vectorEntry.put(this.initialCvssVector.toString());
            else vectorEntry.put((String) null);

            return vectorEntry;
        }
    }
}