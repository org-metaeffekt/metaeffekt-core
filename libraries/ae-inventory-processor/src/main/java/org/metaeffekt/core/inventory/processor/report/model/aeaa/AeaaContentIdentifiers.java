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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.ContentIdentifiers</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public enum AeaaContentIdentifiers {
    CERT_FR("CERT-FR", Pattern.compile("((?:CERTFR|CERTA)-\\d+-(?:ACT|AVI|ALE|INF)-\\d+(?:-\\d+)?)", Pattern.CASE_INSENSITIVE), AeaaCertFrAdvisorEntry.class, AeaaCertFrAdvisorEntry::new),
    CERT_SEI("CERT-SEI", Pattern.compile("(VU#(\\d+))", Pattern.CASE_INSENSITIVE), AeaaCertSeiAdvisorEntry.class, AeaaCertSeiAdvisorEntry::new),
    MSRC("MSRC", Pattern.compile("(MSRC-(?:CVE|CAN)-([0-9]{4})-([0-9]{4,})|ADV(\\d+))", Pattern.CASE_INSENSITIVE), AeaaMsrcAdvisorEntry.class, AeaaMsrcAdvisorEntry::new),
    /**
     * <a href="https://github.com/github/advisory-database">
     * Pattern source</a>.
     */
    GHSA("GHSA", Pattern.compile("GHSA(-[23456789cfghjmpqrvwx]{4}){3}"), AeaaGhsaAdvisorEntry.class, AeaaGhsaAdvisorEntry::new),
    CVE("CVE", Pattern.compile("((?:CVE|CAN)-([0-9]{4})-([0-9]{4,}))", Pattern.CASE_INSENSITIVE), null, null),
    CWE("CWE", Pattern.compile("(CWE-\\d+)", Pattern.CASE_INSENSITIVE), null, null),
    CPE("CPE", Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE), null, null),
    NVD("NVD", Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE), null, null),
    ASSESSMENT_STATUS("Assessment Status", Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE), null, null),
    UNKNOWN("UNKNOWN", Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE), null, null);

    private static final Logger LOG = LoggerFactory.getLogger(AeaaContentIdentifiers.class);

    private final String wellFormedName;
    private final Pattern pattern;
    private final boolean isAdvisorProvider;

    private final Class<? extends AeaaAdvisoryEntry> advisoryEntryClass;
    private final Supplier<AeaaAdvisoryEntry> advisoryEntryFactory;

    AeaaContentIdentifiers(String wellFormedName, Pattern pattern, Class<? extends AeaaAdvisoryEntry> advisoryEntryClass, Supplier<AeaaAdvisoryEntry> advisoryEntryFactory) {
        this.wellFormedName = wellFormedName;
        this.pattern = pattern;

        this.isAdvisorProvider = advisoryEntryClass != null;

        this.advisoryEntryClass = advisoryEntryClass;
        this.advisoryEntryFactory = advisoryEntryFactory;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean isAdvisoryProvider() {
        return isAdvisorProvider;
    }

    public String normalizeEntryIdentifier(String id) {
        if (id == null) {
            return null;
        }

        switch (this) {
            case CERT_FR:
                id = baseNormalizeIdentifier(id);
                if (id.matches("\\d+.+")) {
                    id = id.replaceAll("\\d+(.+)", "$1");
                }
                if (id.contains("FR")) {
                    id = id.replaceAll("CERT[-–_ ]*FR-?", "");
                    id = "CERTFR-" + id;
                } else if (id.contains("CERTA")) {
                    id = id.replaceAll("CERTA-?", "");
                    id = "CERTA-" + id;
                }
                return id.replaceAll("-+", "-");

            case CERT_SEI:
                id = baseNormalizeIdentifier(id);
                return id.replaceAll("\\D*(\\d+)\\D*", "VU#$1");

            case CVE:
                id = baseNormalizeIdentifier(id);
                id = id.replaceAll("(CVE|CAN)-?", "")
                        .replaceAll(".*?(\\d{1,4})-(\\d+).*", "$1-$2");
                id = "CVE-" + id;
                return id.replaceAll("-+", "-");

            case MSRC:
                id = baseNormalizeIdentifier(id);
                final boolean isAdv = id.contains("ADV");
                id = id.replaceAll("(MSRC-)?(CVE|CAN)-?", "")
                        .replaceAll(".*?(\\d{1,4})-(\\d+).*", "$1-$2")
                        .replace("ADV", "");
                id = isAdv ? "ADV" + id : "MSRC-CVE-" + id;
                return id.replaceAll("-+", "-");

            case GHSA:
                id = baseNormalizeIdentifier(id);
                return id.replaceAll("-+", "-").toLowerCase();

            case CWE:
                id = baseNormalizeIdentifier(id);
                return id.replaceAll("CWE-?", "CWE-");

            default:
                return id;
        }
    }

    public String getWellFormedName() {
        return wellFormedName;
    }

    public boolean matches(String id) {
        if (id == null) {
            return false;
        }

        return pattern.matcher(id).matches();
    }

    public boolean matchesName(String name) {
        if (name == null) {
            return false;
        }

        return normalize(this.name()).equals(normalize(name));
    }

    public Class<? extends AeaaAdvisoryEntry> getAdvisoryEntryClass() {
        return advisoryEntryClass;
    }

    public Supplier<AeaaAdvisoryEntry> getAdvisoryEntryFactory() {
        return advisoryEntryFactory;
    }

    public void assertAdvisoryEntryClass(Supplier<String> hint) {
        if (!isAdvisorProvider) {
            throw new IllegalStateException("Content identifier [" + this + "] is not an advisory provider on: " + hint.get());
        }
    }

    public static AeaaContentIdentifiers extractSourceFromJson(JSONObject json) {
        final String source = json.optString("source", "unknown");
        final AeaaContentIdentifiers parsedSource = AeaaContentIdentifiers.fromName(source);
        return parsedSource == null ? UNKNOWN : parsedSource;
    }

    public static AeaaContentIdentifiers extractSourceFromAdvisor(AdvisoryMetaData amd) {
        final String source = amd.get(AdvisoryMetaData.Attribute.SOURCE);
        final AeaaContentIdentifiers parsedSource = AeaaContentIdentifiers.fromName(source);
        return parsedSource == null ? UNKNOWN : parsedSource;
    }

    private static String normalize(String id) {
        return id.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private static String baseNormalizeIdentifier(String id) {
        return id.replaceAll("[-–_ ]+", "-")
                .toUpperCase();
    }

    public List<String> fromFreeText(String freeText) {
        if (freeText == null) {
            return new ArrayList<>();
        }

        final List<String> result = new ArrayList<>();

        final Matcher matcher = this.pattern.matcher(freeText);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }

        return result;
    }

    public static AeaaContentIdentifiers fromName(String name) {
        final String[] names = name.toLowerCase().split(", ");

        for (String n : names) {
            for (AeaaContentIdentifiers id : AeaaContentIdentifiers.values()) {
                if (id.matchesName(n)) {
                    return id;
                }
            }
        }

        for (String n : names) {
            for (AeaaContentIdentifiers id : AeaaContentIdentifiers.values()) {
                if (id.getWellFormedName().equalsIgnoreCase(n)) {
                    return id;
                }
            }
        }

        LOG.debug("Failed to parse content identifier name [{}], returning {}", name, UNKNOWN);

        return UNKNOWN;
    }

    public static List<AeaaContentIdentifiers> fromNames(String name) {
        if (StringUtils.isEmpty(name)) return new ArrayList<>();

        final String[] names = name.toLowerCase().split(", ");
        final List<String> listNames = Arrays.asList(names);

        if (CentralSecurityPolicyConfiguration.containsAny(listNames)) {
            return Arrays.stream(AeaaContentIdentifiers.values())
                    .filter(id -> !UNKNOWN.equals(id))
                    .collect(Collectors.toList());
        }

        return Arrays.stream(names)
                .map(AeaaContentIdentifiers::fromName)
                .distinct()
                .filter(id -> !UNKNOWN.equals(id))
                .collect(Collectors.toList());
    }

    public static AeaaContentIdentifiers fromEntryIdentifier(String identifier) {
        if (identifier == null) {
            return UNKNOWN;
        }

        final String normalizedIdentifier = baseNormalizeIdentifier(identifier);

        for (AeaaContentIdentifiers candidate : AeaaContentIdentifiers.values()) {
            if (candidate.matches(normalizedIdentifier)) {
                return candidate;
            }
        }

        if (normalizedIdentifier.contains("CVE")) {
            return CVE;
        }

        if (normalizedIdentifier.contains("CPE")) {
            return CPE;
        }

        if (normalizedIdentifier.contains("MSRC") || normalizedIdentifier.contains("ADV")) {
            return MSRC;
        }

        if (normalizedIdentifier.contains("CERTFR") || normalizedIdentifier.contains("CERTA")) {
            return CERT_FR;
        }

        if (normalizedIdentifier.contains("VU") || normalizedIdentifier.contains("CERTSEI") || normalizedIdentifier.contains("CERT-SEI")) {
            return CERT_SEI;
        }

        if (normalizedIdentifier.contains("GHSA")) {
            return GHSA;
        }

        if (normalizedIdentifier.contains("CWE")) {
            return CWE;
        }

        if (normalizedIdentifier.matches("\\D*\\d+-\\D+-\\d+")) {
            return CERT_FR;
        }

        if (normalizedIdentifier.matches("\\D*\\d{5,7}")) {
            return CERT_SEI;
        }

        if (normalizedIdentifier.matches("\\D*\\d+-\\d+")) {
            return CERT_FR;
        }

        return UNKNOWN;
    }
}
