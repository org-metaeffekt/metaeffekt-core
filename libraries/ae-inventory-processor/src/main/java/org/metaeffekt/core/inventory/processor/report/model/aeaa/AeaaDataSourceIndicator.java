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

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.base.DataSourceIndicator</code> 
 * until separation of inventory report generation from ae core inventory processor.
 * <p>
 * A data structure to represent the matching source of a vulnerability or advisory.
 */
public class AeaaDataSourceIndicator {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaDataSourceIndicator.class);

    private final AeaaContentIdentifierStore.AeaaContentIdentifier dataSource;
    private final Reason matchReason;

    public AeaaDataSourceIndicator(AeaaContentIdentifierStore.AeaaContentIdentifier dataSource, Reason matchReason) {
        this.dataSource = dataSource;
        this.matchReason = matchReason;
    }

    public Reason getMatchReason() {
        return matchReason;
    }

    public AeaaContentIdentifierStore.AeaaContentIdentifier getDataSource() {
        return dataSource;
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("source", dataSource.name())
                .put("implementation", dataSource.getImplementation())
                .put("matches", matchReason.toJson());
    }

    public static AeaaDataSourceIndicator fromJson(JSONObject json) {
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
                AeaaDataSourceIndicator fromJson = fromJson(jsonObject);
                result.add(fromJson);
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
        return new JSONArray(
                indicators.stream()
                        .filter(Objects::nonNull)
                        .map(AeaaDataSourceIndicator::toJson)
                        .collect(Collectors.toList())
        );
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

    public static class AssessmentStatusReason extends Reason {
        public final static String TYPE = "assessment-status";

        private final String originFile;

        public AssessmentStatusReason(String originFile) {
            super(TYPE);
            this.originFile = originFile;
        }

        public String getOriginFile() {
            return originFile;
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

    public static class VulnerabilityReason extends Reason {
        public final static String TYPE = "vulnerability";

        private final String id;

        public VulnerabilityReason(String id) {
            super(TYPE);
            this.id = id;
        }

        public VulnerabilityReason(AeaaVulnerability vulnerability) {
            super(TYPE);
            this.id = vulnerability.getId();
        }

        public String getId() {
            return id;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("id", id);
        }
    }

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

        public String getCoordinates() {
            return coordinates;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("coordinates", coordinates);
        }
    }

    public static class ArtifactOsvReason extends ArtifactReason {
        public final static String TYPE = "artifact-osv";

        private final String coordinates;

        public ArtifactOsvReason(Artifact artifact, String coordinates) {
            super(TYPE, artifact);
            this.coordinates = coordinates;
        }

        public String getCoordinates() {
            return coordinates;
        }

        protected ArtifactOsvReason(JSONObject artifactData, String coordinates) {
            super(TYPE, artifactData);
            this.coordinates = coordinates;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("coordinates", coordinates);
        }
    }

    public static class ArtifactCpeReason extends ArtifactReason {
        public final static String TYPE = "artifact-cpe";

        private final String cpe;
        private final String configuration;

        public ArtifactCpeReason(Artifact artifact, String cpe) {
            super(TYPE, artifact);
            this.cpe = cpe;
            this.configuration = null;
        }

        protected ArtifactCpeReason(JSONObject artifactData, String cpe) {
            super(TYPE, artifactData);
            this.cpe = cpe;
            this.configuration = null;
        }

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

        public String getCpe() {
            return cpe;
        }

        public String getConfiguration() {
            return configuration;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("cpe", cpe)
                    .put("configuration", configuration);
        }
    }

    public static class MsrcProductReason extends ArtifactReason {
        public final static String TYPE = "msrc-product";

        private final String msrcProductId;
        private final String[] kbIds;

        public MsrcProductReason(Artifact artifact, String msrcProductId, String[] kbIds) {
            super(TYPE, artifact);
            this.msrcProductId = msrcProductId;
            this.kbIds = kbIds;
        }

        protected MsrcProductReason(JSONObject artifactData, String msrcProductId, String[] kbIds) {
            super(TYPE, artifactData);
            this.msrcProductId = msrcProductId;
            this.kbIds = kbIds;
        }

        public String getMsrcProductId() {
            return msrcProductId;
        }

        public String[] getKbIds() {
            return kbIds;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("msrcProductId", msrcProductId)
                    .put("kbIds", kbIds);
        }
    }

    public static class AnyReason extends Reason {
        public final static String TYPE = "any";

        private final String description;

        public AnyReason(String description) {
            super(TYPE);
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public JSONObject toJson() {
            return super.toJson()
                    .put("description", description);
        }
    }

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

        @Override
        public String overwriteSource() {
            return source;
        }

        public String getSource() {
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

        @Override
        public JSONObject toJson() {
            return super.toJson();
        }
    }

    public abstract static class ArtifactReason extends Reason {
        protected final Artifact artifact;

        protected final String artifactId;
        protected final String artifactComponent;
        protected final String artifactVersion;

        protected ArtifactReason(String type, Artifact artifact) {
            super(type);
            if (artifact == null) {
                LOG.warn("Artifact is null in [{}#ArtifactReason(String, Artifact)], using empty artifact", AeaaDataSourceIndicator.class);
                artifact = new Artifact();
            }
            this.artifact = artifact;
            this.artifactId = artifact.getId();
            this.artifactComponent = artifact.getComponent();
            this.artifactVersion = artifact.getVersion();
        }

        protected ArtifactReason(String type, JSONObject artifactData) {
            super(type);
            this.artifact = null;
            this.artifactId = artifactData.optString("artifactId", null);
            this.artifactComponent = artifactData.optString("artifactComponent", null);
            this.artifactVersion = artifactData.optString("artifactVersion", null);
        }

        public Artifact getArtifact() {
            return artifact;
        }

        public boolean hasArtifact() {
            return artifact != null;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getArtifactComponent() {
            return artifactComponent;
        }

        public String getArtifactVersion() {
            return artifactVersion;
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

    public abstract static class Reason {
        protected final String type;

        protected Reason(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
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
            if (!json.has("type")) throw new IllegalArgumentException("Missing type attribute in reason JSON: " + json);
            final String type = json.getString("type");
            switch (type) {
                case VulnerabilityReason.TYPE:
                    return new VulnerabilityReason(json.getString("id"));
                case ArtifactCpeReason.TYPE:
                    return new ArtifactCpeReason(
                            json,
                            json.optString("cpe", null),
                            json.optString("configuration", null)
                    );
                case MsrcProductReason.TYPE:
                    return new MsrcProductReason(
                            json,
                            json.optString("msrcProductId", null),
                            json.getJSONArray("kbIds").toList().stream().map(Object::toString).toArray(String[]::new)
                    );
                case ArtifactGhsaReason.TYPE:
                    return new ArtifactGhsaReason(
                            json,
                            json.optString("coordinates", null)
                    );
                case ArtifactOsvReason.TYPE:
                    return new ArtifactOsvReason(
                            json,
                            json.optString("coordinates", null)
                    );
                case AnyReason.TYPE:
                    return new AnyReason(json.optString("description", null));
                case AnyArtifactReason.TYPE:
                    return new AnyArtifactReason(json);
                case AssessmentStatusReason.TYPE:
                    return new AssessmentStatusReason(json.optString("originFile", null));
                case AnyArtifactOverwriteSourceReason.TYPE:
                    return new AnyArtifactOverwriteSourceReason(json);
                default:
                    throw new IllegalArgumentException("Unknown reason type: " + type + "\nIn reason JSON:" + json);
            }
        }
    }
}
