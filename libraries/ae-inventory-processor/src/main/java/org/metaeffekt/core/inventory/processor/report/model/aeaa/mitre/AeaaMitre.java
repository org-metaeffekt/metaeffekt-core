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

package org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AeaaMitre {

    @AllArgsConstructor
    public enum Severity {
        VERY_HIGH("Very High"),
        HIGH("High"),
        MEDIUM("Medium"),
        LOW("Low"),
        VERY_LOW("Very Low"),
        UNKNOWN("Unknown");

        private final String severity;

        public static Severity of(String severity) {
            for (Severity value : values()) {
                if (value.severity.equalsIgnoreCase(severity)) {
                    return value;
                }
            }
            log.error("Could not find CWE severity level [{}], expected one of {}", severity, values());
            return null;
        }

        @Override
        public String toString() {
            return severity;
        }
    }

    @AllArgsConstructor
    public enum Scope {
        CONFIDENTIALITY("Confidentiality"),
        INTEGRITY("Integrity"),
        AVAILABILITY("Availability"),
        ACCESS_CONTROL("Access Control"),
        ACCOUNTABILITY("Accountability"),
        AUTHENTICATION("Authentication"),
        AUTHORIZATION("Authorization"),
        NON_REPUDIATION("Non-Repudiation"),
        OTHER("Other");

        private final String scope;

        public static Scope of(String scope) {
            for (Scope value : values()) {
                if (value.scope.equalsIgnoreCase(scope)) {
                    return value;
                }
            }
            log.error("Could not find CWE scope [{}], expected one of {}", scope, values());
            return null;
        }

        @Override
        public String toString() {
            return scope;
        }
    }

    @AllArgsConstructor
    public enum Status {
        DEPRECATED("Deprecated"),
        DRAFT("Draft"),
        INCOMPLETE("Incomplete"),
        OBSOLETE("Obsolete"),
        STABLE("Stable"),
        USABLE("Usable"),
        UNKNOWN("Unknown");

        private final String status;

        public static Status of(String status) {
            for (Status value : values()) {
                if (value.status.equalsIgnoreCase(status)) {
                    return value;
                }
            }
            log.error("Could not find CWE status [{}], expected one of {}", status, values());
            return null;
        }

        @Override
        public String toString() {
            return status;
        }
    }

    @AllArgsConstructor
    public enum Relation {
        CHILD_OF("ChildOf"),
        PARENT_OF("ParentOf"),
        PEER_OF("PeerOf"),
        CAN_PRECEDE("CanPrecede"),
        CAN_FOLLOW("CanFollow"),
        CAN_ALSO_BE("CanAlsoBe"),
        STARTS_WITH("StartsWith"),
        REQUIRED_BY("RequiredBy"),
        REQUIRES("Requires"),
        CWE("CWE"),
        CAPEC("CAPEC");

        private final String relation;

        public static Relation of(String relation) {
            for (Relation value : values()) {
                if (value.relation.equalsIgnoreCase(relation)) {
                    return value;
                }
            }
            log.error("Could not find CWE relation type [{}], expected one of {}", relation, values());
            return null;
        }

        @Override
        public String toString() {
            return relation;
        }
    }

    // FIXME-JKO: Importance enum is never used.
    @AllArgsConstructor
    public enum Importance {
        NORMAL("Normal"),
        CRITICAL("Critical");

        private final String importance;

        public static Importance of(String importance) {
            for (Importance value : values()) {
                if (value.importance.equalsIgnoreCase(importance)) {
                    return value;
                }
            }
            log.error("Could not find CWE importance level [{}], expected one of {}", importance, values());
            return null;
        }

        @Override
        public String toString() {
            return importance;
        }
    }

    // FIXME-JKO: Skill enum is never used.
    @AllArgsConstructor
    public enum Skill {
        HIGH("High"),
        MEDIUM("Medium"),
        LOW("Low"),
        UNKNOWN("Unknown");

        private final String skill;

        public static Skill of(String skill) {
            for (Skill value : values()) {
                if (value.skill.equalsIgnoreCase(skill)) {
                    return value;
                }
            }
            log.error("Could not find CWE skill [{}], expected one of {}", skill, values());
            return null;
        }

        @Override
        public String toString() {
            return skill;
        }
    }
}
