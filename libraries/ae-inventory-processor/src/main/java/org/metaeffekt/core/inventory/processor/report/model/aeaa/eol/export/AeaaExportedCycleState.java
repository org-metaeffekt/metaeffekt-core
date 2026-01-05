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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.export;

import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaInventoryAttribute;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.AeaaEolCycle;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.AeaaEolLifecycle;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.state.*;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.BiFunction;

@Getter
public class AeaaExportedCycleState {

    private final Artifact artifact;
    private final AeaaEolCycle cycle;

    private final String eol;
    private final AeaaEolState eolState;
    private final long eolMillis;

    private final String lts;
    private final AeaaLtsState ltsState;
    private final long ltsMillis;

    private final String discontinued;
    private final AeaaDiscontinuedState discontinuedState;
    private final long discontinuedMillis;

    private final String support;
    private final AeaaSupportState supportState;
    private final long supportMillis;

    private final String extendedSupport;
    private final AeaaSupportState extendedAeaaSupportState;
    private final long extendedSupportMillis;

    private final String technicalGuidance;
    private final AeaaTechnicalGuidanceState technicalGuidanceState;
    private final long technicalGuidanceMillis;

    private final AeaaCycleStateScenario cycleStateScenario;
    private final AeaaCycleStateExtendedSupportInformationNotPresent cycleStateExtendedSupportInformationNotPresent;
    private final AeaaCycleStateExtendedSupportInformationPresent cycleStateExtendedSupportInformationPresent;

    private final boolean isAlreadyLatestCycleVersion;
    private final boolean isAlreadyLatestVersion;

    private final String latestCycleVersion;
    private final String latestLifecycleVersion;

    private final String nextSupportedVersion;
    private final String nextSupportedExtendedVersion;

    private final String closestActiveLtsVersion;
    private final String latestActiveLtsVersion;

    public AeaaExportedCycleState(
            Artifact artifact, AeaaEolCycle cycle,
            String eol, AeaaEolState eolState, long eolMillis,
            String lts, AeaaLtsState ltsState, long ltsMillis,
            String discontinued, AeaaDiscontinuedState discontinuedState, long discontinuedMillis,
            String support, AeaaSupportState supportState, long supportMillis,
            String extendedSupport, AeaaSupportState extendedAeaaSupportState, long extendedSupportMillis,
            String technicalGuidance, AeaaTechnicalGuidanceState technicalGuidanceState, long technicalGuidanceMillis,
            AeaaCycleStateScenario cycleStateScenario, AeaaCycleStateExtendedSupportInformationNotPresent cycleStateExtendedSupportInformationNotPresent, AeaaCycleStateExtendedSupportInformationPresent cycleStateExtendedSupportInformationPresent,
            boolean isAlreadyLatestCycleVersion, boolean isAlreadyLatestVersion,
            String latestCycleVersion, String latestLifecycleVersion,
            String nextSupportedVersion, String nextSupportedExtendedVersion,
            String closestActiveLtsVersion, String latestActiveLtsVersion
    ) {
        this.artifact = artifact;
        this.cycle = cycle;

        this.eol = eol;
        this.eolState = eolState;
        this.eolMillis = eolMillis;
        this.lts = lts;
        this.ltsState = ltsState;
        this.ltsMillis = ltsMillis;
        this.discontinued = discontinued;
        this.discontinuedState = discontinuedState;
        this.discontinuedMillis = discontinuedMillis;
        this.support = support;
        this.supportState = supportState;
        this.supportMillis = supportMillis;
        this.extendedSupport = extendedSupport;
        this.extendedAeaaSupportState = extendedAeaaSupportState;
        this.extendedSupportMillis = extendedSupportMillis;
        this.technicalGuidance = technicalGuidance;
        this.technicalGuidanceState = technicalGuidanceState;
        this.technicalGuidanceMillis = technicalGuidanceMillis;

        this.cycleStateScenario = cycleStateScenario;
        this.cycleStateExtendedSupportInformationNotPresent = cycleStateExtendedSupportInformationNotPresent;
        this.cycleStateExtendedSupportInformationPresent = cycleStateExtendedSupportInformationPresent;

        this.isAlreadyLatestCycleVersion = isAlreadyLatestCycleVersion;
        this.isAlreadyLatestVersion = isAlreadyLatestVersion;
        this.latestCycleVersion = latestCycleVersion;
        this.latestLifecycleVersion = latestLifecycleVersion;
        this.nextSupportedVersion = nextSupportedVersion;
        this.nextSupportedExtendedVersion = nextSupportedExtendedVersion;
        this.closestActiveLtsVersion = closestActiveLtsVersion;
        this.latestActiveLtsVersion = latestActiveLtsVersion;
    }

