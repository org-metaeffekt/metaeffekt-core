package org.metaeffekt.core.inventory.processor.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.JSONArray;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;

import java.util.Collections;
import java.util.List;

@NoArgsConstructor
public class DemoConfiguration extends ProcessConfiguration {

    @Getter
    @Setter
    private int number = -1;

    @ConfigurableProcessProperty(customName = "custom", converter = DemoClassConverter.class)
    private String advisoryTypes = new JSONArray()
            .put(AeaaAdvisoryTypeStore.OSV_GENERIC_IDENTIFIER.toJson()).toString();


    @ExcludedProcessProperty
    private String ignoreProperty = "ignored";

    private DemoEnum demoEnum = DemoEnum.REAL;

    private SubDemoConfiguration subDemoConfiguration = new SubDemoConfiguration();


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

    @Override
    public void collectMisconfigurations(List<ProcessMisconfiguration> misconfigurations) {

    }

    public static class DemoClassConverter implements ConfigurationSerializer<JSONArray, String> {

        @Override
        public String serialize(JSONArray internal) {
            return internal.toString();
        }

        @Override
        public JSONArray deserialize(String external) {
            return new JSONArray(external);
        }
    }


    public enum DemoEnum {
        REAL;
    }

    public static class SubDemoConfiguration extends ProcessConfiguration {

        @Getter
        @Setter
        private int config1 = -1;

        @ConfigurableProcessProperty(customName = "custom", converter = DemoClassConverter.class)
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
