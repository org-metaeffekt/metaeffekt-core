/*
 * Copyright 2009-2026 the original author or authors.
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

import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.csaf.AeaaCsafEntryVulnerabilityStatus;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A data structure to represent the matching source of a vulnerability or advisory.
 */
@Getter
public class AeaaDataSourceIndicator {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaDataSourceIndicator.class);

    private final AeaaContentIdentifierStore.AeaaContentIdentifier dataSource;
    private final Reason matchReason;

    public AeaaDataSourceIndicator(AeaaContentIdentifierStore.AeaaContentIdentifier dataSource, Reason matchReason) {
        this.dataSource = dataSource;
        this.matchReason = matchReason;
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("source", dataSource.name())
                .put("implementation", dataSource.getImplementation())
                .put("matches", matchReason.toJson());
    }

    public static AeaaDataSourceIndicator fromJson(JSONObject json) {
        if (!json.has("source")) {
            throw new IllegalArgumentException("Missing source attribute in reason JSON: " + json);
        }
        final String source = json.getString("source");
        final String implementation = json.optString("implementation", null);
        final AeaaContentIdentifierStore.AeaaContentIdentifier parsedSource = sourceFromSourceAndImplementation(source, implementation);

        return new AeaaDataSourceIndicator(
                parsedSource,
                Reason.fromJson(json.getJSONObject("matches"))
        );
    }

    public static List<AeaaDataSourceIndicator> fromJson(JSONArray json) {
        final List<AeaaDataSourceIndicator> result = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            final Object o = json.get(i);
            if (o instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) o;
                result.add(fromJson(jsonObject));
            } else {
                LOG.warn("Unexpected JSON object in array on [{}#fromJson(JSONArray)]: {}", AeaaDataSourceIndicator.class, o);
            }
        }
        return result;
    }

    public static JSONArray toJson(Collection<AeaaDataSourceIndicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return new JSONArray();
        }
        try {
            return new JSONArray(
                    indicators.stream()
                            .filter(Objects::nonNull)
                            .map(AeaaDataSourceIndicator::toJson)
                            .collect(Collectors.toList())
            );
        } catch (Exception e) {
            LOG.error("Failed to convert indicators to JSON: {}", indicators, e);
            return new JSONArray();
        }
    }

    @Override
    public String toString() {
        return "DataSourceIndicator[" + dataSource + " --> " + (matchReason == null ? "unspecified" : matchReason.toJson()) + "]";
    }

    private static AeaaContentIdentifierStore.AeaaContentIdentifier sourceFromSourceAndImplementation(String source, String implementation) {
        final AeaaAdvisoryTypeIdentifier<?> advisoryTypeIdentifier = AeaaAdvisoryTypeStore.get().fromNameAndImplementationWithoutCreation(source, implementation);
        if (advisoryTypeIdentifier != null) {
            return advisoryTypeIdentifier;
        }

        final AeaaVulnerabilityTypeIdentifier<?> vulnerabilityTypeIdentifier = AeaaVulnerabilityTypeStore.get().fromNameAndImplementationWithoutCreation(source, implementation);
        if (vulnerabilityTypeIdentifier != null) {
            return vulnerabilityTypeIdentifier;
        }

        final AeaaOtherTypeIdentifier otherTypeIdentifier = AeaaOtherTypeStore.get().fromNameAndImplementationWithoutCreation(source, implementation);
        if (otherTypeIdentifier != null) {
            return otherTypeIdentifier;
        }

        return AeaaAdvisoryTypeStore.get().fromNameAndImplementationWithoutCreation(source, implementation);
    }

    @Getter
    public abstract static class Reason {
        protected final String type;

        private static final Map<String, Function<JSONObject, Reason>> registry = new HashMap<>();

        protected Reason(String type) {
            this.type = type;
        }

        public JSONObject toJson() {
            return new JSONObject().put("type", type);
        }

        public String overwriteSource() {
            return null;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        public static Reason fromJson(JSONObject json) {
            if (!json.has("type")) {
                throw new IllegalArgumentException("Missing type attribute in reason JSON: " + json);
            }
            final String type = json.getString("type");
            Function<JSONObject, Reason> factory = registry.get(type);
            if (factory == null) {
                throw new IllegalArgumentException("Unknown reason type: " + type + "\nIn reason JSON:" + json);
            }
            return factory.apply(json);
        }
    }

    static {
        Reason.registry.put(VulnerabilityReason.TYPE, VulnerabilityReason::fromJson);
        Reason.registry.put(ArtifactCpeReason.TYPE, ArtifactCpeReason::fromJson);
        Reason.registry.put(MsrcProductReason.TYPE, MsrcProductReason::fromJson);
        Reason.registry.put(ArtifactGhsaReason.TYPE, ArtifactGhsaReason::fromJson);
        Reason.registry.put(ArtifactOsvReason.TYPE, ArtifactOsvReason::fromJson);
        Reason.registry.put(ArtifactCsafReason.TYPE, ArtifactCsafReason::fromJson);
        Reason.registry.put(AnyReason.TYPE, AnyReason::fromJson);
        Reason.registry.put(AnyArtifactReason.TYPE, AnyArtifactReason::fromJson);
        Reason.registry.put(AssessmentStatusReason.TYPE, AssessmentStatusReason::fromJson);
        Reason.registry.put(AnyArtifactOverwriteSourceReason.TYPE, AnyArtifactOverwriteSourceReason::fromJson);
    }

    @Getter
    public abstract static class ArtifactReason extends Reason {
        protected final Artifact artifact;

        protected final String artifactId;
        protected final String artifactComponent;
        protected final String artifactVersion;

        protected ArtifactReason(String type, Artifact artifact) {
            super(type);

            this.artifact = artifact == null ? new Artifact() : artifact;
            this.artifactId = this.artifact.getId();
            this.artifactComponent = this.artifact.getComponent();
            this.artifactVersion = this.artifact.getVersion();
        }

        protected ArtifactReason(String type, JSONObject artifactData) {
            super(type);

            this.artifact = null;
            this.artifactId = artifactData.optString("artifactId", null);
            this.artifactComponent = artifactData.optString("artifactComponent", null);
            this.artifactVersion = artifactData.optString("artifactVersion", null);
        }

        public boolean hasArtifact() {
            return artifact != null;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("artifactId", artifactId)
                    .put("artifactComponent", artifactComponent)
                    .put("artifactVersion", artifactVersion);
        }

        public Artifact findArtifact(Set<Artifact> artifacts) {
            if (artifact != null) {
                return artifact;
            }
            return artifacts.stream()
                    .filter(this::isArtifact)
                    .findFirst()
                    .orElse(null);
        }

        public boolean isArtifact(Artifact artifact) {
            if (artifact == null) {
                return false;
            }
            if (this.artifact != null) {
                return this.artifact.equals(artifact);
            }
            return Objects.equals(artifactId, artifact.getId()) &&
                    Objects.equals(artifactComponent, artifact.getComponent()) &&
                    Objects.equals(artifactVersion, artifact.getVersion());
        }
    }

    @Getter
    public static class AssessmentStatusReason extends Reason {
        public final static String TYPE = "assessment-status";

        private final String originFile;

        public AssessmentStatusReason(String originFile) {
            super(TYPE);
            this.originFile = originFile;
        }

        public static AssessmentStatusReason fromJson(JSONObject json) {
            return new AssessmentStatusReason(json.optString("originFile", null));
        }

        public String getOriginFileName() {
            if (originFile == null || originFile.isEmpty() || originFile.equals("no-file")) {
                return "no-file";
            }
            return new File(originFile).getName();
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("originFile", originFile);
        }
    }

    @Getter
    public static class VulnerabilityReason extends Reason {
        public final static String TYPE = "vulnerability";

        private final String id;

        public VulnerabilityReason(String id) {
            super(TYPE);
            this.id = id;
        }

        public VulnerabilityReason(AeaaVulnerability vulnerability) {
            this(vulnerability.getId());
        }

        public static VulnerabilityReason fromJson(JSONObject json) {
            return new VulnerabilityReason(json.getString("id"));
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("id", id);
        }
    }

    @Getter
    public static class ArtifactGhsaReason extends ArtifactReason {
        public final static String TYPE = "artifact-ghsa";

        private final String coordinates;

        public ArtifactGhsaReason(Artifact artifact, String coordinates) {
            super(TYPE, artifact);
            this.coordinates = coordinates;
        }

        protected ArtifactGhsaReason(JSONObject artifactData, String coordinates) {
            super(TYPE, artifactData);
            this.coordinates = coordinates;
        }

        public static ArtifactGhsaReason fromJson(JSONObject json) {
            return new ArtifactGhsaReason(json, json.optString("coordinates", null));
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("coordinates", coordinates);
        }
    }

    @Getter
    public static class ArtifactCsafReason extends ArtifactReason {
        public final static String TYPE = "artifact-csaf";

        private final String provider;
        private final String documentId;

        private final Map<AeaaCsafEntryVulnerabilityStatus, Set<String>> helpers;

        public ArtifactCsafReason(Artifact artifact, AeaaAdvisoryTypeIdentifier<?> advisoryTypeIdentifier, String document, Map<AeaaCsafEntryVulnerabilityStatus, Set<String>> helpers) {
            super(TYPE, artifact);

            this.provider = advisoryTypeIdentifier.getWellFormedName();
            this.documentId = document;
            this.helpers = helpers;
        }

        protected ArtifactCsafReason(JSONObject artifactData, String provider, String document, Map<AeaaCsafEntryVulnerabilityStatus, Set<String>> helpers) {
            super(TYPE, artifactData);

            this.provider = provider;
            this.documentId = document;
            this.helpers = helpers;
        }

        public static ArtifactCsafReason fromJson(JSONObject json) {
            final JSONObject helpersJson = json.getJSONObject("usedHelpers");
            final Map<AeaaCsafEntryVulnerabilityStatus, Set<String>> helperStatusMapping = new HashMap<>();
            for (String key : helpersJson.keySet()) {
                final JSONArray helperJson = helpersJson.getJSONArray(key);
                final Set<String> helpers = new HashSet<>();
                for (int i = 0; i < helperJson.length(); i++) {
                    final JSONObject jsonObject = helperJson.getJSONObject(i);
                    helpers.add(jsonObject.toString());
                }
                helperStatusMapping.put(AeaaCsafEntryVulnerabilityStatus.fromString(key), helpers);
            }
            return new ArtifactCsafReason(json, json.getString("provider"), json.getString("document"), helperStatusMapping);
        }

        @Override
        public JSONObject toJson() {
            JSONObject statusMappings = new JSONObject();
            for (Map.Entry<AeaaCsafEntryVulnerabilityStatus, Set<String>> entry : helpers.entrySet()) {
                statusMappings.put(entry.getKey().getDefaultValue(), new ArrayList<>(entry.getValue()));
            }
            return super.toJson()
                    .put("provider", provider)
                    .put("document", documentId)
                    .put("usedHelpers", statusMappings);
        }
    }

    @Getter
    public static class ArtifactOsvReason extends ArtifactReason {
        public final static String TYPE = "artifact-osv";

        private final String purl;
        private final String coordinates;
        private final String matchedEntry;

        public ArtifactOsvReason(Artifact artifact, String purl, String coordinates, String matchedEntry) {
            super(TYPE, artifact);
            this.purl = purl;
            this.coordinates = coordinates;
            this.matchedEntry = matchedEntry;
        }

        public ArtifactOsvReason(JSONObject artifactData, String purl, String coordinates, String matchedEntry) {
            super(TYPE, artifactData);
            this.purl = purl;
            this.coordinates = coordinates;
            this.matchedEntry = matchedEntry;
        }

        public static ArtifactOsvReason fromJson(JSONObject json) {
            return new ArtifactOsvReason(json, json.optString("purl", null), json.optString("coordinates", null), json.optString("matchedEntry", null));
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("purl", purl)
                    .put("matchedEntry", matchedEntry)
                    .put("coordinates", coordinates);
        }
    }

    @Getter
    public static class ArtifactCpeReason extends ArtifactReason {
        public final static String TYPE = "artifact-cpe";

        private final String cpe;
        private final String configuration;

        public ArtifactCpeReason(Artifact artifact, String cpe, String configuration) {
            super(TYPE, artifact);
            this.cpe = cpe;
            this.configuration = configuration;
        }

        protected ArtifactCpeReason(JSONObject artifactData, String cpe, String configuration) {
            super(TYPE, artifactData);
            this.cpe = cpe;
            this.configuration = configuration;
        }

        public static ArtifactCpeReason fromJson(JSONObject json) {
            return new ArtifactCpeReason(json, json.optString("cpe", null), json.optString("configuration", null));
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("cpe", cpe)
                    .put("configuration", configuration);
        }
    }

    @Getter
    public static class MsrcProductReason extends ArtifactReason {
        public final static String TYPE = "msrc-product";

        private final String msrcProductId;
        private final String[] kbIds;

        public MsrcProductReason(Artifact artifact, String msrcProductId, String... kbIds) {
            super(TYPE, artifact);
            this.msrcProductId = msrcProductId;
            this.kbIds = kbIds;
        }

        protected MsrcProductReason(JSONObject artifactData, String msrcProductId, String[] kbIds) {
            super(TYPE, artifactData);
            this.msrcProductId = msrcProductId;
            this.kbIds = kbIds;
        }

        public static MsrcProductReason fromJson(JSONObject json) {
            String[] kbIds = json.getJSONArray("kbIds").toList().stream().map(Object::toString).toArray(String[]::new);
            return new MsrcProductReason(json, json.optString("msrcProductId", null), kbIds);
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("msrcProductId", msrcProductId)
                    .put("kbIds", new JSONArray(kbIds));
        }
    }

    @Getter
    public static class AnyReason extends Reason {
        public final static String TYPE = "any";

        private final String description;

        public AnyReason(String description) {
            super(TYPE);
            this.description = description;
        }

        public static AnyReason fromJson(JSONObject json) {
            return new AnyReason(json.optString("description", null));
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("description", description);
        }
    }

    @Getter
    public static class AnyArtifactOverwriteSourceReason extends ArtifactReason {
        public final static String TYPE = "any-artifact-overwrite-source";

        private final String source;

        public AnyArtifactOverwriteSourceReason(Artifact artifact, String source) {
            super(TYPE, artifact);
            this.source = source;
        }

        protected AnyArtifactOverwriteSourceReason(JSONObject artifactData) {
            super(TYPE, artifactData);
            this.source = artifactData.optString("source", null);
        }

        public static AnyArtifactOverwriteSourceReason fromJson(JSONObject json) {
            return new AnyArtifactOverwriteSourceReason(json);
        }

        @Override
        public String overwriteSource() {
            return source;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("source", source);
        }
    }

    public static class AnyArtifactReason extends ArtifactReason {
        public final static String TYPE = "any-artifact";

        public AnyArtifactReason(Artifact artifact) {
            super(TYPE, artifact);
        }

        protected AnyArtifactReason(JSONObject artifactData) {
            super(TYPE, artifactData);
        }

        public static AnyArtifactReason fromJson(JSONObject json) {
            return new AnyArtifactReason(json);
        }

        @Override
        public JSONObject toJson() {
            return super.toJson();
        }
    }
}