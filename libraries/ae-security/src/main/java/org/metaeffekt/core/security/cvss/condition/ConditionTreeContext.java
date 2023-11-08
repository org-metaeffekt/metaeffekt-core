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
package org.metaeffekt.core.security.cvss.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConditionTreeContext {

    private final static Logger LOG = LoggerFactory.getLogger(ConditionTreeContext.class);

    private final Object values;

    public ConditionTreeContext(Map<String, Object> values) {
        this.values = values;
    }

    public ConditionTreeContext(List<Object> values) {
        this.values = values;
    }

    public Object getObject(Object[] objectSelector) {
        return getObject(objectSelector, 0, this.values);
    }

    protected Object getObject(Object[] objectSelector, int index, Object currentObject) {
        if (index >= objectSelector.length) {
            return currentObject;

        } else if (objectSelector[index] instanceof String) {
            if (currentObject instanceof Map) {
                final Map map = (Map) currentObject;
                final String accessKey = (String) objectSelector[index];
                if (!map.containsKey(accessKey)) {
                    LOG.warn("Key [{}] not found on object [{}] with object selector {}, returning null",
                            objectSelector[index], currentObject, Arrays.toString(objectSelector));
                    return null;
                }
                return getObject(objectSelector, index + 1, map.get(accessKey));
            } else {
                LOG.warn("Cannot access map element [{}] on object [{}] with object selector {}, returning null",
                        objectSelector[index], currentObject, Arrays.toString(objectSelector));
                return null;
            }

        } else if (objectSelector[index] instanceof Integer) {
            if (currentObject instanceof List) {
                final List list = (List) currentObject;
                final Integer accessIndex = (Integer) objectSelector[index];
                if (list.size() <= accessIndex) {
                    LOG.warn("Index out of bounds on list element [{}] on object [{}] with object selector {}, returning null",
                            objectSelector[index], currentObject, Arrays.toString(objectSelector));
                    return null;
                }
                return getObject(objectSelector, index + 1, list.get(accessIndex));
            } else {
                LOG.warn("Cannot access list element [{}] on object [{}] with object selector {}, returning null",
                        objectSelector[index], currentObject, Arrays.toString(objectSelector));
                return null;
            }
        } else {
            throw new IllegalArgumentException("Illegal object selector element type [" + objectSelector[index].getClass().getSimpleName() + "] of [" + objectSelector[index] + "] on object [" + currentObject + "] with object selector " + Arrays.toString(objectSelector));
        }
    }
}
