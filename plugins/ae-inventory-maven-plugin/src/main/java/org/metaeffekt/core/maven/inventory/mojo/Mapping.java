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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Mapping {

    private static final String SEPARATOR = "-->";

    private List<String> mappings = new ArrayList<String>();

    public Map<String, String> getMap() {
        Map<String, String> map = new HashMap<String, String>();
        if (mappings != null) {
            for (String mapping : mappings) {
                int index = mapping.indexOf(SEPARATOR);
                if (index != -1) {
                    final String key = mapping.substring(0, index);
                    final String value = mapping.substring(index + SEPARATOR.length(), mapping.length());
                    map.put(key.trim(), value.trim());
                }
            }
        }
        return map;
    }

    public void setMapping(String string) {
        this.mappings.add(string);
    }

    public List<String> getMappings() {
        return mappings;
    }

}
