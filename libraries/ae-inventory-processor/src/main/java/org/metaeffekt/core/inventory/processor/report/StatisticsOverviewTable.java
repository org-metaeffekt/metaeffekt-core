/*
 * Copyright 2009-2021 the original author or authors.
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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatisticsOverviewTable {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsOverviewTable.class);

    private final Map<String, Map<String, Integer>> severityStatusCountMap = new LinkedHashMap<>();

    private void addSeverityCategory(String severityCategory) {
        severityStatusCountMap.putIfAbsent(normalize(severityCategory), new LinkedHashMap<>());
    }

    private void addStatus(String status) {
        final String normalizedStatus = normalize(status);
        severityStatusCountMap.values().forEach(m -> m.putIfAbsent(normalizedStatus, 0));
    }

    private void incrementCount(String severity, String status) {
        severity = normalize(severity);
        status = normalize(status);
        addSeverityCategory(severity);
        addStatus(status);
        severityStatusCountMap.get(severity).put(status, severityStatusCountMap.get(severity).get(status) + 1);
    }

    public List<String> getHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("Severity");
        headers.addAll(severityStatusCountMap.values().stream().findFirst().orElse(new LinkedHashMap<>()).keySet());
        return headers.stream().map(this::capitalizeWords).collect(Collectors.toList());
    }

    public List<String> getSeverityCategories() {
        return severityStatusCountMap.keySet().stream().map(this::capitalizeWords).collect(Collectors.toList());
    }

    public List<Integer> getCountsForSeverityCategory(String severityCategory) {
        severityCategory = normalize(severityCategory);
        List<Integer> counts = new ArrayList<>();
        for (String status : severityStatusCountMap.get(severityCategory).keySet()) {
            counts.add(severityStatusCountMap.get(severityCategory).get(status));
        }
        return counts;
    }

    public List<Integer> getCountsForStatus(String status) {
        final String normalizedStatus = normalize(status);
        List<Integer> counts = new ArrayList<>();
        for (String severityCategory : severityStatusCountMap.keySet()) {
            counts.add(severityStatusCountMap.get(severityCategory).get(normalizedStatus));
        }
        return counts;
    }

    public int getTotalForStatus(String status) {
        return getCountsForStatus(status).stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Checks if the table is empty.<br>
     * The table is considered empty if all cells have the value "0", except for the "% assessed" column.
     *
     * @return true if the table is empty, false otherwise.
     */
    public boolean isEmpty() {
        for (Map<String, Integer> m : severityStatusCountMap.values()) {
            for (Map.Entry<String, Integer> e : m.entrySet()) {
                if (!e.getKey().equals("% assessed")) {
                    if (e.getValue() != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private String normalize(String s) {
        return s.toLowerCase();
    }

    private String capitalizeWords(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Arrays.stream(s.split(" "))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static String getSeverityFromVMD(VulnerabilityMetaData vulnerabilityMetaData, boolean modified) {
        return ObjectUtils.firstNonNull(
                modified ? vulnerabilityMetaData.getComplete("CVSS Modified Severity (v3)") : null,
                vulnerabilityMetaData.getComplete("CVSS Unmodified Severity (v3)"),
                modified ? vulnerabilityMetaData.getComplete("CVSS Modified Severity (v2)") : null,
                vulnerabilityMetaData.getComplete("CVSS Unmodified Severity (v2)"),
                "unset"
        );
    }

    private static void filterVulnerabilityMetaDataForAdvisories(List<VulnerabilityMetaData> vmds, String filterCert) {
        vmds.removeIf(
                vmd -> {
                    String adv = vmd.getComplete("Advisories");
                    if (adv == null) return true;
                    JSONArray advisories = new JSONArray(adv);
                    for (int j = 0; j < advisories.length(); j++) {
                        if (advisories.optJSONObject(j).optString("source", "").equals(filterCert)) {
                            return false;
                        }
                    }
                    return true;
                }
        );
    }

    private static String getStatusFromVMD(VulnerabilityMetaData vulnerabilityMetaData, float threshold, VulnerabilityReportAdapter adapter) {
        return adapter.getStatusText(vulnerabilityMetaData, threshold);
    }

    public static StatisticsOverviewTable fromInventoryUnmodified(VulnerabilityReportAdapter adapter, String filterCert, float threshold, Function<String, String> vulnerabilityStatusMapper) {
        return StatisticsOverviewTable.fromInventory(adapter, filterCert, threshold, false, vulnerabilityStatusMapper);
    }

    public static StatisticsOverviewTable fromInventoryModified(VulnerabilityReportAdapter adapter, String filterCert, float threshold, Function<String, String> vulnerabilityStatusMapper) {
        return StatisticsOverviewTable.fromInventory(adapter, filterCert, threshold, true, vulnerabilityStatusMapper);
    }

    private static StatisticsOverviewTable fromInventory(VulnerabilityReportAdapter adapter, String filterCert, float threshold, boolean useModifiedSeverity, Function<String, String> vulnerabilityStatusMapper) {
        final List<VulnerabilityMetaData> vmds = new ArrayList<>(adapter.inventory.getVulnerabilityMetaData());

        if (filterCert != null && !filterCert.isEmpty()) {
            StatisticsOverviewTable.filterVulnerabilityMetaDataForAdvisories(vmds, filterCert);
        }

        final StatisticsOverviewTable table = new StatisticsOverviewTable();

        // add the default severity categories in the case there are no vulnerabilities with that category
        for (String severityCategory : new String[]{"critical", "high", "medium", "low"}) {
            table.addSeverityCategory(severityCategory);
        }

        // the columns and rows have to be created first, for them to be added in the correct order below
        for (VulnerabilityMetaData vmd : vmds) {
            final String severity = StatisticsOverviewTable.getSeverityFromVMD(vmd, useModifiedSeverity);
            table.addSeverityCategory(severity);
        }
        final Set<String> allStatuses = new HashSet<>();
        for (VulnerabilityMetaData vmd : vmds) {
            final String status = vulnerabilityStatusMapper.apply(StatisticsOverviewTable.getStatusFromVMD(vmd, threshold, adapter));
            allStatuses.add(status);
        }
        allStatuses.stream().sorted((o1, o2) -> {
            final String[] order = new String[]{"reviewed", "affected", "potentially affected", "potential vulnerability", "in review", "not affected", "not applicable", "insignificant", "void"};
            return Integer.compare(Arrays.asList(order).indexOf(o1), Arrays.asList(order).indexOf(o2));
        }).forEach(table::addStatus);

        // now that the cells exist, count the individual severity and status combinations
        for (VulnerabilityMetaData vmd : vmds) {
            final String status = vulnerabilityStatusMapper.apply(StatisticsOverviewTable.getStatusFromVMD(vmd, threshold, adapter));
            final String severity = StatisticsOverviewTable.getSeverityFromVMD(vmd, useModifiedSeverity);

            table.incrementCount(severity, status);
        }

        // add up the total number of vulnerabilities for each severity category
        for (Map.Entry<String, Map<String, Integer>> severityMap : table.severityStatusCountMap.entrySet()) {
            int total = 0;
            for (Map.Entry<String, Integer> statusMap : severityMap.getValue().entrySet()) {
                total += statusMap.getValue();
            }
            severityMap.getValue().put("total", total);
        }

        // find the % of assessed vulnerabilities for each severity category
        for (Map.Entry<String, Map<String, Integer>> severityMap : table.severityStatusCountMap.entrySet()) {
            final int applicable = severityMap.getValue().getOrDefault("reviewed", 0);
            final int notApplicable = useModifiedSeverity ? 0 : severityMap.getValue().getOrDefault("not applicable", 0);
            final int inReview = severityMap.getValue().getOrDefault("in review", 0);
            final int insignificant = severityMap.getValue().getOrDefault("insignificant", 0);
            final int voidCat = severityMap.getValue().getOrDefault("void", 0);

            final int affected = severityMap.getValue().getOrDefault("affected", 0);
            final int potentiallyAffected = severityMap.getValue().getOrDefault("potentially affected", 0);
            final int notAffected = severityMap.getValue().getOrDefault("not affected", 0);

            if (vulnerabilityStatusMapper == VULNERABILITY_STATUS_MAPPER_DEFAULT) {
                if (inReview == 0 && insignificant == 0) {
                    severityMap.getValue().put("% assessed", 100);
                } else {
                    final int total = inReview + applicable + notApplicable + insignificant + voidCat;
                    final double ratio = ((double) (applicable + notApplicable + voidCat)) / total;

                    severityMap.getValue().put("% assessed", (int) (ratio * 100));
                }

            } else if (vulnerabilityStatusMapper == VULNERABILITY_STATUS_MAPPER_ABSTRACTED) {
                if (potentiallyAffected == 0) {
                    severityMap.getValue().put("% assessed", 100);
                } else {
                    final int total = affected + potentiallyAffected + notAffected;
                    final double ratio = ((double) (affected + notAffected)) / total;

                    severityMap.getValue().put("% assessed", (int) (ratio * 100));
                }
            }
        }

        // remove 'unset' if all but the '% assessed' column are 0
        if (table.severityStatusCountMap.containsKey("unset")
                && table.severityStatusCountMap.get("unset").values().stream().filter(i -> i != 0).count() == 1) {
            table.severityStatusCountMap.remove("unset");
        }

        LOG.debug("Generated Overview Table for [{}] vulnerabilities:\n{}", vmds.size(), table);

        return table;
    }

    private void removeStatus(String status) {
        for (Map<String, Integer> m : severityStatusCountMap.values()) {
            m.remove(status);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        final List<List<String>> rows = new ArrayList<>();
        rows.add(getHeaders());
        for (String severityCategory : getSeverityCategories()) {
            final List<String> row = new ArrayList<>();
            row.add(severityCategory);
            row.addAll(getCountsForSeverityCategory(severityCategory).stream().map(String::valueOf).collect(Collectors.toList()));
            rows.add(row);
        }

        final int[] columnWidths = new int[rows.get(0).size()];
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                columnWidths[i] = Math.max(columnWidths[i], row.get(i).length());
            }
        }

        boolean isFirst = true;
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                sb.append(String.format("%" + columnWidths[i] + "s", row.get(i)));
                if (i < row.size() - 1) {
                    sb.append(" │ ");
                }
            }
            sb.append("\n");

            if (isFirst) {
                isFirst = false;
                for (int i = 0; i < columnWidths.length; i++) {
                    sb.append(String.join("", Collections.nCopies(columnWidths[i], "─")));
                    if (i < columnWidths.length - 1) {
                        sb.append("─┼─");
                    }
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public final static Function<String, String> VULNERABILITY_STATUS_MAPPER_DEFAULT = name -> {
        if ("Applicable".equalsIgnoreCase(name)) {
            return "Reviewed";
        }

        if ("Potential Vulnerability".equalsIgnoreCase(name)) {
            return "Reviewed";
        }

        return name;
    };

    /**
     * Where the category
     * <ul>
     *     <li>
     *         <code>not affected</code> covers vulnerabilities with status <code>not applicable</code>,
     *         <code>void</code> or <code>insignificant</code>
     *     </li>
     *     <li>
     *         <code>potentially affected</code> covers vulnerabilities <code>in review</code> and have not yet been
     *         fully assessed (no category)
     *     </li>
     *     <li>
     *         <code>affected</code> covers reviewed and assesses as <code>applicable</code> vulnerabilities
     *     </li>
     * </ul>
     * see <code>AEAA-221</code> for more details.
     */
    public final static Function<String, String> VULNERABILITY_STATUS_MAPPER_ABSTRACTED = name -> {
        if (StringUtils.isEmpty(name)) { // in review
            return "potentially affected";
        }

        switch (name) {
            case VulnerabilityMetaData.STATUS_VALUE_APPLICABLE:
            case "reviewed":
            case "potential vulnerability":
                return "affected";

            case VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE:
            case VulnerabilityMetaData.STATUS_VALUE_VOID:
            case VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT:
                return "not affected";

            case "in review":
                return "potentially affected";

            default:
                return name;
        }
    };

    public static Function<String, String> getStatusMapperFunction(String statusMapper) {
        if ("abstracted".equals(statusMapper)) {
            return StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_ABSTRACTED;
        } else {
            return StatisticsOverviewTable.VULNERABILITY_STATUS_MAPPER_DEFAULT;
        }
    }
}
