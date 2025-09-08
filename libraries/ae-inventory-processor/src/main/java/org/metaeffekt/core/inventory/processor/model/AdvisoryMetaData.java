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
package org.metaeffekt.core.inventory.processor.model;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class AdvisoryMetaData extends AbstractModelBase {

    // Maximize compatibility with serialized inventories
    private static final long serialVersionUID = 1L;

    public static final String STATUS_VALUE_UNAFFECTED = "unaffected";
    public static final String STATUS_VALUE_UNCLASSIFIED = "unclassified";
    public static final String STATUS_VALUE_NEW = "new";
    public static final String STATUS_VALUE_IN_REVIEW = "in review";
    public static final String STATUS_VALUE_REVIEWED = "reviewed";

    public static final List<String> ADVISORY_REVIEW_STATUS_VALUES = Arrays.asList(
            STATUS_VALUE_UNAFFECTED,
            STATUS_VALUE_UNCLASSIFIED,
            STATUS_VALUE_NEW,
            STATUS_VALUE_IN_REVIEW,
            STATUS_VALUE_REVIEWED
    );

    public static Comparator<AdvisoryMetaData> CERT_COMPARATOR_NAME_DESC = Comparator.comparing(cm -> cm.get(Attribute.NAME));
    public static final Comparator<AdvisoryMetaData> CERT_COMPARATOR_LAST_UPDATED_DESC = Comparator.comparing((AdvisoryMetaData cm) -> cm.get(Attribute.UPDATE_DATE)).reversed();


    public AdvisoryMetaData(AdvisoryMetaData cm) {
        super(cm);
    }

    public AdvisoryMetaData() {
    }

    /**
     * Core attributes to support cert advisories.
     */
    public enum Attribute implements AbstractModelBase.Attribute {
        NAME("Name"),
        URL("URL"),
        SUMMARY("Summary"),
        SOURCE("Source"),
        SOURCE_IMPLEMENTATION("Source-Implementation"),
        TYPE("Type"),

        DESCRIPTION("Description"),
        THREAT("Threat"),
        RECOMMENDATIONS("Recommendations"),
        WORKAROUNDS("Workarounds"),
        ACKNOWLEDGEMENTS("Acknowledgements"),
        KEYWORDS("Keywords"),
        REFERENCES("References"),
        @Deprecated
        REFERENCED_IDS("Referenced Ids"),
        REFERENCED_VULNERABILITIES("Referenced Vulnerabilities"),
        REFERENCED_SECURITY_ADVISORIES("Referenced Advisories"),
        REFERENCED_OTHER("Other Referenced Ids"),
        DATA_SOURCE("Data Source"),
        MATCHING_SOURCE("Matching Source"),

        CVSS_VECTORS("CVSS Vectors"),

        CREATE_DATE("Create Date"),
        CREATE_DATE_FORMATTED("Formatted Create Date"),
        UPDATE_DATE("Update Date"),
        UPDATE_DATE_FORMATTED("Formatted Update Date"),
        REVIEW_STATUS("Review Status");

        private String key;

        Attribute(String key) {
            this.key = key;
        }

        public static Attribute match(String key) {
            for (Attribute a : values()) {
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
        MIN_ATTRIBUTES.add(Attribute.NAME.getKey());
        MIN_ATTRIBUTES.add(Attribute.URL.getKey());
        MIN_ATTRIBUTES.add(Attribute.SUMMARY.getKey());
        MIN_ATTRIBUTES.add(Attribute.SOURCE.getKey());
        MIN_ATTRIBUTES.add(Attribute.TYPE.getKey());
    }

    /**
     * Defines the core attributes. Used for logging and ordering.
     */
    public static ArrayList<String> CORE_ATTRIBUTES = new ArrayList<>();
    static {
        CORE_ATTRIBUTES.add(Attribute.NAME.getKey());
        CORE_ATTRIBUTES.add(Attribute.URL.getKey());
        CORE_ATTRIBUTES.add(Attribute.SUMMARY.getKey());
        CORE_ATTRIBUTES.add(Attribute.SOURCE.getKey());
        CORE_ATTRIBUTES.add(Attribute.TYPE.getKey());
        CORE_ATTRIBUTES.add(Attribute.CREATE_DATE.getKey());
        CORE_ATTRIBUTES.add(Attribute.UPDATE_DATE.getKey());
        CORE_ATTRIBUTES.add(Attribute.REVIEW_STATUS.getKey());
    }

    /**
     * Validates that the mandatory attributes of a component are set.
     *
     * @return Boolean indicating whether the instance is valid.
     */
    public boolean isValid() {
        if (StringUtils.isEmpty(get(Attribute.NAME))) return false;
        return true;
    }

    /**
     * @return The derived string qualifier for this instance.
     */
    public String deriveQualifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(get(Attribute.NAME));
        return sb.toString();
    }

    /**
     * The compare string representations is built from the core attributes.
     *
     * @return String to compare component patterns.
     */
    public String createCompareStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        for (String attributeKey : CORE_ATTRIBUTES) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(get(attributeKey));
        }
        return sb.toString();
    }

    public String get(Attribute attribute, String defaultValue) {
        return get(attribute.getKey(), defaultValue);
    }

    public float getFloat(Attribute attribute, float defaultValue) {
        return getFloat(attribute.getKey(), defaultValue);
    }

    public String get(Attribute attribute) {
        return get(attribute.getKey());
    }

    public void set(Attribute attribute, String value) {
        set(attribute.getKey(), value);
    }

    public void append(Attribute attribute, String value, String delimiter) {
        append(attribute.getKey(), value, delimiter);
    }

    public String getName() {
        return get(Attribute.NAME);
    }

    public static boolean hasDetails(AdvisoryMetaData cm) {

        // TODO: define what attributes have to be defined in order for the entry to have details
        if (true || StringUtils.isNotBlank(cm.get(Attribute.NAME))) {
            return true;
        }

        return false;
    }

    public static AdvisoryMetaData getByName(Collection<AdvisoryMetaData> advisoryMetaData, String name) {
        return advisoryMetaData.stream()
                .filter(e -> name.equals(e.get(Attribute.NAME)))
                .findFirst()
                .orElse(null);
    }

}
