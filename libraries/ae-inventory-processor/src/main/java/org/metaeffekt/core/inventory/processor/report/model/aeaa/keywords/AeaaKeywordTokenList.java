/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.keywords;

import org.json.JSONArray;

import java.util.Arrays;
import java.util.Collection;

public class AeaaKeywordTokenList {

    private final String[] tokens;

    public AeaaKeywordTokenList(String[] tokens) {
        this.tokens = tokens;
    }

    public AeaaKeywordTokenList(Collection<String> tokens) {
        this.tokens = tokens.toArray(new String[0]);
    }

    @Override
    public String toString() {
        return "[" + String.join(", ", tokens) + "]";
    }

    public JSONArray toJson() {
        return new JSONArray(Arrays.asList(tokens));
    }

    public static AeaaKeywordTokenList fromJson(JSONArray jsonArray) {
        return new AeaaKeywordTokenList(jsonArray.toList().toArray(new String[0]));
    }
}
