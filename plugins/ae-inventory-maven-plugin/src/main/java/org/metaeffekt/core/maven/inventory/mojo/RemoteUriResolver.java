package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugins.annotations.Parameter;

import java.util.Map;

public class RemoteUriResolver {

    @Parameter
    private Map<String, Object> properties;

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

}
