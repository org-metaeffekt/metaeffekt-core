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
package org.metaeffekt.core.inventory.processor.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.JSONArray;
import org.metaeffekt.core.inventory.processor.configuration.converter.JsonArrayConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor
public class DemoConfiguration extends ProcessConfiguration {

    public static class CustomAdvisoryTypeIdentifier {
        private final String identifier;

        public CustomAdvisoryTypeIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String toJson() {
            return this.identifier;
        }

        public static List<CustomAdvisoryTypeIdentifier> parseAdvisoryProviders(String json) {
            final JSONArray array = new JSONArray(json);
            final List<CustomAdvisoryTypeIdentifier> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                result.add(new CustomAdvisoryTypeIdentifier(array.getString(i)));
            }
            return result;
        }

        public static JSONArray toJsonArray(List<CustomAdvisoryTypeIdentifier> list) {
            final JSONArray array = new JSONArray();
            for (CustomAdvisoryTypeIdentifier id : list) {
                array.put(id.toJson());
            }
            return array;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            final CustomAdvisoryTypeIdentifier that = (CustomAdvisoryTypeIdentifier) o;
            return this.identifier.equals(that.identifier);
        }

        @Override
        public int hashCode() {
            return this.identifier.hashCode();
        }
    }

    public static final CustomAdvisoryTypeIdentifier OSV_GENERIC_IDENTIFIER = new CustomAdvisoryTypeIdentifier("OSV");
    public static final CustomAdvisoryTypeIdentifier CSAF_GENERIC_IDENTIFIER = new CustomAdvisoryTypeIdentifier("CSAF");

    @Getter
    @Setter
    private int number = -1;

    @ProcessConfigurationProperty(alternativeNames = {"Deprecated Field Name"}, converter = JsonArrayConverter.class)
    private String advisoryTypes = new JSONArray()
            .put(OSV_GENERIC_IDENTIFIER.toJson()).toString();

    @ExcludeProcessConfigurationProperty
    private String ignoreProperty = "ignored";

    @Getter
    @Setter
    private DemoEnum demoEnum = DemoEnum.REAL;

    @Getter
    private final SubDemoConfiguration subDemoConfiguration = new SubDemoConfiguration();

    public List<CustomAdvisoryTypeIdentifier> getAdvisoryTypes() {
        return this.accessCachedProperty("custom", this.advisoryTypes, (s) -> Collections.unmodifiableList(CustomAdvisoryTypeIdentifier.parseAdvisoryProviders(s)));
    }

    public void setAdvisoryTypes(List<CustomAdvisoryTypeIdentifier> advisoryTypes) {
        this.advisoryTypes = CustomAdvisoryTypeIdentifier.toJsonArray(advisoryTypes).toString();
    }

    @Override
    public void collectMisconfigurations(List<ProcessMisconfiguration> misconfigurations) {

    }

    public enum DemoEnum {
        REAL;
    }

    public static class SubDemoConfiguration extends ProcessConfiguration {
        @Getter
        @Setter
        private int config1 = -1;

        @ProcessConfigurationProperty(customName = "custom")
        private String advisoryTypes = new JSONArray()
                .put(OSV_GENERIC_IDENTIFIER.toJson()).toString();

        public List<CustomAdvisoryTypeIdentifier> getAdvisoryTypes() {
            return this.accessCachedProperty("custom", this.advisoryTypes, (s) -> Collections.unmodifiableList(CustomAdvisoryTypeIdentifier.parseAdvisoryProviders(s)));
        }

        public void setAdvisoryTypes(List<CustomAdvisoryTypeIdentifier> advisoryTypes) {
            final JSONArray json = new JSONArray();
            for(CustomAdvisoryTypeIdentifier advisoryTypeIdentifier : advisoryTypes) {
                json.put(advisoryTypeIdentifier.toJson());
            }

            this.advisoryTypes = json.toString();
        }

    }
}
