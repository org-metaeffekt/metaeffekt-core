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

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AeaaMitre {

    public enum Severity {
        VERY_HIGH("Very High"),
        HIGH("High"),
        MEDIUM("Medium"),
        LOW("Low"),
        VERY_LOW("Very Low"),
        UNKNOWN("Unknown");

        private static final Map<String, Severity> map = new HashMap<>(values().length, 1);

        static {
            for (Severity c : values()) map.put(c.severity, c);
        }

        private final String severity;

        Severity(String severity) {
            this.severity = severity;
        }

        public static Severity of(String name) {
            Severity result = map.get(name);
            if (result == null) {
                log.error("Unknown severity level: {}", name);
            }
            return result;
        }

        @Override
        public String toString() {
            return severity;
        }
    }

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

        private static final Map<String, Scope> map = new HashMap<>(values().length, 1);

        static {
            for (Scope c : values()) map.put(c.scope, c);
        }

        private final String scope;

        Scope(String category) {
            this.scope = category;
        }

        public static Scope of(String name) {
            Scope result = map.get(name);
            if (result == null) {
                log.error("Unknown scope: {}", name);
            }
            return result;
        }

        @Override
        public String toString() {
            return scope;
        }
    }

    public enum Status {
        DEPRECATED("Deprecated"),
        DRAFT("Draft"),
        INCOMPLETE("Incomplete"),
        OBSOLETE("Obsolete"),
        STABLE("Stable"),
        USABLE("Usable"),
        UNKNOWN("Unknown");

        private static final Map<String, Status> map = new HashMap<>(values().length, 1);

        static {
            for (Status c : values()) map.put(c.status, c);
        }

        private final String status;

        Status(String category) {
            this.status = category;
        }

        public static Status of(String name) {
            Status result = map.get(name);
            if (result == null) {
                log.error("Unknown status: {}", name);
            }
            return result;
        }

        @Override
        public String toString() {
            return status;
        }
    }

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

        private static final Map<String, Relation> map = new HashMap<>(values().length, 1);

        static {
            for (Relation c : values()) map.put(c.relation, c);
        }

        private final String relation;

        Relation(String category) {
            this.relation = category;
        }

        public static Relation of(String name) {
            Relation result = map.get(name);
            if (result == null) {


                log.error("Unknown relation: {}", name);
            }
            return result;
        }

        @Override
        public String toString(){
            return relation;
        }
    }

    public enum Importance {
        NORMAL("Normal"),
        Critical("Critical");

        private static final Map<String, Importance> map = new HashMap<>(values().length, 1);

        static {
            for (Importance c : values()) map.put(c.importance, c);
        }

        private final String importance;

        Importance(String category) {
            this.importance = category;
        }

        public static Importance of(String name) {
            Importance result = map.get(name);
            if (result == null) {
                log.error("Unknown importance level: {}", name);
            }
            return result;
        }

        @Override
        public String toString() {
            return importance;
        }
    }

    public enum Skill {
        HIGH("High"),
        MEDIUM("Medium"),
        LOW("Low"),
        UNKNOWN("Unknown");

        private static final Map<String, Skill> map = new HashMap<>(values().length, 1);

        static {
            for (Skill c : values()) map.put(c.skill, c);
        }

        private final String skill;

        Skill(String skill) {
            this.skill = skill;
        }

        public static Skill of(String name) {
            Skill result = map.get(name);
            if (result == null) {
                log.error("Unknown skill level: {}", name);
            }
            return result;
        }

        @Override
        public String toString() {
            return skill;
        }
    }
}
