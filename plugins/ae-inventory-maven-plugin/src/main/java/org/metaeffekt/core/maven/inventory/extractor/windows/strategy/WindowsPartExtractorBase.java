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
package org.metaeffekt.core.maven.inventory.extractor.windows.strategy;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public abstract class WindowsPartExtractorBase {

    protected String findFirstNonNull(JSONObject jsonObject, Set<String> keys) {
        return keys.stream()
                .map(key -> jsonObject.optString(key, null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    protected <T extends AbstractModelBase> void mapBaseJsonInformationToInventory(JSONObject json, T model) {
        mapJsonFieldToInventory(json, model, "Computer", "Computer");
    }

    protected <T extends AbstractModelBase> void mapJsonFieldToInventory(JSONObject json, T model, AbstractModelBase.Attribute inventoryField, String... jsonFields) {
        mapJsonFieldToInventory(json, model, inventoryField.getKey(), jsonFields);
    }

    protected String getJsonFieldValue(JSONObject json, String... jsonFields) {
        for (String field : jsonFields) {
            if (isStringFieldPresent(json, field)) {
                return json.getString(field);
            } else if (isNumberFieldPresent(json, field)) {
                return String.valueOf(json.getInt(field));
            }
        }
        return null;
    }

    protected Integer getJsonFieldValueInt(JSONObject json, String... jsonFields) {
        for (String field : jsonFields) {
            if (isNumberFieldPresent(json, field)) {
                return json.getInt(field);
            }
        }
        return null;
    }

    protected <T extends AbstractModelBase> void mapJsonFieldToInventory(JSONObject json, T model, String inventoryField, String... jsonFields) {
        final String fieldValue = getJsonFieldValue(json, jsonFields);
        if (fieldValue != null) {
            model.set(inventoryField, fieldValue);
        }
    }

    protected JSONObject findFirstJsonObjectInArray(JSONArray array, String key, Object value) {
        for (int i = 0; i < array.length(); i++) {
            final JSONObject object = array.getJSONObject(i);
            if (Objects.equals(object.opt(key), value)) {
                return object;
            }
        }
        return null;
    }

    protected JSONObject findFirstJsonObjectInArray(JSONArray array, Map<String, Object> keyValues) {
        for (int i = 0; i < array.length(); i++) {
            final JSONObject object = array.getJSONObject(i);
            boolean matches = true;
            for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
                if (!Objects.equals(object.opt(entry.getKey()), entry.getValue())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return object;
            }
        }
        return null;
    }

    protected boolean isStringFieldPresent(JSONObject json, String field) {
        final Object fieldValue = json.opt(field);
        return fieldValue instanceof String && StringUtils.isNotEmpty((String) fieldValue);
    }

    protected boolean isNumberFieldPresent(JSONObject json, String field) {
        return json.has(field) && !json.isNull(field) && json.get(field) instanceof Number;
    }

    protected Artifact findArtifactOrElseAppendNew(Inventory inventory, Predicate<Artifact> predicate) {
        final Artifact existing = findArtifact(inventory, predicate);
        if (existing != null) return existing;

        final Artifact constructed = new Artifact();
        inventory.getArtifacts().add(constructed);
        return constructed;
    }

    protected Artifact findArtifact(Inventory inventory, Predicate<Artifact> predicate) {
        for (Artifact artifact : inventory.getArtifacts()) {
            if (predicate.test(artifact)) {
                return artifact;
            }
        }
        return null;
    }
}
