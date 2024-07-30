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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;

/**
 * Mirrors structure of <code>com.metaeffekt.artifact.analysis.vulnerability.enrichment.AeaaInventoryAttribute</code>
 * until separation of inventory report generation from ae core inventory processor.
 * <p>
 * More attributes for the inventory artifacts that are not in core.
 */
public enum AeaaInventoryAttribute implements AbstractModelBase.Attribute {
    REVIEWED_ADVISORIES("Reviewed Advisories"),
    /**
     * The vulnerability description text.
     */
    DESCRIPTION("Description"),
    /**
     * Contains a JSON Object representing a vulnerability status.
     */
    VULNERABILITY_STATUS("Vulnerability Status"),
    STATUS_TITLE("Title"),
    STATUS_HISTORY("Status history"),
    STATUS_ACCEPTED("Accepted by"),
    STATUS_REPORTED("Reported by"),
    MS_PRODUCT_ID("MS Product ID"),
    MS_REMEDIATIONS("MS Remediations"),
    MS_AFFECTED_PRODUCTS("MS Affected Products"),
    MS_THREATS("MS Threats"),
    MS_FIXING_KB_IDENTIFIER("MS Fixing Knowledge Base ID"),
    /**
     * CSV attribute for marking entries with tags.
     * <ul>
     *     <li><code>marker</code></li>
     *     <li><code>added by status</code></li>
     * </ul>
     */
    TAGS("Tags"),
    VULNERABILITY_UPDATED_DATE_TIMESTAMP("Last Updated Timestamp"),
    VULNERABILITY_UPDATED_DATE_FORMATTED("Last Updated Date"),
    VULNERABILITY_CREATED_DATE_TIMESTAMP("Created Timestamp"),
    VULNERABILITY_CREATED_DATE_FORMATTED("Created Date"),
    /**
     * Stores IDs of referenced content, such as advisories.
     */
    @Deprecated
    VULNERABILITY_REFERENCED_CONTENT_IDS("Referenced Ids"),
    MATCHES_ON_MS_PRODUCT_ID(MS_PRODUCT_ID.getKey()),
    VULNERABILITIES_FIXED_BY_KB("Vulnerability fixed by KB"),
    INAPPLICABLE_CVE("Inapplicable CVE"),
    ADDON_CVES("Addon CVEs"),
    KEV_DATA("KEV Data"),
    EPSS_DATA("EPSS Data");

    private final String key;

    AeaaInventoryAttribute(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
