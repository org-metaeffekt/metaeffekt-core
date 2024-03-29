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

import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;

import java.util.*;
import java.util.function.Consumer;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.base.AmbDataClass</code> 
 * until separation of inventory report generation from ae core inventory processor.
 */
public abstract class AeaaAmbDataClass<AMB extends AbstractModelBase, DC extends AeaaAmbDataClass<AMB, DC>> implements Comparable<AeaaAmbDataClass<AMB, DC>> {

    protected final static Set<String> CONVERSION_KEYS_AMB = new HashSet<>(Arrays.asList(
            Artifact.Attribute.ID.getKey(),
            VulnerabilityMetaData.Attribute.NAME.getKey(),
            AdvisoryMetaData.Attribute.NAME.getKey()
    ));

    protected final static Set<String> CONVERSION_KEYS_MAP = new HashSet<>(Arrays.asList(
            "id"
    ));

    protected String id;
    protected Map<String, String> additionalAttributes = new LinkedHashMap<>();

    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void setAdditionalAttribute(String key, String value) {
        if (value == null) {
            additionalAttributes.remove(key);
        } else {
            additionalAttributes.put(key, value);
        }
    }

    public <T extends AbstractModelBase.Attribute> void setAdditionalAttribute(T key, String value) {
        if (value == null) {
            additionalAttributes.remove(key.getKey());
        } else {
            additionalAttributes.put(key.getKey(), value);
        }
    }

    public String getAdditionalAttribute(String key) {
        return additionalAttributes.get(key);
    }

    public <T extends AbstractModelBase.Attribute> String getAdditionalAttribute(T key) {
        return additionalAttributes.get(key.getKey());
    }

    public String getId() {
        return id;
    }

    public DC setId(String id) {
        this.id = id;
        return (DC) this;
    }

    public abstract AMB constructBaseModel();

    protected abstract Set<String> conversionKeysAmb();

    protected abstract Set<String> conversionKeysMap();

    public void appendFromDataClass(DC dataClass) {
        this.additionalAttributes.putAll(dataClass.getAdditionalAttributes());

        this.setId(dataClass.getId());
    }

    protected void appendToBaseModel(AMB baseModel) {
        for (Map.Entry<String, String> attributes : additionalAttributes.entrySet()) {
            baseModel.set(attributes.getKey(), attributes.getValue());
        }

        if (baseModel instanceof VulnerabilityMetaData) {
            baseModel.set(VulnerabilityMetaData.Attribute.NAME.getKey(), this.getId());
        } else if (baseModel instanceof AdvisoryMetaData) {
            baseModel.set(AdvisoryMetaData.Attribute.NAME.getKey(), this.getId());
        } else if (baseModel instanceof Artifact) {
            baseModel.set(Artifact.Attribute.ID.getKey(), this.getId());
        } else {
            baseModel.set("Id", this.getId());
        }
    }

    /**
     * Appends the data from a base model instance to this instance, overwriting existing values.<br>
     * Returns a set of keys that have been used to overwrite values.
     *
     * @param baseModel The base model instance to append from.
     */
    protected void appendFromBaseModel(AMB baseModel) {
        final Set<String> modelKeys = conversionKeysAmb();
        baseModel.getAttributes().stream()
                .filter(a -> !modelKeys.contains(a))
                .forEach(a -> this.getAdditionalAttributes().put(a, baseModel.get(a)));

        if (baseModel instanceof VulnerabilityMetaData) {
            this.setId(baseModel.get(VulnerabilityMetaData.Attribute.NAME));
        } else if (baseModel instanceof AdvisoryMetaData) {
            this.setId(baseModel.get(AdvisoryMetaData.Attribute.NAME));
        } else if (baseModel instanceof Artifact) {
            this.setId(baseModel.get(Artifact.Attribute.ID));
        } else {
            this.setId(baseModel.get("Id"));
        }
    }

    protected void appendToJson(JSONObject json) {
        for (Map.Entry<String, String> attributes : additionalAttributes.entrySet()) {
            json.put(attributes.getKey(), attributes.getValue());
        }

        json.put("id", this.getId());
    }

    protected void appendFromMap(Map<String, Object> map) {
        final Set<String> modelKeys = conversionKeysMap();
        map.keySet().stream()
                .filter(a -> !modelKeys.contains(a))
                .forEach(a -> this.getAdditionalAttributes().put(a, String.valueOf(map.get(a))));

        if (map.containsKey("id")) {
            this.setId(String.valueOf(map.get("id")));
        }
    }

    public AMB toBaseModel() {
        final AMB amb = constructBaseModel();

        appendToBaseModel(amb);

        return amb;
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();

        appendToJson(json);

        return json;
    }

    public <T> T performAction(Consumer<T> action) {
        action.accept((T) this);
        return (T) this;
    }

    @Override
    public int compareTo(AeaaAmbDataClass<AMB, DC> o) {
        if (this.getId() == null && o.getId() == null) return 0;
        if (this.getId() == null) return -1;
        if (o.getId() == null) return 1;
        return String.CASE_INSENSITIVE_ORDER.compare(this.getId(), o.getId());
    }
}
