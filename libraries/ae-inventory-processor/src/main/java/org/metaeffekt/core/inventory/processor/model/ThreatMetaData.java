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
package org.metaeffekt.core.inventory.processor.model;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

public class ThreatMetaData extends AbstractModelBase{

    public ThreatMetaData(AbstractModelBase baseModelInstance) {
        super(baseModelInstance);
    }

    public ThreatMetaData(){
    }

    public enum Attribute implements AbstractModelBase.Attribute {
        ID("Id"),
        NAME("Name"),

        DESCRIPTION("Description"),
        SOURCE("Source"),
        RATIONALE("Rationale"),

        BASED_ON("'Based On' Threat Reference"),
        RELATED_TO("'Related To' Threat Reference"),
        THREAT_REFERENCES("Threat References"),

        IMPACT_ASSESSMENTS("Impact Assessments"),

        REFERENCED_VULNERABILITIES("Referenced Vulnerabilities"),
        REFERENCED_SECURITY_ADVISORIES("Referenced Advisories"),
        REFERENCED_OTHER("Other Referenced Ids");

        private String key;

        Attribute(String key) {
            this.key = key;
        }

        public static ThreatMetaData.Attribute match(String key) {
            for (ThreatMetaData.Attribute a : values()) {
                if (a.getKey().equalsIgnoreCase(key)) {
                    return a;
                }
            }
            return null;
        }

        public String getKey() {
            return key;
        }
    }

    /**
     * Defines the minimum set of attributes for serialization. The order is not relevant.
     */
    public static ArrayList<String> MIN_ATTRIBUTES = new ArrayList<>();
    static {
        MIN_ATTRIBUTES.add(ThreatMetaData.Attribute.ID.getKey());
    }

    /**
     * Defines the core attributes. Used for logging and ordering.
     */
    public static ArrayList<String> CORE_ATTRIBUTES = new ArrayList<>();
    static {
        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.ID.getKey());
        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.NAME.getKey());

        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.DESCRIPTION.getKey());
        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.SOURCE.getKey());
        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.RATIONALE.getKey());

        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.BASED_ON.getKey());
        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.RELATED_TO.getKey());
        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.THREAT_REFERENCES.getKey());

        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.IMPACT_ASSESSMENTS.getKey());

        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.REFERENCED_VULNERABILITIES.getKey());
        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.REFERENCED_SECURITY_ADVISORIES.getKey());
        CORE_ATTRIBUTES.add(ThreatMetaData.Attribute.REFERENCED_OTHER.getKey());
    }

    /**
     * Validates that the mandatory attributes of a component are set.
     *
     * @return Boolean indicating whether the instance is valid.
     */
    public boolean isValid() {
        if (StringUtils.isEmpty(get(ThreatMetaData.Attribute.ID))) return false;
        return true;
    }

}
