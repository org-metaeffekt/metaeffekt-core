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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.store;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class AeaaAdvisoryTypeStore extends AeaaContentIdentifierStore<AeaaAdvisoryTypeIdentifier<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(Inventory.class);

    public final static AeaaAdvisoryTypeIdentifier<AeaaCertFrAdvisorEntry> CERT_FR = new AeaaAdvisoryTypeIdentifier<>(
            "CERT_FR", "CERT-FR", "",
            Pattern.compile("((?:CERTFR|CERTA)-\\d+-(?:ACT|AVI|ALE|INF)-\\d+(?:-\\d+)?)", Pattern.CASE_INSENSITIVE),
            AeaaCertFrAdvisorEntry.class, AeaaCertFrAdvisorEntry::new);
    public final static AeaaAdvisoryTypeIdentifier<AeaaCertSeiAdvisorEntry> CERT_SEI = new AeaaAdvisoryTypeIdentifier<>(
            "CERT_SEI", "CERT-SEI", "",
            Pattern.compile("(VU#(\\d+))", Pattern.CASE_INSENSITIVE),
            AeaaCertSeiAdvisorEntry.class, AeaaCertSeiAdvisorEntry::new);
    public final static AeaaAdvisoryTypeIdentifier<AeaaCertEuAdvisorEntry> CERT_EU = new AeaaAdvisoryTypeIdentifier<>(
            "CERT_EU", "CERT-EU", "",
            Pattern.compile("(CERT-EU-(\\d+))", Pattern.CASE_INSENSITIVE),
            AeaaCertEuAdvisorEntry.class, AeaaCertEuAdvisorEntry::new);
    public final static AeaaAdvisoryTypeIdentifier<AeaaMsrcAdvisorEntry> MSRC = new AeaaAdvisoryTypeIdentifier<>(
            "MSRC", "MSRC", "",
            Pattern.compile("(MSRC-(?:CVE|CAN)-([0-9]{4})-([0-9]{4,})|ADV(\\d+))", Pattern.CASE_INSENSITIVE),
            AeaaMsrcAdvisorEntry.class, AeaaMsrcAdvisorEntry::new);
    /**
     * <a href="https://github.com/github/advisory-database">Pattern source</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaGhsaAdvisorEntry> GHSA = new AeaaAdvisoryTypeIdentifier<>(
            "GHSA", "GHSA", "",
            Pattern.compile("GHSA(-[23456789cfghjmpqrvwx]{4}){3}"),
            AeaaGhsaAdvisorEntry.class, AeaaGhsaAdvisorEntry::new);
    public final static AeaaAdvisoryTypeIdentifier<AeaaGeneralAdvisorEntry> ANY_ADVISORY_FILTER_WILDCARD = new AeaaAdvisoryTypeIdentifier<>(
            "any", "any", "any",
            Pattern.compile("any"),
            AeaaGeneralAdvisorEntry.class, AeaaGeneralAdvisorEntry::new);

    private final static AeaaAdvisoryTypeStore INSTANCE = new AeaaAdvisoryTypeStore();

    public static AeaaAdvisoryTypeStore get() {
        return INSTANCE;
    }

    protected AeaaAdvisoryTypeStore() {
    }

    @Override
    protected AeaaAdvisoryTypeIdentifier<?> createIdentifier(String name, String implementation) {
        if (implementation.equals("CSAF")) {
            throw new UnsupportedOperationException("CSAF is not yet supported, but will be in the future.");
        }

        if (name.equalsIgnoreCase("any") || implementation.equalsIgnoreCase("all")) {
            return AeaaAdvisoryTypeStore.ANY_ADVISORY_FILTER_WILDCARD;
        }

        return new AeaaAdvisoryTypeIdentifier<>(
                name, AeaaContentIdentifier.deriveWellFormedName(name),
                implementation,
                Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE),
                AeaaGeneralAdvisorEntry.class,
                () -> new AeaaGeneralAdvisorEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation(name, implementation)));
    }

    @Override
    protected Collection<AeaaAdvisoryTypeIdentifier<?>> createDefaultIdentifiers() {
        return Arrays.asList(
                CERT_FR, CERT_SEI, CERT_EU, MSRC, GHSA,
                ANY_ADVISORY_FILTER_WILDCARD
        );
    }

    @Override
    public AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>> fromJson(JSONObject json) {
        final AeaaSingleContentIdentifierParseResult<?> superResult = super.fromJson(json);

        if (superResult.getIdentifier() instanceof AeaaAdvisoryTypeIdentifier) {
            return (AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>>) superResult;
        } else {
            throw new IllegalArgumentException("The provided JSON object does not represent an advisory type identifier, which is an impossible scenario since this class can by definition only support advisory type identifiers.");
        }
    }

    @Override
    public AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>> fromMap(Map<String, Object> map) {
        final AeaaSingleContentIdentifierParseResult<?> superResult = super.fromMap(map);

        if (superResult.getIdentifier() instanceof AeaaAdvisoryTypeIdentifier) {
            return (AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>>) superResult;
        } else {
            throw new IllegalArgumentException("The provided map does not represent an advisory type identifier, which is an impossible scenario since this class can by definition only support advisory type identifiers.");
        }
    }

    public AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>> fromAdvisoryMetaData(AdvisoryMetaData amd) {
        final String source = ObjectUtils.firstNonNull(amd.get(AdvisoryMetaData.Attribute.SOURCE));
        final String implementation = ObjectUtils.firstNonNull(amd.get(AdvisoryMetaData.Attribute.SOURCE_IMPLEMENTATION));
        final String entryId = ObjectUtils.firstNonNull(amd.get(AdvisoryMetaData.Attribute.NAME));

        if (StringUtils.isEmpty(source)) {
            // this should never happen, but let's catch it anyway.
            // the only other option is to check if the name is somehow recognized by any of the patterns.
            final Optional<AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>>> result = fromEntryIdentifier(entryId);
            if (result.isPresent()) {
                return result.get();
            }

            throw new IllegalArgumentException("The advisory meta data does not contain a source attribute, which is required to determine the advisory type.");
        }

        final AeaaAdvisoryTypeIdentifier<?> advisoryTypeIdentifier = this.fromNameAndImplementation(source, implementation);
        return new AeaaSingleContentIdentifierParseResult<>(advisoryTypeIdentifier, entryId);
    }

    public static <T extends AeaaContentIdentifier> Optional<AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>>> fromEntryIdentifier(String entryId) {
        if (StringUtils.isEmpty(entryId)) {
            LOG.warn("The advisory does not contain a source or name attribute, which is required to determine the advisory type.");
            return Optional.empty();
        }

        for (AeaaAdvisoryTypeIdentifier<?> typeIdentifier : AeaaAdvisoryTypeStore.get().values()) {
            if (typeIdentifier.patternMatchesId(entryId)) {
                return Optional.of(new AeaaSingleContentIdentifierParseResult<>(typeIdentifier, entryId));
            }
        }

        return Optional.empty();
    }
}