    public AeaaCycleStateExtendedSupportInformationPresent getCycleStateExtendedSupportAvailable() {
        return cycleStateExtendedSupportInformationPresent;
    }

    public AeaaCycleStateExtendedSupportInformationNotPresent getCycleStateExtendedSupportUnavailable() {
        return cycleStateExtendedSupportInformationNotPresent;
    }

    public boolean isLts() {
        return ltsState == AeaaLtsState.LTS || ltsState == AeaaLtsState.LTS_DATE_REACHED;
    }

    public boolean isEol() {
        return eolState == AeaaEolState.EOL || eolState == AeaaEolState.EOL_DATE_REACHED;
    }

    public boolean isDiscontinued() {
        return discontinuedState == AeaaDiscontinuedState.DISCONTINUED || discontinuedState == AeaaDiscontinuedState.DISCONTINUED_DATE_REACHED;
    }

    public boolean isSupport() {
        return supportState == AeaaSupportState.SUPPORT || supportState == AeaaSupportState.UPCOMING_SUPPORT_END_DATE || supportState == AeaaSupportState.DISTANT_SUPPORT_END_DATE;
    }

    public boolean isExtendedSupport() {
        return extendedAeaaSupportState == AeaaSupportState.SUPPORT || extendedAeaaSupportState == AeaaSupportState.UPCOMING_SUPPORT_END_DATE || extendedAeaaSupportState == AeaaSupportState.DISTANT_SUPPORT_END_DATE;
    }

    public boolean isTechnicalGuidance() {
        return technicalGuidanceState == AeaaTechnicalGuidanceState.TECHNICAL_GUIDANCE || technicalGuidanceState == AeaaTechnicalGuidanceState.UPCOMING_END_OF_TECHNICAL_GUIDANCE_DATE;
    }

    private static String getLatestFromFilteredLifecycleOrNull(AeaaEolLifecycle lifecycle, AeaaEolCycle current, BiFunction<AeaaEolLifecycle, AeaaEolCycle, AeaaEolCycle> converter) {
        if (lifecycle == null) {
            return null;
        }

        final AeaaEolCycle cycle = converter.apply(lifecycle, current);

        if (cycle != null) {
            return cycle.getLatest();
        } else {
            return null;
        }
    }

    public static Map<String, List<AeaaExportedCycleState>> parseAndSortByProduct(Collection<Artifact> artifacts) {
        final Map<String, List<AeaaExportedCycleState>> statesByProducts = new TreeMap<>();

        for (Artifact artifact : artifacts) {
            AeaaExportedCycleState.fromArtifact(artifact).ifPresent(cycleStates -> {
                if (cycleStates.isEmpty()) return;
                for (AeaaExportedCycleState cycleState : cycleStates) {
                    final String productName = cycleState.getCycle().getProduct();
                    statesByProducts.computeIfAbsent(productName, p -> new ArrayList<>()).add(cycleState);
                }
            });

        }

        return statesByProducts;
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();

        json.put("artifact", new JSONObject()
                .put("id", artifact.getId())
                .put("version", artifact.getVersion()));

        json.put("cycle", cycle.toJson());

        json.put("eol", new JSONObject()
                .put("date", eol)
                .put("state", eolState.name())
                .put("millisTillEol", eolMillis)
                .put("formattedMillisTillEol", AeaaEolCycle.formatTimeUntilOrAgo(eolMillis))
                .put("active", isEol()));

        json.put("lts", new JSONObject()
                .put("date", lts)
                .put("state", ltsState.name())
                .put("millisTillLts", ltsMillis)
                .put("formattedMillisTillLts", AeaaEolCycle.formatTimeUntilOrAgo(ltsMillis))
                .put("active", isLts()));

        json.put("discontinued", new JSONObject()
                .put("date", discontinued)
                .put("state", discontinuedState.name())
                .put("millisTillDiscontinued", discontinuedMillis)
                .put("formattedMillisTillDiscontinued", AeaaEolCycle.formatTimeUntilOrAgo(discontinuedMillis))
                .put("active", isDiscontinued()));

        json.put("support", new JSONObject()
                .put("date", support)
                .put("state", supportState.name())
                .put("millisTillSupportEnd", supportMillis)
                .put("formattedMillisTillSupportEnd", AeaaEolCycle.formatTimeUntilOrAgo(supportMillis))
                .put("active", isSupport()));

        json.put("extendedSupport", new JSONObject()
                .put("date", extendedSupport)
                .put("state", extendedAeaaSupportState.name())
                .put("millisTillExtendedSupportEnd", extendedSupportMillis)
                .put("formattedMillisTillExtendedSupportEnd", AeaaEolCycle.formatTimeUntilOrAgo(extendedSupportMillis))
                .put("active", isExtendedSupport()));

        json.put("technicalGuidance", new JSONObject()
                .put("date", technicalGuidance)
                .put("state", technicalGuidanceState.name())
                .put("millisTillTechnicalGuidanceEnd", technicalGuidanceMillis)
                .put("formattedMillisTillTechnicalGuidanceEnd", AeaaEolCycle.formatTimeUntilOrAgo(technicalGuidanceMillis))
                .put("active", isTechnicalGuidance()));

        json.put("state", new JSONObject()
                .put("scenario", cycleStateScenario.getKey())
                .put("extendedSupportAvailable", cycleStateExtendedSupportInformationPresent != null ? cycleStateExtendedSupportInformationPresent.toJson() : null)
                .put("extendedSupportUnavailable", cycleStateExtendedSupportInformationNotPresent != null ? cycleStateExtendedSupportInformationNotPresent.toJson() : null));

        json.put("versionRecommendation", new JSONObject()
                .put("cycle", new JSONObject()
                        .put("alreadyActive", isAlreadyLatestCycleVersion)
                        .put("latest", latestCycleVersion))
                .put("lifecycle", new JSONObject()
                        .put("alreadyActive", isAlreadyLatestVersion)
                        .put("latest", latestLifecycleVersion))
                .put("nextSupported", nextSupportedVersion)
                .put("nextSupportedExtended", nextSupportedExtendedVersion)
                .put("closestActiveLtsVersion", closestActiveLtsVersion)
                .put("latestActiveLtsVersion", latestActiveLtsVersion));

        return json;
    }

