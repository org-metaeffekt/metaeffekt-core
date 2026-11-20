/*
 * Copyright 2009-2026 the original author or authors.
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
package org.metaeffekt.core.document.model;

// FIXME-RTU: consider renaming this class and review types of parts, do the same with DocumentType
public enum DocumentPartType {

    // Software distribution annex part for license and license obligation documentation
    ANNEX,

    // Initial license documentation part with focus on associated license reporting
    INITIAL_LICENSE_DOCUMENTATION,

    // Software license documentation similar to ANNEX, but with an emphasis on licenses
    LICENSE_DOCUMENTATION,

    // Lists vulnerabilities (overview and details)
    VULNERABILITY_REPORT,

    // Lists exploitable vulnerabilities
    EXPLOITABILITY_REPORT,

    // Provides a summary of the vulnerabilities; statistics
    VULNERABILITY_SUMMARY_PART,

    // Provides a summary of the vulnerabilities; asset/vulnerability tables
    VULNERABILITY_SUMMARY_REPORT,

    // Details vulnerability statistics
    VULNERABILITY_STATISTICS_REPORT,

    // Details vulnerability exploitability statistics
    EXPLOITABILITY_STATISTICS_REPORT,

    // Context part of a document
    CONTEXT,

    // Purpose part of a document
    PURPOSE,

    // Notice part of a document
    NOTICE

}
