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
package org.metaeffekt.core.inventory.processor.report;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaVulnerability;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.vulnerabilitystatus.AeaaVulnerabilityStatusHistoryEntry;
import org.metaeffekt.core.security.cvss.CvssSeverityRanges;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 * Severity │ Applicable │ Not Applicable │ In Review │ Insignificant │ Void │ Total │ Assessed
 * ─────────┼────────────┼────────────────┼───────────┼───────────────┼──────┼───────┼─────────
 * Critical │          0 │              0 │         0 │             0 │    0 │     0 │      n/a
 *     High │          0 │              0 │         9 │             0 │    1 │    10 │   10,0 %
 *   Medium │          0 │              0 │       184 │             0 │    0 │   184 │    0,0 %
 *      Low │          0 │              0 │        38 │             0 │    0 │    38 │    0,0 %
 * </pre>
 */
public class StatisticsOverviewTable {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsOverviewTable.class);

    private final List<SeverityToStatusRow> rows = new ArrayList<>();
    private final boolean usesEffectiveSeverity;

    public StatisticsOverviewTable(boolean usesEffectiveSeverity) {
        this.usesEffectiveSeverity = usesEffectiveSeverity;
    }

    public List<SeverityToStatusRow> getRows() {
        return rows;
    }

    public boolean isUsesEffectiveSeverity() {
        return usesEffectiveSeverity;
    }

    protected void incrementCount(CentralSecurityPolicyConfiguration securityPolicy, String severity, String status) {
        if (severity == null || status == null) {
            LOG.warn("Severity [{}] or status [{}] is null. Skipping incrementCount.", severity, status);
            return;
        }

        final String normalizedSeverity = normalize(severity);
        final String normalizedStatus = normalize(status);

        final SeverityToStatusRow row = findOrCreateRowBySeverity(securityPolicy, normalizedSeverity);
        row.incrementCount(normalizedStatus);
    }

    protected SeverityToStatusRow findOrCreateRowBySeverity(CentralSecurityPolicyConfiguration securityPolicy, String severity) {
        final String normalizedSeverity = normalize(severity);
        return rows.stream()
                .filter(row -> row.isSeverity(normalizedSeverity))
                .findFirst()
                .orElseGet(() -> {
                    final SeverityToStatusRow row = new SeverityToStatusRow(securityPolicy, normalizedSeverity);
                    this.rows.add(row);
                    return row;
                });
    }

    public SeverityToStatusRow findRowBySeverity(String severity) {
        final String normalizedSeverity = normalize(severity);
        return rows.stream()
                .filter(row -> row.isSeverity(normalizedSeverity))
                .findFirst()
                .orElse(null);
    }

    public static StatisticsOverviewTable buildTableStrFilterAdvisor(CentralSecurityPolicyConfiguration securityPolicy, Collection<AeaaVulnerability> inputVulnerabilities, String filterAdvisory, boolean useEffectiveSeverityScores) {
        final AeaaAdvisoryTypeIdentifier<?> filterCertAsAeaaContentIdentifiers = StringUtils.isEmpty(filterAdvisory) ? null : AeaaAdvisoryTypeStore.get().fromNameWithoutCreation(filterAdvisory);
        return buildTable(securityPolicy, inputVulnerabilities, filterCertAsAeaaContentIdentifiers, useEffectiveSeverityScores);
    }

    public static StatisticsOverviewTable buildTable(CentralSecurityPolicyConfiguration securityPolicy, Collection<AeaaVulnerability> inputVulnerabilities, AeaaAdvisoryTypeIdentifier<?> filterAdvisory, boolean useEffectiveSeverity) {
        final StatisticsOverviewTable table = new StatisticsOverviewTable(useEffectiveSeverity);

        final Collection<AeaaVulnerability> effectiveVulnerabilities;
        if (filterAdvisory != null) {
            effectiveVulnerabilities = CentralSecurityPolicyConfiguration.filterVulnerabilitiesForAdvisoryProviders(inputVulnerabilities, Collections.singleton(filterAdvisory));
        } else {
            effectiveVulnerabilities = inputVulnerabilities;
        }

        final List<String> requiredSeverityCategories = Arrays.stream(securityPolicy.getCvssSeverityRanges().getRanges())
                .sorted(Comparator.reverseOrder())
                .map(CvssSeverityRanges.SeverityRange::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        final List<String> requiredStatusCategories = securityPolicy.getVulnerabilityStatusDisplayMapper().getStatusNames();

        // add the default severity categories
        for (String severityCategory : requiredSeverityCategories) {
            table.findOrCreateRowBySeverity(securityPolicy, severityCategory);
        }

        // count the individual severity and status combinations
        for (AeaaVulnerability vulnerability : effectiveVulnerabilities) {

            // status in effectiveStatus
            final AeaaVulnerabilityStatusHistoryEntry latestStatusEntry = vulnerability.getOrCreateNewVulnerabilityStatus().getLatestActiveStatusHistoryEntry();
            final String baseStatus = latestStatusEntry != null && latestStatusEntry.getStatus() != null ? latestStatusEntry.getStatus() : "";
            final String effectiveStatus = securityPolicy.getVulnerabilityStatusDisplayMapper().getMapper().apply(baseStatus);

            // severity in effectiveSeverity
            final CvssVector vector = useEffectiveSeverity ? vulnerability.getCvssSelectionResult().getSelectedContextIfAvailableOtherwiseInitial() : vulnerability.getCvssSelectionResult().getSelectedInitialCvss();
            final String effectiveSeverity;
            if (vector == null || (useEffectiveSeverity && (VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE.equals(baseStatus) || VulnerabilityMetaData.STATUS_VALUE_VOID.equals(baseStatus)))) {
                effectiveSeverity = "none";
            } else {
                final double overallScore = vector.getBakedScores().getOverallScore();
                final CvssSeverityRanges.SeverityRange severityRange = securityPolicy.getCvssSeverityRanges().getRange(overallScore);
                effectiveSeverity = severityRange.getName();
            }

            table.incrementCount(securityPolicy, effectiveSeverity, effectiveStatus);
        }

        table.getRows().forEach(row -> row.updateCalculatedColumns(securityPolicy));

        // remove 'none' if all but the 'assessed' column are 0
        final SeverityToStatusRow noneEntry = table.findRowBySeverity("none");
        if (noneEntry != null) {
            // remove the 'none' entry if the content does not contribute any further
            // information. That is when only 0s are included.
            // n/a cannot occur here, as the queried values are all integers.
            final boolean allZero = noneEntry.getStatusCountMap().values().stream().allMatch(i -> i == 0);
            if (allZero) {
                table.rows.remove(noneEntry);
            }
        }

        table.getRows().forEach(row -> row.rearrangeStatusCategories(requiredStatusCategories));

        LOG.debug("Generated {} Overview Table for [{}] vulnerabilities with{}:\n{}", useEffectiveSeverity ? "effective" : "provided", effectiveVulnerabilities.size(), filterAdvisory == null ? "out advisory filtering" : " advisory filtering using [" + filterAdvisory + "]", table);
        return table;
    }

    public List<String> getHeaders() {
        final List<String> headers = new ArrayList<>();
        headers.add("severity");

        final Set<String> severityHeadersFromRows = new LinkedHashSet<>();
        for (SeverityToStatusRow row : rows) {
            severityHeadersFromRows.addAll(row.keySet());
        }
        headers.addAll(severityHeadersFromRows);

        headers.add("total");
        headers.add("assessed");

        return headers.stream().map(StatisticsOverviewTable::capitalizeWords).collect(Collectors.toList());
    }

    public List<String> getSeverityCategories() {
        return rows.stream().map(SeverityToStatusRow::getSeverity).map(StatisticsOverviewTable::capitalizeWords).collect(Collectors.toList());
    }

    public int getIntersectionCount(String severity, String status) {
        final SeverityToStatusRow row = findRowBySeverity(severity);
        if (row == null) return 0;
        return row.getCount(status);
    }

    public int getStatusCount(String status) {
        return rows.stream().mapToInt(row -> row.getCount(status)).sum();
    }

    public List<String> getTableRowValues(String severity) {
        final SeverityToStatusRow row = findRowBySeverity(severity);
        if (row == null) return Collections.emptyList();
        final List<String> values = new ArrayList<>();
        values.add(row.getCapitalizedSeverity());
        for (String status : row.keySet()) {
            values.add(String.valueOf(row.getCount(status)));
        }
        values.add(String.valueOf(row.getTotal()));
        values.add(row.getAssessed());
        return values;
    }

    /**
     * Checks if the table is empty.<br>
     * The table is considered empty if all cells have the value "0".
     *
     * @return true if the table is empty, false otherwise.
     */
    public boolean isEmpty() {
        return rows.stream()
                .allMatch(row -> row.getTotal() == 0);
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

    @Override
    public String toString() {
        final List<SeverityToStatusRow> severityToStatusRows = getRows();
        final List<String> headers = getHeaders();

        final String[][] cells = new String[severityToStatusRows.size() + 1][headers.size()];

        // fill header
        for (int i = 0; i < headers.size(); i++) {
            cells[0][i] = headers.get(i);
        }

        // fill cells
        for (int i = 1; i < severityToStatusRows.size() + 1; i++) {
            final SeverityToStatusRow row = severityToStatusRows.get(i - 1);
            cells[i][0] = row.getCapitalizedSeverity();
            for (int j = 1; j < headers.size() - 2; j++) {
                cells[i][j] = String.valueOf(row.getCount(headers.get(j)));
            }
            cells[i][cells[i].length - 2] = String.valueOf(row.getTotal());
            cells[i][cells[i].length - 1] = row.getAssessed();
        }

        // calculate column widths
        final int[] columnWidths = new int[headers.size()];
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < columnWidths.length; j++) {
                columnWidths[j] = Math.max(columnWidths[j], cells[i][j] == null ? 4 : cells[i][j].length());
            }
        }

        // build table
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            final String[] row = cells[i];
            for (int j = 0; j < row.length; j++) {
                sb.append(String.format("%" + columnWidths[j] + "s", row[j]));
                if (j < row.length - 1) {
                    sb.append(" │ ");
                }
            }
            sb.append("\n");

            if (i == 0) {
                for (int j = 0; j < columnWidths.length; j++) {
                    sb.append(String.join("", Collections.nCopies(columnWidths[j], "─")));
                    if (j < columnWidths.length - 1) {
                        sb.append("─┼─");
                    }
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static class SeverityToStatusRow {
        private final String severity;
        private final Map<String, Integer> statusCountMap = new LinkedHashMap<>();
        private int total = 0;
        private int assessedCount = 0;
        private String assessed = "undefined";

        public SeverityToStatusRow(CentralSecurityPolicyConfiguration securityPolicy, String severity) {
            if (severity == null) {
                throw new IllegalArgumentException("Severity must not be null when constructing row.");
            }
            this.severity = severity;

            for (String requiredCategories : securityPolicy.getVulnerabilityStatusDisplayMapper().getStatusNames()) {
                this.statusCountMap.put(requiredCategories, 0);
            }
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

        public int getAssessedCount() {
            return assessedCount;
        }

        public String getAssessed() {
            return this.assessed;
        }

        public int getCount(String status) {
            final String normalizedStatus = normalize(status);
            return this.statusCountMap.getOrDefault(normalizedStatus, 0);
        }

        public void updateCalculatedColumns(CentralSecurityPolicyConfiguration securityPolicy) {
            this.assessed = calculateAssessed(securityPolicy);
            this.assessedCount = getExactAssessedCount(securityPolicy);
            this.total = calculateTotal();
        }

        private int calculateTotal() {
            return this.statusCountMap.values().stream().mapToInt(Integer::intValue).sum();
        }

        protected String calculateAssessed(CentralSecurityPolicyConfiguration securityPolicy) {
            int total = 0;
            for (String totalName : securityPolicy.getVulnerabilityStatusDisplayMapper().getStatusNames()) {
                total += this.statusCountMap.getOrDefault(totalName, 0);
            }

            final int assessed = getExactAssessedCount(securityPolicy);

            if (total == 0) {
                return "n/a";
            } else {
                final double ratio = ((double) assessed) / total;
                return String.format(Locale.GERMANY, "%.1f %%", ratio * 100);
            }
        }

        protected int getExactAssessedCount(CentralSecurityPolicyConfiguration securityPolicy) {
            int assessed = 0;
            for (String assessedName : securityPolicy.getVulnerabilityStatusDisplayMapper().getAssessedStatusNames()) {
                assessed += this.statusCountMap.getOrDefault(assessedName, 0);
            }
            return assessed;
        }

        public boolean isSeverity(String severity) {
            return Objects.equals(this.severity, severity);
        }

        public int incrementCount(String status) {
            final int newValue = this.statusCountMap.getOrDefault(status, 0) + 1;
            this.statusCountMap.put(status, newValue);
            return newValue;
        }

        public void rearrangeStatusCategories(List<String> statusCategories) {
            final Map<String, Integer> rearrangedStatusCountMap = new LinkedHashMap<>();
            for (String statusCategory : statusCategories) {
                rearrangedStatusCountMap.put(statusCategory, this.statusCountMap.getOrDefault(statusCategory, 0));
            }
            for (String statusCategory : this.statusCountMap.keySet()) {
                if (!rearrangedStatusCountMap.containsKey(statusCategory)) {
                    rearrangedStatusCountMap.put(statusCategory, this.statusCountMap.getOrDefault(statusCategory, 0));
                }
            }
            this.statusCountMap.clear();
            this.statusCountMap.putAll(rearrangedStatusCountMap);
        }

        public List<String> getTableRowValues() {
            final List<String> values = new ArrayList<>();
            values.add(getCapitalizedSeverity());
            for (String status : keySet()) {
                values.add(String.valueOf(getCount(status)));
            }
            values.add(String.valueOf(getTotal()));
            values.add(getAssessed());
            return values;
        }

        @Override
        public String toString() {
            return "SeverityToStatusRow{" +
                    "severity='" + severity + '\'' +
                    ", statusCountMap=" + statusCountMap +
                    '}';
        }
    }
}
