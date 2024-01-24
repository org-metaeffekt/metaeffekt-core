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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.msrc;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.msrcdata.MsrcProduct</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public class AeaaMsrcProduct {

    private final static Logger LOG = LoggerFactory.getLogger(AeaaMsrcProduct.class);

    private final String id, name, vendor, family;

    public AeaaMsrcProduct(String id, String name, String vendor, String family) {
        if (StringUtils.isEmpty(id)) {
            LOG.warn("Product ID is empty on MS product");
        }

        this.id = id;
        this.name = name;
        this.vendor = vendor;
        this.family = family;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVendor() {
        return vendor;
    }

    public String getFamily() {
        return family;
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();

        json.put("id", id);
        json.put("name", name);
        json.put("vendor", vendor);
        json.put("family", family);

        return json;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static AeaaMsrcProduct fromJson(JSONObject json) {
        return new AeaaMsrcProduct(
                json.optString("id", null),
                json.optString("name", null),
                json.optString("vendor", null),
                json.optString("family", null)
        );
    }
}
