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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import org.apache.commons.lang.WordUtils;
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
public class AeaaContentIdentifiers {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaContentIdentifiers.class);

    public final static AeaaContentIdentifiers CERT_FR = new AeaaContentIdentifiers("CERT_FR", "CERT-FR", Pattern.compile("((?:CERTFR|CERTA)-\\d+-(?:ACT|AVI|ALE|INF)-\\d+(?:-\\d+)?)", Pattern.CASE_INSENSITIVE), AeaaCertFrAdvisorEntry.class, AeaaCertFrAdvisorEntry::new);
    public final static AeaaContentIdentifiers CERT_SEI = new AeaaContentIdentifiers("CERT_SEI", "CERT-SEI", Pattern.compile("(VU#(\\d+))", Pattern.CASE_INSENSITIVE), AeaaCertSeiAdvisorEntry.class, AeaaCertSeiAdvisorEntry::new);
    public final static AeaaContentIdentifiers CERT_EU = new AeaaContentIdentifiers("CERT_EU", "CERT-EU", Pattern.compile("(CERT-EU-(\\d+))", Pattern.CASE_INSENSITIVE), AeaaCertEuAdvisorEntry.class, AeaaCertEuAdvisorEntry::new);
    public final static AeaaContentIdentifiers MSRC = new AeaaContentIdentifiers("MSRC", "MSRC", Pattern.compile("(MSRC-(?:CVE|CAN)-([0-9]{4})-([0-9]{4,})|ADV(\\d+))", Pattern.CASE_INSENSITIVE), AeaaMsrcAdvisorEntry.class, AeaaMsrcAdvisorEntry::new);
    /**
     * <a href="https://github.com/github/advisory-database">Pattern source</a>.
     */
    public final static AeaaContentIdentifiers GHSA = new AeaaContentIdentifiers("GHSA", "GHSA", Pattern.compile("GHSA(-[23456789cfghjmpqrvwx]{4}){3}"), AeaaGhsaAdvisorEntry.class, AeaaGhsaAdvisorEntry::new);
    public final static AeaaContentIdentifiers CVE = new AeaaContentIdentifiers("CVE", "CVE", Pattern.compile("((?:CVE|CAN)-([0-9]{4})-([0-9]{4,}))", Pattern.CASE_INSENSITIVE), null, null);
    public final static AeaaContentIdentifiers CWE = new AeaaContentIdentifiers("CWE", "CWE", Pattern.compile("(CWE-\\d+)", Pattern.CASE_INSENSITIVE), null, null);
    public final static AeaaContentIdentifiers CPE = new AeaaContentIdentifiers("CPE", "CPE", Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE), null, null);
    public final static AeaaContentIdentifiers NVD = new AeaaContentIdentifiers("NVD", "NVD", Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE), null, null);
    public final static AeaaContentIdentifiers ASSESSMENT_STATUS = new AeaaContentIdentifiers("ASSESSMENT_STATUS", "Assessment Status", Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE), null, null);
    public final static AeaaContentIdentifiers UNKNOWN = new AeaaContentIdentifiers("UNKNOWN", "UNKNOWN", Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE), null, null);
    public final static AeaaContentIdentifiers UNKNOWN_ADVISORY = new AeaaContentIdentifiers("UNKNOWN_ADVISORY", "Unknown Advisory", Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE), AeaaGeneralAdvisorEntry.class, AeaaGeneralAdvisorEntry::new);

    public static final List<AeaaContentIdentifiers> REGISTERED_CONTENT_IDENTIFIERS = new ArrayList<>(Arrays.asList(
            CERT_FR, CERT_SEI, CERT_EU, MSRC, GHSA,
            NVD, CVE, CPE, CWE,
            ASSESSMENT_STATUS,
            UNKNOWN, UNKNOWN_ADVISORY
    ));

    /**
     * This class used to be an enum, but was converted to a class to allow for dynamic instantiation of new identifiers.
     * This method adds support for the old enum-like <code>value</code> method.
     *
     * @return a list of all known content identifiers.
     */
    public static List<AeaaContentIdentifiers> values() {
        return REGISTERED_CONTENT_IDENTIFIERS;
    }

    private final String name;
    private final String wellFormedName;
    private final Pattern pattern;
    private final boolean isAdvisorProvider;
    private final Class<? extends AeaaAdvisoryEntry> advisoryEntryClass;
    private final Supplier<AeaaAdvisoryEntry> advisoryEntryFactory;

    public AeaaContentIdentifiers(String name, String wellFormedName, Pattern pattern, Class<? extends AeaaAdvisoryEntry> advisoryEntryClass, Supplier<AeaaAdvisoryEntry> advisoryEntryFactory) {
        this.name = name;
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

    /**
     * This method is used to provide a similar API to the old enum-like <code>valueOf</code> method.
     *
     * @return the content identifier with the given name.
     */
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String normalizeEntryIdentifier(String id) {
        if (id == null) {
            return null;
        }

        if (this == CERT_FR) {
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
        } else if (this == CERT_SEI) {
            id = baseNormalizeIdentifier(id);
            return id.replaceAll("\\D*(\\d+)\\D*", "VU#$1");
        } else if (this == CVE) {
            id = baseNormalizeIdentifier(id);
            id = id.replaceAll("(CVE|CAN)-?", "")
                    .replaceAll(".*?(\\d{1,4})-(\\d+).*", "$1-$2");
            id = "CVE-" + id;
            return id.replaceAll("-+", "-");
        } else if (this == MSRC) {
            id = baseNormalizeIdentifier(id);
            final boolean isAdv = id.contains("ADV");
            id = id.replaceAll("(MSRC-)?(CVE|CAN)-?", "")
                    .replaceAll(".*?(\\d{1,4})-(\\d+).*", "$1-$2")
                    .replace("ADV", "");
            id = isAdv ? "ADV" + id : "MSRC-CVE-" + id;
            return id.replaceAll("-+", "-");
        } else if (this == GHSA) {
            id = baseNormalizeIdentifier(id);
            return id.replaceAll("-+", "-").toLowerCase();
        } else if (this == CWE) {
            id = baseNormalizeIdentifier(id);
            return id.replaceAll("CWE-?", "CWE-");
        }

        return id;
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

    public static AeaaContentIdentifiers findOrCreateGenericNamedAdvisory(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name must not be empty.");
        }

        final AeaaContentIdentifiers existingIdentifier = AeaaContentIdentifiers.fromName(name);
        if (existingIdentifier != UNKNOWN) {
            if (existingIdentifier.isAdvisoryProvider()) {
                return existingIdentifier;
            } else {
                LOG.error("Existing content identifier [{}#{}] is not an advisory provider, but was requested as such. Removing existing identifier.", existingIdentifier, existingIdentifier.hashCode());
            }
        }

        final String wellFormedName = createWellFormedNameFromBaseName(name);
        final AeaaContentIdentifiers createdIdentifier = new AeaaContentIdentifiers(name, wellFormedName, Pattern.compile("UNDEFINED"), AeaaGeneralAdvisorEntry.class, AeaaGeneralAdvisorEntry::new);
        AeaaContentIdentifiers.registerContentIdentifier(createdIdentifier);
        return createdIdentifier;
    }

    private static String createWellFormedNameFromBaseName(String name) {
        name = name.toLowerCase().replaceAll("[-_]", " ");
        if (name.startsWith("cert ")) {
            return name.replaceFirst("cert ", "CERT-").toUpperCase();
        } else {
            return WordUtils.capitalize(name);
        }
    }

    public static void registerContentIdentifier(AeaaContentIdentifiers contentIdentifier) {
        if (contentIdentifier == null) {
            throw new IllegalArgumentException("Content identifier must not be null.");
        }

        final AeaaContentIdentifiers found = AeaaContentIdentifiers.fromName(contentIdentifier.name());
        if (found != UNKNOWN) {
            if (found == contentIdentifier) {
                return;
            }
            throw new IllegalArgumentException("Content identifier [" + contentIdentifier + "] is already registered as [" + found + "].");
        }

        LOG.info("Registering content identifier [{}] with attributes [wellFormedName={}, pattern={}, advisoryClass={}].", contentIdentifier, contentIdentifier.getWellFormedName(), contentIdentifier.getPattern(), contentIdentifier.getAdvisoryEntryClass());
        REGISTERED_CONTENT_IDENTIFIERS.add(contentIdentifier);
    }

    public static AeaaContentIdentifiers extractAdvisorySourceFromSourceOrName(String name, String source) {
        final AeaaContentIdentifiers parsedSource;
        if (source != null) {
            parsedSource = AeaaContentIdentifiers.fromName(source);
        } else if (name != null) {
            LOG.warn("Security Advisory source attribute [{}] should not be empty when parsing advisory entry [{}], falling back to parsing from name",
                    AdvisoryMetaData.Attribute.SOURCE, name);
            parsedSource = AeaaContentIdentifiers.fromEntryIdentifier(name);
        } else {
            LOG.warn("Security Advisory source attribute [{}] and name attribute [{}] are empty, falling back to [{}]",
                    AdvisoryMetaData.Attribute.SOURCE, AdvisoryMetaData.Attribute.NAME, AeaaContentIdentifiers.UNKNOWN.name());
            parsedSource = UNKNOWN;
        }
        if (parsedSource == null || parsedSource == UNKNOWN) {
            if (StringUtils.isNotEmpty(source)) {
                final AeaaContentIdentifiers createdIdentifier = findOrCreateGenericNamedAdvisory(source);
                LOG.warn("Could not infer source from source [{}], created source [{}]", source, createdIdentifier);
                return createdIdentifier;
            } else {
                // LOG.warn("Could not infer or create source from source [{}], using [{}]", source, UNKNOWN_ADVISORY);
                return UNKNOWN_ADVISORY;
            }
        } else {
            return parsedSource;
        }
    }

    public static AeaaContentIdentifiers extractSourceFromJson(JSONObject json) {
        final String source = json.optString("source", "unknown");
        final AeaaContentIdentifiers parsedSource = AeaaContentIdentifiers.fromName(source);
        return parsedSource == null ? UNKNOWN : parsedSource;
    }

    public static AeaaContentIdentifiers extractSourceFromAdvisor(AeaaAdvisoryEntry advisory) {
        if (advisory == null) {
            return UNKNOWN;
        }

        for (AeaaContentIdentifiers sourceIdentifier : AeaaContentIdentifiers.values()) {
            if (sourceIdentifier.isAdvisoryProvider() && sourceIdentifier.getAdvisoryEntryClass() == advisory.getClass()) {
                return sourceIdentifier;
            }
        }

        final String source = advisory.getAdditionalAttribute(AdvisoryMetaData.Attribute.SOURCE);
        final String id = advisory.getId();

        final AeaaContentIdentifiers parsedSource = extractAdvisorySourceFromSourceOrName(id, source);

        if (parsedSource != AeaaContentIdentifiers.UNKNOWN) {
            return parsedSource;
        }

        if (!advisory.getDataSources().isEmpty()) {
            LOG.warn("Could not infer source of [{}] from id: {}", advisory.getClass().getSimpleName(), id);
            return advisory.getDataSources().iterator().next();
        } else if (source != null) {
            final AeaaContentIdentifiers createdIdentifier = findOrCreateGenericNamedAdvisory(source);
            LOG.warn("Could not infer source of [{}] from id [{}] and no data sources are set, created source [{}]", advisory.getClass().getSimpleName(), id, createdIdentifier);
            return createdIdentifier;
        } else {
            LOG.warn("Could not infer source of [{}] from id [{}] and no data sources are set, using [{}]", advisory.getClass().getSimpleName(), id, AeaaContentIdentifiers.UNKNOWN);
            return AeaaContentIdentifiers.UNKNOWN_ADVISORY;
        }
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
            return AeaaContentIdentifiers.values().stream()
                    .filter(id -> UNKNOWN != id)
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