    public static Optional<List<AeaaExportedCycleState>> fromArtifact(Artifact artifact) {
        if (StringUtils.hasText(artifact.get(AeaaInventoryAttribute.EOL_FULL_STATE.getKey()))) {
            return Optional.of(fromJson(artifact, new JSONArray(artifact.get(AeaaInventoryAttribute.EOL_FULL_STATE.getKey()))));
        } else {
            return Optional.empty();
        }
    }

    public static AeaaExportedCycleState fromJson(Inventory inventory, JSONObject json) {
        final JSONObject artifactJson = json.getJSONObject("artifact");
        final Artifact artifact = inventory.findArtifact(artifactJson.optString("id", null));

        return fromJson(artifact, json);
    }

    public static List<AeaaExportedCycleState> fromJson(Artifact artifact) {
        if (artifact.get(AeaaInventoryAttribute.EOL_FULL_STATE.getKey()) == null) {
            return new ArrayList<>();
        }
        return fromJson(artifact, new JSONArray(artifact.get(AeaaInventoryAttribute.EOL_FULL_STATE.getKey())));
    }

    public static List<AeaaExportedCycleState> fromJson(Artifact artifact, JSONArray json) {
        final List<AeaaExportedCycleState> states = new ArrayList<>();

        for (int i = 0; i < json.length(); i++) {
            states.add(fromJson(artifact, json.getJSONObject(i)));
        }

        return states;
    }

