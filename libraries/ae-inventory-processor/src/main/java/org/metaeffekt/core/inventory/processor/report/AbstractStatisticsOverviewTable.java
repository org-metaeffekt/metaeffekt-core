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
package org.metaeffekt.core.inventory.processor.report;

import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractStatisticsOverviewTable<R extends AbstractStatisticsOverviewTable.AbstractSeverityToStatusRow> {

    protected final List<R> rows = new ArrayList<>();
    protected final boolean usesEffectiveSeverity;

    public AbstractStatisticsOverviewTable(boolean usesEffectiveSeverity) {
        this.usesEffectiveSeverity = usesEffectiveSeverity;
    }

    public List<R> getRows() {
        return rows;
    }

    public boolean isUsesEffectiveSeverity() {
        return usesEffectiveSeverity;
    }

    public abstract R findOrCreateRowBySeverity(CentralSecurityPolicyConfiguration securityPolicy, String severity);

    public R findRowBySeverity(String severity) {
        final String normalizedSeverity = normalize(severity);
        return rows.stream()
                .filter(row -> row.isSeverity(normalizedSeverity))
                .findFirst()
                .orElse(null);
    }

    public void incrementCount(CentralSecurityPolicyConfiguration securityPolicy, String severity, String status) {
        if (severity == null || status == null) {
            log.warn("Severity [{}] or status [{}] is null. Skipping incrementCount.", severity, status);
            return;
        }

        final String normalizedSeverity = normalize(severity);
        final String normalizedStatus = normalize(status);

        final R row = findOrCreateRowBySeverity(securityPolicy, normalizedSeverity);
        row.incrementCount(normalizedStatus);
    }

    public abstract List<String> getHeaders();

    public List<String> getSeverityCategories() {
        return rows.stream().map(AbstractSeverityToStatusRow::getSeverity).map(AbstractStatisticsOverviewTable::capitalizeWords).collect(Collectors.toList());
    }

    public int getIntersectionCount(String severity, String status) {
        final R row = findRowBySeverity(severity);
        if (row == null) return 0;
        return row.getCount(status);
    }

    public int getStatusCount(String status) {
        return rows.stream().mapToInt(row -> row.getCount(status)).sum();
    }

    public abstract List<String> getTableRowValues(String severity);

    public boolean isEmpty() {
        return rows.stream().allMatch(row -> row.getTotal() == 0);
    }

    protected static String normalize(String s) {
        return s.toLowerCase();
    }

    public static String capitalizeWords(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Arrays.stream(s.split(" "))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    public abstract String getColumnWidth(int index);
    public abstract String getHeaderAlignment(int index);
    public abstract String getAlignment(int index);

    public static abstract class AbstractSeverityToStatusRow {
        protected final String severity;
        protected final Map<String, Integer> statusCountMap = new LinkedHashMap<>();
        protected int total = 0;

        public AbstractSeverityToStatusRow(String severity) {
            if (severity == null) {
                throw new IllegalArgumentException("Severity must not be null when constructing row.");
            }
            this.severity = severity;
        }

        public String getSeverity() {
            return this.severity;
        }

        public String getCapitalizedSeverity() {
            return capitalizeWords(this.severity);
        }

        public Map<String, Integer> getStatusCountMap() {
            return this.statusCountMap;
        }

        public Set<String> keySet() {
            return this.statusCountMap.keySet();
        }

        public int getTotal() {
            return this.total;
        }

        public int getCount(String status) {
            final String normalizedStatus = normalize(status);
            return this.statusCountMap.getOrDefault(normalizedStatus, 0);
        }

        public abstract void updateCalculatedColumns(CentralSecurityPolicyConfiguration securityPolicy);

        protected int calculateTotal() {
            return this.statusCountMap.values().stream().mapToInt(Integer::intValue).sum();
        }

        public boolean isSeverity(String severity) {
            return Objects.equals(this.severity, severity);
        }

        public int incrementCount(String status) {
            final String normalizedStatus = normalize(status);
            final int newValue = this.statusCountMap.getOrDefault(normalizedStatus, 0) + 1;
            this.statusCountMap.put(normalizedStatus, newValue);
            return newValue;
        }

        public void rearrangeStatusCategories(List<String> statusCategories) {
            final Map<String, Integer> rearrangedStatusCountMap = new LinkedHashMap<>();
            for (String statusCategory : statusCategories) {
                final String normalizedStatusCategory = normalize(statusCategory);
                rearrangedStatusCountMap.put(normalizedStatusCategory, this.statusCountMap.getOrDefault(normalizedStatusCategory, 0));
            }
            for (String statusCategory : this.statusCountMap.keySet()) {
                if (!rearrangedStatusCountMap.containsKey(statusCategory)) {
                    rearrangedStatusCountMap.put(statusCategory, this.statusCountMap.getOrDefault(statusCategory, 0));
                }
            }
            this.statusCountMap.clear();
            this.statusCountMap.putAll(rearrangedStatusCountMap);
        }

        public abstract List<String> getTableRowValues();

        @Override
        public String toString() {
            return "SeverityToStatusRow{" +
                    "severity='" + severity + '\'' +
                    ", statusCountMap=" + statusCountMap +
                    '}';
        }
    }
}
