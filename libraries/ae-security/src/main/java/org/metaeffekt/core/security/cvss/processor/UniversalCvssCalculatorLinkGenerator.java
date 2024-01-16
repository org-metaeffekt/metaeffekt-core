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
package org.metaeffekt.core.security.cvss.processor;

import org.json.JSONArray;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Accepts optionally named {@link org.metaeffekt.core.security.cvss.CvssVector} instances to generate a link to the
 * <a href="https://www.metaeffekt.com/security/cvss/calculator">Universal CVSS Calculator</a>.
 */
public class UniversalCvssCalculatorLinkGenerator {

    private final static Logger LOG = LoggerFactory.getLogger(UniversalCvssCalculatorLinkGenerator.class);

    private String baseUrl = "https://www.metaeffekt.com/security/cvss/calculator";

    private final List<UniversalCvssCalculatorEntry> entries = new ArrayList<>();
    private final Set<String> openSections = new LinkedHashSet<>();
    private final Set<String> cves = new LinkedHashSet<>();
    private CvssVector selectedVector;

    public UniversalCvssCalculatorLinkGenerator() {
    }

    public UniversalCvssCalculatorEntry addVector(CvssVector cvssVector, String name, boolean visible) {
        if (cvssVector == null) {
            throw new IllegalArgumentException("CVSS vector to be added must not be null.");
        }
        final UniversalCvssCalculatorEntry entry = new UniversalCvssCalculatorEntry(cvssVector, name, visible);
        entries.add(entry);
        return entry;
    }

    public UniversalCvssCalculatorEntry addVector(CvssVector cvssVector, String name) {
        return addVector(cvssVector, name, true);
    }

    public UniversalCvssCalculatorEntry addVector(CvssVector cvssVector, boolean visible) {
        return addVector(cvssVector, cvssVector.getName(), visible);
    }

    public UniversalCvssCalculatorEntry addVector(CvssVector cvssVector) {
        return addVector(cvssVector, cvssVector.getName(), true);
    }

    public void addOpenSection(String section) {
        openSections.add(section);
    }

    public void addOpenSections(String... section) {
        for (String s : section) {
            addOpenSection(s);
        }
    }

    public void addOpenSections(Collection<String> sections) {
        openSections.addAll(sections);
    }

    public void setSelectedVector(CvssVector selectedVector) {
        if (findVectorEntryByVector(selectedVector) == null) {
            throw new IllegalArgumentException("Selected vector must be added to the link generator first.");
        }
        this.selectedVector = selectedVector;
    }

    public void setSelectedVector(String selectedVector) {
        final UniversalCvssCalculatorEntry foundVector = findVectorEntryByName(selectedVector);
        if (foundVector == null) {
            throw new IllegalArgumentException("Selected vector must be added to the link generator first.");
        }
        this.selectedVector = foundVector.getCvssVector();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void addCve(String cve) {
        cves.add(cve);
    }

    public boolean isEmpty() {
        return entries.isEmpty() && cves.isEmpty();
    }

    protected UniversalCvssCalculatorEntry findVectorEntryByVector(CvssVector searchVector) {
        if (searchVector == null) {
            return null;
        }
        for (UniversalCvssCalculatorEntry entry : entries) {
            if (entry.getCvssVector().equals(searchVector)) {
                return entry;
            }
        }
        return null;
    }

    protected UniversalCvssCalculatorEntry findVectorEntryByName(String searchVectorName) {
        if (searchVectorName == null) {
            return null;
        }
        for (UniversalCvssCalculatorEntry entry : entries) {
            if (entry.getName().equals(searchVectorName)) {
                return entry;
            }
        }
        return null;
    }

    public String generateLink() {
        final StringBuilder linkBuilder = new StringBuilder();
        linkBuilder.append(baseUrl);

        final Map<String, String> parameters = new LinkedHashMap<>();

        if (!entries.isEmpty()) {
            final JSONArray vectorArray = new JSONArray();
            for (UniversalCvssCalculatorEntry entry : entries) {
                final CvssVector cvssVector = entry.getCvssVector();

                final JSONArray vectorEntry = new JSONArray();
                vectorEntry.put(entry.getName());
                vectorEntry.put(entry.isVisible());
                vectorEntry.put(cvssVector.toString());
                vectorEntry.put(cvssVector.getName());

                vectorArray.put(vectorEntry);
            }
            parameters.put("vector", vectorArray.toString());
        }

        if (!openSections.isEmpty()) {
            parameters.put("open", String.join(",", openSections));
        }

        if (selectedVector != null) {
            final UniversalCvssCalculatorEntry selectedVectorEntry = findVectorEntryByVector(selectedVector);
            parameters.put("selected", selectedVectorEntry.getName());
        }

        if (!cves.isEmpty()) {
            parameters.put("cve", String.join(",", cves));
        }

        if (!parameters.isEmpty()) {
            linkBuilder.append("?");
            final List<String> parameterStrings = new ArrayList<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                try {
                    final String urlEncoded = URLEncoder.encode(entry.getValue(), "UTF-8");
                    parameterStrings.add(entry.getKey() + "=" + urlEncoded);
                } catch (UnsupportedEncodingException e) {
                    LOG.error("Failed encode parameter value, resuming with default platform encoding: {}", entry.getValue(), e);
                    final String urlEncoded = URLEncoder.encode(entry.getValue());
                    parameterStrings.add(entry.getKey() + "=" + urlEncoded);
                }
            }
            linkBuilder.append(String.join("&", parameterStrings));
        }

        return linkBuilder.toString();
    }

    @Override
    public String toString() {
        return generateLink();
    }

    public static class UniversalCvssCalculatorEntry {
        private final CvssVector cvssVector;
        private String name;
        private boolean visible;

        public UniversalCvssCalculatorEntry(CvssVector cvssVector, String name, boolean visible) {
            this.cvssVector = cvssVector;
            this.name = name;
            this.visible = visible;
        }

        public String getName() {
            return name;
        }

        public UniversalCvssCalculatorEntry setName(String name) {
            this.name = name;
            return this;
        }

        public CvssVector getCvssVector() {
            return cvssVector;
        }

        public boolean isVisible() {
            return visible;
        }

        public UniversalCvssCalculatorEntry setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }
    }
}