    public static AeaaExportedCycleState fromJson(Artifact artifact, JSONObject json) {
        final AeaaEolCycle cycle = AeaaEolCycle.fromJson(json.getJSONObject("cycle"));

        final JSONObject eolJson = json.getJSONObject("eol");
        final String eol = eolJson.optString("date", null);
        final AeaaEolState eolState = AeaaEolState.valueOf(eolJson.getString("state"));
        final long eolMillis = eolJson.getLong("millisTillEol");

        final JSONObject ltsJson = json.getJSONObject("lts");
        final String lts = ltsJson.optString("date", null);
        final AeaaLtsState ltsState = AeaaLtsState.valueOf(ltsJson.getString("state"));
        final long ltsMillis = ltsJson.getLong("millisTillLts");

        final JSONObject discontinuedJson = json.getJSONObject("discontinued");
        final String discontinued = discontinuedJson.optString("date", null);
        final AeaaDiscontinuedState discontinuedState = AeaaDiscontinuedState.valueOf(discontinuedJson.getString("state"));
        final long discontinuedMillis = discontinuedJson.getLong("millisTillDiscontinued");

        final JSONObject supportJson = json.getJSONObject("support");
        final String support = supportJson.optString("date", null);
        final AeaaSupportState supportState = AeaaSupportState.valueOf(supportJson.getString("state"));
        final long supportMillis = supportJson.getLong("millisTillSupportEnd");

        final JSONObject extendedSupportJson = json.getJSONObject("extendedSupport");
        final String extendedSupport = extendedSupportJson.optString("date", null);
        final AeaaSupportState extendedAeaaSupportState = AeaaSupportState.valueOf(extendedSupportJson.getString("state"));
        final long extendedSupportMillis = extendedSupportJson.getLong("millisTillExtendedSupportEnd");

        final JSONObject technicalGuidanceJson = json.getJSONObject("technicalGuidance");
        final String technicalGuidance = technicalGuidanceJson.optString("date", null);
        final AeaaTechnicalGuidanceState technicalGuidanceState = AeaaTechnicalGuidanceState.valueOf(technicalGuidanceJson.getString("state"));
        final long technicalGuidanceMillis = technicalGuidanceJson.getLong("millisTillTechnicalGuidanceEnd");

        final AeaaCycleStateScenario cycleStateScenario = AeaaCycleStateScenario.fromKey(json.getJSONObject("state").getString("scenario"));

        final JSONObject extendedSupportAvailableJson = json.getJSONObject("state").optJSONObject("extendedSupportAvailable");
        final AeaaCycleStateExtendedSupportInformationPresent extendedSupportAvailable;
        if (extendedSupportAvailableJson != null) {
            extendedSupportAvailable = AeaaCycleStateExtendedSupportInformationPresent.fromJson(extendedSupportAvailableJson);
        } else {
            extendedSupportAvailable = null;
        }

        final JSONObject extendedSupportUnavailableJson = json.getJSONObject("state").optJSONObject("extendedSupportUnavailable");
        final AeaaCycleStateExtendedSupportInformationNotPresent extendedSupportUnavailable;
        if (extendedSupportUnavailableJson != null) {
            extendedSupportUnavailable = AeaaCycleStateExtendedSupportInformationNotPresent.fromJson(extendedSupportUnavailableJson);
        } else {
            extendedSupportUnavailable = null;
        }

        final JSONObject versionRecommendationJson = json.getJSONObject("versionRecommendation");
        final JSONObject cycleVersionJson = versionRecommendationJson.getJSONObject("cycle");
        final boolean isAlreadyLatestCycleVersion = cycleVersionJson.getBoolean("alreadyActive");
        final String latestCycleVersion = cycleVersionJson.optString("latest", null);

        final JSONObject lifecycleJson = versionRecommendationJson.getJSONObject("lifecycle");
        final boolean isAlreadyLatestVersion = lifecycleJson.getBoolean("alreadyActive");
        final String latestVersion = lifecycleJson.optString("latest", null);

        final String nextSupportedVersion = versionRecommendationJson.optString("nextSupported", null);
        final String nextSupportedExtendedVersion = versionRecommendationJson.optString("nextSupportedExtended", null);

        final String closestActiveLtsVersion = versionRecommendationJson.optString("closestActiveLtsVersion", null);
        final String latestActiveLtsVersion = versionRecommendationJson.optString("latestActiveLtsVersion", null);

        return new AeaaExportedCycleState(
                artifact, cycle,
                eol, eolState, eolMillis,
                lts, ltsState, ltsMillis,
                discontinued, discontinuedState, discontinuedMillis,
                support, supportState, supportMillis,
                extendedSupport, extendedAeaaSupportState, extendedSupportMillis,
                technicalGuidance, technicalGuidanceState, technicalGuidanceMillis,
                cycleStateScenario, extendedSupportUnavailable, extendedSupportAvailable,
                isAlreadyLatestCycleVersion, isAlreadyLatestVersion,
                latestCycleVersion, latestVersion,
                nextSupportedVersion, nextSupportedExtendedVersion,
                closestActiveLtsVersion, latestActiveLtsVersion
        );
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static Map<String, List<AeaaExportedCycleState>> groupByArtifactVersion(Collection<AeaaExportedCycleState> states) {
        final Map<String, List<AeaaExportedCycleState>> grouped = new LinkedHashMap<>();

        for (AeaaExportedCycleState state : states) {
            if (state.getArtifact().getVersion() == null) {
                grouped.computeIfAbsent("", k -> new ArrayList<>()).add(state);
            } else {
                grouped.computeIfAbsent(state.getArtifact().getVersion(), k -> new ArrayList<>()).add(state);
            }
        }

        return grouped;
    }
}
