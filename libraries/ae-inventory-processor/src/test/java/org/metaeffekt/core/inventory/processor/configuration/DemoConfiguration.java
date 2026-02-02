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
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaContentIdentifierStore;

import java.util.Collections;
import java.util.List;

@NoArgsConstructor
public class DemoConfiguration extends ProcessConfiguration {

    @Getter
    @Setter
    private int number = -1;

    @ProcessConfigurationProperty(alternativeNames = {"Deprecated Field Name"}, converter = JsonArrayConverter.class)
    private String advisoryTypes = new JSONArray()
            .put(AeaaAdvisoryTypeStore.OSV_GENERIC_IDENTIFIER.toJson()).toString();

    @ExcludeProcessConfigurationProperty
    private String ignoreProperty = "ignored";

    @Getter
    @Setter
    private DemoEnum demoEnum = DemoEnum.REAL;

    @Getter
    private final SubDemoConfiguration subDemoConfiguration = new SubDemoConfiguration();

    public List<AeaaAdvisoryTypeIdentifier<?>> getAdvisoryTypes() {
        return accessCachedProperty("custom", advisoryTypes, (s) -> Collections.unmodifiableList(AeaaAdvisoryTypeStore.parseAdvisoryProviders(s)));
    }

    public void setAdvisoryTypes(List<AeaaAdvisoryTypeIdentifier<?>> advisoryTypes) {
        this.advisoryTypes = AeaaContentIdentifierStore.AeaaContentIdentifier.toJsonArray(advisoryTypes).toString();
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
                .put(AeaaAdvisoryTypeStore.OSV_GENERIC_IDENTIFIER.toJson()).toString();

        public List<AeaaAdvisoryTypeIdentifier<?>> getAdvisoryTypes() {
            return accessCachedProperty("custom", advisoryTypes, (s) -> Collections.unmodifiableList(AeaaAdvisoryTypeStore.parseAdvisoryProviders(s)));
        }

        public void setAdvisoryTypes(List<AeaaAdvisoryTypeIdentifier<?>> advisoryTypes) {
            JSONArray json = new JSONArray();
            for(AeaaAdvisoryTypeIdentifier<?> advisoryTypeIdentifier : advisoryTypes) {
                json.put(advisoryTypeIdentifier.toJson());
            }

            this.advisoryTypes = json.toString();
        }

    }
}
