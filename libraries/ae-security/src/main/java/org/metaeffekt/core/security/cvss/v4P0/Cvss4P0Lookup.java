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
package org.metaeffekt.core.security.cvss.v4P0;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Access to the <code>cvss/cvss-4.0-mv-lookup.json</code> resource.<br>
 * Sourced from
 * <a href="https://github.com/RedHatProductSecurity/cvss-v4-calculator/blob/main/cvss_lookup.js">https://github.com/RedHatProductSecurity/cvss-v4-calculator/blob/main/cvss_lookup.js</a>.
 */
public class Cvss4P0Lookup {

    private final static Map<String, Double> MACRO_VECTOR_LOOKUP_TABLE;

    static {
        MACRO_VECTOR_LOOKUP_TABLE = new LinkedHashMap<>();

        final String path = "cvss/cvss-4.0-mv-lookup.json";
        try (InputStream inputStream = Cvss4P0Lookup.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("Cannot find resource: " + path);
            }
            final String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            final JSONObject jsonObject = new JSONObject(json);
            jsonObject.keySet().forEach(key -> {
                final double score = jsonObject.getDouble(key);
                MACRO_VECTOR_LOOKUP_TABLE.put(key, score);
            });
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read resource: " + path, e);
        }
    }

    public static double getMacroVectorScore(String macroVector) {
        return MACRO_VECTOR_LOOKUP_TABLE.getOrDefault(macroVector, Double.NaN);
    }

    public static double getMacroVectorScore(Cvss4P0MacroVector macroVector) {
        return MACRO_VECTOR_LOOKUP_TABLE.getOrDefault(macroVector.toString(), Double.NaN);
    }
}
