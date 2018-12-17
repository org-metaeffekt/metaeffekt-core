package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Map;

public abstract class IdentifiableComponent {

    @Parameter(required = true)
    private String id;

    @Parameter
    private Map<String, Object> properties;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    protected void dumpConfig(Log log, String prefix) {
        log.debug(prefix + getClass().getSimpleName());
        log.debug(prefix + "  id: " + getId());
        log.debug(prefix + "  properties: " + getProperties());
    }

    protected String extractPattern(int index, String[] split) {
        String value = "^.*";
        if (split.length > index) {
            String part = split[index];
            if (part != null) {
                part = part.trim();
                if (!StringUtils.isEmpty(part)) {
                    value = part;
                }
            }
        }
        return value;
    }
}
