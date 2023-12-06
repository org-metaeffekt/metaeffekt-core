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
package org.metaeffekt.core.inventory.processor.report.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvisoryUtils {

    private static final Logger LOG = LoggerFactory.getLogger(AdvisoryUtils.class);

    public final static Map<String, List<String>> TYPE_NORMALIZATION_MAP = new HashMap<String, List<String>>() {{
        put("notice", Arrays.asList(
                "notice", "info", "Description", "tag",
                "avis", "ioc", "cti", "information" // CERT-FR
        ));
        put("alert", Arrays.asList(
                "alert", "advisory", "cna", "compromise indicators",
                "hardening and recommendations", "threats and incidents",
                "alerte" // CERT-FR
        ));
        put("news", Arrays.asList(
                "news",
                "actualite", "dur" // CERT-FR
        ));
    }};

    public static String normalizeType(String type) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(type)) {
            return "notice";
        }

        final String inputNormalizedType = type.toLowerCase().trim();

        for (Map.Entry<String, List<String>> entry : TYPE_NORMALIZATION_MAP.entrySet()) {
            final String normalizedType = entry.getKey();
            final List<String> typeList = entry.getValue();

            for (String t : typeList) {
                if (inputNormalizedType.equalsIgnoreCase(t)) {
                    return normalizedType;
                }
            }
        }

        return "notice";
    }
}
