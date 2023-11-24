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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.report.model.AdvisoryData;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaContentIdentifiers;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaInventoryAttribute;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.msrc.AeaaMsThreat;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.msrc.AeaaMsrcRemediation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.advisory.MsrcAdvisorEntry</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public class AeaaMsrcAdvisorEntry extends AeaaAdvisoryEntry {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaMsrcAdvisorEntry.class);

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_AMB) {{
        add(AeaaInventoryAttribute.MS_AFFECTED_PRODUCTS.getKey());
        add(AeaaInventoryAttribute.MS_THREATS.getKey());
        add(AeaaInventoryAttribute.MS_REMEDIATIONS.getKey());
    }};

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<String>(AeaaAdvisoryEntry.CONVERSION_KEYS_MAP) {{
        add("affectedProducts");
        add("msThreats");
        add("msRemediations");
    }};


    protected final Set<String> affectedProducts = new HashSet<>();
    protected final Set<AeaaMsThreat> msThreats = new HashSet<>();
    protected final Set<AeaaMsrcRemediation> msrcRemediations = new HashSet<>();

    public AeaaMsrcAdvisorEntry() {
        super(AeaaContentIdentifiers.MSRC);
    }

    public AeaaMsrcAdvisorEntry(String id) {
        super(AeaaContentIdentifiers.MSRC, id);
    }

    public void addAffectedProduct(String product) {
        affectedProducts.add(product);
    }

    public void addAffectedProducts(Collection<String> products) {
        affectedProducts.addAll(products);
    }

    public void addMsThreat(AeaaMsThreat threat) {
        msThreats.add(threat);
    }

    public void addMsThreats(Collection<AeaaMsThreat> threats) {
        msThreats.addAll(threats);
    }

    private void addMsThreats(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject json = jsonArray.getJSONObject(i);
            final AeaaMsThreat threat = AeaaMsThreat.fromJson(json);
            msThreats.add(threat);
        }
    }

    private void addMsThreats(List<Map<String, Object>> maps) {
        for (Map<String, Object> map : maps) {
            final AeaaMsThreat threat = AeaaMsThreat.fromMap(map);
            msThreats.add(threat);
        }
    }

    public void addMsRemediation(AeaaMsrcRemediation remediation) {
        msrcRemediations.add(remediation);
    }

    public void addMsRemediations(Collection<AeaaMsrcRemediation> remediations) {
        msrcRemediations.addAll(remediations);
    }

    private void addMsRemediations(JSONArray jsonArray) {
        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject json = jsonArray.getJSONObject(i);
            final AeaaMsrcRemediation remediation = AeaaMsrcRemediation.fromJson(json);
            msrcRemediations.add(remediation);
        }
    }

    private void addMsRemediations(List<Map<String, Object>> maps) {
        for (Map<String, Object> map : maps) {
            final AeaaMsrcRemediation remediation = AeaaMsrcRemediation.fromMap(map);
            msrcRemediations.add(remediation);
        }
    }

    public Set<String> getAffectedProducts() {
        return affectedProducts;
    }

    public Set<AeaaMsThreat> getMsThreats() {
        return msThreats;
    }

    public Set<AeaaMsrcRemediation> getMsRemediations() {
        return msrcRemediations;
    }

    public Set<AeaaMsrcRemediation> getMsRemediationsByAffectedProduct(String productId) {
        return msrcRemediations.stream()
                .filter(r -> r.getAffectedProductIds().contains(productId))
                .collect(Collectors.toSet());
    }

    public Set<AeaaMsrcRemediation> getMsRemediationsByDescriptionEquals(String description) {
        if (description.charAt(0) == 'K' && description.charAt(1) == 'B') {
            LOG.warn("MSRC remediation description starts with KB, which is most likely a mistake: {}", description);
        }
        return msrcRemediations.stream()
                .filter(r -> Objects.equals(description, r.getDescription()))
                .collect(Collectors.toSet());
    }

    public String getCveFromId() {
        if (StringUtils.isEmpty(super.id) || super.id.contains("ADV")) return null;
        return super.id.replace("MSRC-", "");
    }

    @Override
    public String getUrl() {
        return "https://msrc.microsoft.com/update-guide/en-US/vulnerability/" + id;
    }

    @Override
    public String getType() {
        return AdvisoryData.normalizeType("alert");
    }

    /* TYPE CONVERSION METHODS */

    @Override
    protected Set<String> conversionKeysAmb() {
        return CONVERSION_KEYS_AMB;
    }

    @Override
    protected Set<String> conversionKeysMap() {
        return CONVERSION_KEYS_MAP;
    }

    public static AeaaMsrcAdvisorEntry fromAdvisoryMetaData(AdvisoryMetaData amd) {
        return AeaaAdvisoryEntry.fromAdvisoryMetaData(amd, AeaaMsrcAdvisorEntry::new);
    }

    public static AeaaMsrcAdvisorEntry fromInputMap(Map<String, Object> map) {
        return AeaaAdvisoryEntry.fromInputMap(map, AeaaMsrcAdvisorEntry::new);
    }

    public static AeaaMsrcAdvisorEntry fromJson(JSONObject json) {
        return AeaaAdvisoryEntry.fromJson(json, AeaaMsrcAdvisorEntry::new);
    }

    @Override
    public void appendFromDataClass(AeaaAdvisoryEntry dataClass) {
        super.appendFromDataClass(dataClass);

        if (!(dataClass instanceof AeaaMsrcAdvisorEntry)) {
            return;
        }

        final AeaaMsrcAdvisorEntry msrcAdvisorEntry = (AeaaMsrcAdvisorEntry) dataClass;

        this.addAffectedProducts(msrcAdvisorEntry.getAffectedProducts());
        this.addMsThreats(msrcAdvisorEntry.getMsThreats());
        this.addMsRemediations(msrcAdvisorEntry.getMsRemediations());
    }

    @Override
    public void appendFromBaseModel(AdvisoryMetaData amd) {
        super.appendFromBaseModel(amd);

        if (amd.get(AeaaInventoryAttribute.MS_AFFECTED_PRODUCTS) != null) {
            this.addAffectedProducts(Arrays.stream(amd.get(AeaaInventoryAttribute.MS_AFFECTED_PRODUCTS).split(", ")).collect(Collectors.toSet()));
        }
        if (amd.get(AeaaInventoryAttribute.MS_THREATS) != null) {
            this.addMsThreats(new JSONArray(amd.get(AeaaInventoryAttribute.MS_THREATS)));
        }
        if (amd.get(AeaaInventoryAttribute.MS_REMEDIATIONS) != null) {
            this.addMsRemediations(new JSONArray(amd.get(AeaaInventoryAttribute.MS_REMEDIATIONS)));
        }
    }

    @Override
    public void appendToBaseModel(AdvisoryMetaData amd) {
        super.appendToBaseModel(amd);

        amd.set(AeaaInventoryAttribute.MS_AFFECTED_PRODUCTS, String.join(", ", affectedProducts));
        amd.set(AeaaInventoryAttribute.MS_THREATS, new JSONArray(msThreats.stream().map(AeaaMsThreat::toJson).collect(Collectors.toList())).toString());
        amd.set(AeaaInventoryAttribute.MS_REMEDIATIONS, new JSONArray(msrcRemediations.stream().map(AeaaMsrcRemediation::toJson).collect(Collectors.toList())).toString());
    }

    @Override
    public void appendFromMap(Map<String, Object> map) {
        super.appendFromMap(map);

        try {
            if (map.containsKey("affectedProducts")) {
                this.addAffectedProducts(((List<Object>) map.get("affectedProducts")).stream().map(Object::toString).collect(Collectors.toSet()));
            }
            if (map.containsKey("msThreats")) {
                this.addMsThreats(((List<Map<String, Object>>) map.get("msThreats")));
            }
            if (map.containsKey("msRemediations")) {
                this.addMsRemediations(((List<Map<String, Object>>) map.get("msRemediations")));
            }
        } catch (Exception e) {
            LOG.error("Error parsing MSRC Advisor entry from map:\n" + map, e);
        }
    }

    @Override
    public void appendToJson(JSONObject json) {
        super.appendToJson(json);

        json.put("affectedProducts", new JSONArray(affectedProducts));
        json.put("msThreats", new JSONArray(msThreats.stream().map(AeaaMsThreat::toJson).collect(Collectors.toList())));
        json.put("msRemediations", new JSONArray(msrcRemediations.stream().map(AeaaMsrcRemediation::toJson).collect(Collectors.toList())));
    }

    public static Set<String> getAllMsrcProductIds(Collection<Artifact> artifacts) {
        return artifacts.stream()
                .filter(a -> StringUtils.isNotBlank(a.get(AeaaInventoryAttribute.MS_PRODUCT_ID.getKey())))
                .map(a -> a.get(AeaaInventoryAttribute.MS_PRODUCT_ID.getKey()))
                .map(s -> s.split(","))
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());
    }
}
