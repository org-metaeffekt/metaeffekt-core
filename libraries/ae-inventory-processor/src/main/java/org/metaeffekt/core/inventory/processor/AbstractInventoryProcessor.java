/**
 * Copyright 2009-2018 the original author or authors.
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
package org.metaeffekt.core.inventory.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractInventoryProcessor implements InventoryProcessor {

    public static final String FAIL_ON_ERROR = "failOnError";

    /**
     * The contextMap stores results are intermediate computations.
     */
    private final Map<String, Object> contextMap = new HashMap<String, Object>();

    /**
     * Properties can be used to configure the processor.
     */
    private Properties properties = new Properties(System.getProperties());

    public AbstractInventoryProcessor(Properties properties) {
        this.properties = properties;
    }

    public AbstractInventoryProcessor() {
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Map<String, Object> getContextMap() {
        return contextMap;
    }

    public boolean isFailOnError() {
        return Boolean.parseBoolean(getProperties().
                getProperty(FAIL_ON_ERROR, Boolean.TRUE.toString()));
    }

}
