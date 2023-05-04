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
