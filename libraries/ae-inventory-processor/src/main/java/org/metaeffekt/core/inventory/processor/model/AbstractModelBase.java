/**
 * Copyright 2009-2019 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.model;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractModelBase {

    public interface Attribute {
        String getKey();
    }

    // map to store key values pairs.
    private Map<String, String> attributeMap = new HashMap<>();

    public AbstractModelBase(AbstractModelBase baseModelInstance) {
        this.attributeMap = new HashMap<>(baseModelInstance.attributeMap);
    }

    public AbstractModelBase() {
    }

    public String get(String key) {
        return get(key, null);
    }

    public String get(String key, String defaultValue) {
        if (key == null) return null;
        String currentValue = attributeMap.get(key);
        return (currentValue != null) ? currentValue : defaultValue;
    }

    public void set(String key, String value) {
        if (value == null) {
            attributeMap.remove(key);
        } else {
            attributeMap.put(key, value);
        }
    }

    public void append(String key, String value, String delimiter) {
        String currentValue = get(key);
        if (currentValue == null) {
            set(key, value);
        } else {
            set(key, currentValue + delimiter + value);
        }
    }

    public Set<String> getAttributes() {
        return attributeMap.keySet();
    }

    public int numAttributes() {
        return attributeMap.size();
    }

    public float getFloat(String key, float defaultValue) {
        String stringValue = get(key);
        if (stringValue == null) return defaultValue;
        try {
            return Float.parseFloat(stringValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
