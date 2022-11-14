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
package org.metaeffekt.core.inventory.processor.model;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class CertMetaData extends AbstractModelBase {

    public static final String STATUS_VALUE_UNAFFECTED = "unaffected";
    public static final String STATUS_VALUE_NEW = "new";
    public static final String STATUS_VALUE_IN_REVIEW = "in review";
    public static final String STATUS_VALUE_REVIEWED = "reviewed";

    public static Comparator<CertMetaData> CERT_COMPARATOR_NAME_DESC = Comparator.comparing(cm -> cm.get(Attribute.NAME));

    public static Comparator<Object> CERT_COMPARATOR_LAST_UPDATED_DESC = Comparator.comparing(cm -> ((CertMetaData) cm).get(Attribute.UPDATE_DATE)).reversed();

    public CertMetaData(CertMetaData cm) {
        super(cm);
    }

    public CertMetaData() {
    }

    /**
     * Core attributes to support cert advisories.
     */
    public enum Attribute implements AbstractModelBase.Attribute {
        NAME("Name"),
        URL("Url"),
        SUMMARY("Summary"),
        SOURCE("Source"),
        TYPE("Type"),
        CREATE_DATE("Create Date"),
        UPDATE_DATE("Update Date"),
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

    public static ArrayList<String> CORE_ATTRIBUTES = new ArrayList<>();

    static {
        // fix selection and order
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

    public static boolean hasDetails(CertMetaData cm) {

        // TODO: define what attributes have to be defined in order for the entry to have details
        if (true || StringUtils.hasText(cm.get(Attribute.NAME))) {
            return true;
        }

        return false;
    }

    public static CertMetaData getByName(Collection<CertMetaData> certMetaData, String name) {
        return certMetaData.stream()
                .filter(e -> name.equals(e.get(Attribute.NAME)))
                .findFirst()
                .orElse(null);
    }

}
