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

    private final Map<String, Map<String, Object>> severityStatusCountMap = new LinkedHashMap<>();

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
        severityStatusCountMap.get(severity).put(status, ((Integer) severityStatusCountMap.get(severity).get(status)) + 1);
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
        List<Integer> countList = new ArrayList<>();
        if (severityStatusCountMap.containsKey(severityCategory)) {
            for (String status : severityStatusCountMap.get(severityCategory).keySet()) {
                if (!status.equals("assessed") && !status.equals("% assessed")) {
                    countList.add((Integer) severityStatusCountMap.get(severityCategory).get(status));
                }
            }
        }
        return countList;
    }

    public List<Object> getValuesForSeverityCategory(String severityCategory) {
        severityCategory = normalize(severityCategory);
        List<Object> countList = new ArrayList<>();
        if (severityStatusCountMap.containsKey(severityCategory)) {
            for (String status : severityStatusCountMap.get(severityCategory).keySet()) {
                countList.add(severityStatusCountMap.get(severityCategory).get(status));
            }
        }
        return countList;
    }

    public List<Integer> getCountsForStatus(String status) {
        final String normalizedStatus = normalize(status);
        List<Integer> counts = new ArrayList<>();
        for (String severityCategory : severityStatusCountMap.keySet()) {
            final Integer i = (Integer) severityStatusCountMap.get(severityCategory).get(normalizedStatus);
            if (i != null) counts.add(i);
        }
        return counts;
    }

    public int getTotalForStatus(String status) {
        return getCountsForStatus(status).stream()
                .mapToInt(Integer::intValue).sum();
    }

    /**
     * Checks if the table is empty.<br>
     * The table is considered empty if all cells have the value "0", except for the "% assessed" column.
     *
     * @return true if the table is empty, false otherwise.
     */
    public boolean isEmpty() {
        for (Map<String, Object> m : severityStatusCountMap.values()) {
            for (Map.Entry<String, Object> e : m.entrySet()) {
                if (!e.getKey().equals("assessed") && !e.getKey().equals("% assessed")) {
                    if ((Integer) e.getValue() != 0) {
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

        if (modified) {
            if (vulnerabilityMetaData.isStatus("not applicable") || vulnerabilityMetaData.isStatus("void")) {
                return "none";
            }
        }

        return ObjectUtils.firstNonNull(
                modified ? vulnerabilityMetaData.getComplete("CVSS Modified Severity (v3)") : null,
                vulnerabilityMetaData.getComplete("CVSS Unmodified Severity (v3)"),
                modified ? vulnerabilityMetaData.getComplete("CVSS Modified Severity (v2)") : null,
                vulnerabilityMetaData.getComplete("CVSS Unmodified Severity (v2)"),
                "none"
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

    private static String getVulnerabilityStatus(VulnerabilityMetaData vulnerabilityMetaData, VulnerabilityReportAdapter adapter) {
        return adapter.getStatusText(vulnerabilityMetaData);
    }

    public static StatisticsOverviewTable fromInventoryUnmodified(VulnerabilityReportAdapter adapter, Function<String, String> vulnerabilityStatusMapper) {
        return StatisticsOverviewTable.fromInventory(adapter, null, false, vulnerabilityStatusMapper);
    }

    public static StatisticsOverviewTable fromInventoryModified(VulnerabilityReportAdapter adapter, Function<String, String> vulnerabilityStatusMapper) {
        return StatisticsOverviewTable.fromInventory(adapter, null, true, vulnerabilityStatusMapper);
    }

    public static StatisticsOverviewTable fromInventoryUnmodified(VulnerabilityReportAdapter adapter, String filterCert, Function<String, String> vulnerabilityStatusMapper) {
        return StatisticsOverviewTable.fromInventory(adapter, filterCert, false, vulnerabilityStatusMapper);
    }

    public static StatisticsOverviewTable fromInventoryModified(VulnerabilityReportAdapter adapter, String filterCert, Function<String, String> vulnerabilityStatusMapper) {
        return StatisticsOverviewTable.fromInventory(adapter, filterCert, true, vulnerabilityStatusMapper);
    }

    private static StatisticsOverviewTable fromInventory(VulnerabilityReportAdapter adapter, String filterCert, boolean useModifiedSeverity, Function<String, String> vulnerabilityStatusMapper) {
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
            final String status = vulnerabilityStatusMapper.apply(StatisticsOverviewTable.getVulnerabilityStatus(vmd, adapter));
            allStatuses.add(status);
        }

        // add the default status columns
        if (vulnerabilityStatusMapper == VULNERABILITY_STATUS_MAPPER_ABSTRACTED) {
            for (String status : new String[]{"affected", "potentially affected", "not affected"}) {
                table.addStatus(status);
            }
        }

        allStatuses.stream().sorted((o1, o2) -> {
            final String[] order = new String[]{
                    "applicable", "in review", "not applicable", "insignificant", "void",
                    "affected", "potentially affected", "not affected"};
            return Integer.compare(Arrays.asList(order).indexOf(o1), Arrays.asList(order).indexOf(o2));
        }).forEach(table::addStatus);

        // now that the cells exist, count the individual severity and status combinations
        for (VulnerabilityMetaData vmd : vmds) {
            final String status = vulnerabilityStatusMapper.apply(StatisticsOverviewTable.getVulnerabilityStatus(vmd, adapter));
            final String severity = StatisticsOverviewTable.getSeverityFromVMD(vmd, useModifiedSeverity);

            table.incrementCount(severity, status);
        }

        // add up the total number of vulnerabilities for each severity category
        for (Map.Entry<String, Map<String, Object>> severityMap : table.severityStatusCountMap.entrySet()) {
            int total = 0;
            for (Map.Entry<String, Object> statusMap : severityMap.getValue().entrySet()) {
                total += (Integer) statusMap.getValue();
            }
            severityMap.getValue().put("total", total);
        }

        // find the % of assessed vulnerabilities for each severity category
        for (Map.Entry<String, Map<String, Object>> severityMap : table.severityStatusCountMap.entrySet()) {
            final int applicable = (Integer) severityMap.getValue().getOrDefault("applicable", 0);
            final int notApplicable = useModifiedSeverity ? 0 : (Integer) severityMap.getValue().getOrDefault("not applicable", 0);
            final int inReview = (Integer) severityMap.getValue().getOrDefault("in review", 0);
            final int insignificant = (Integer) severityMap.getValue().getOrDefault("insignificant", 0);
            final int voidCat = (Integer) severityMap.getValue().getOrDefault("void", 0);

            final int affected = (Integer) severityMap.getValue().getOrDefault("affected", 0);
            final int potentiallyAffected = (Integer) severityMap.getValue().getOrDefault("potentially affected", 0);
            final int notAffected = (Integer) severityMap.getValue().getOrDefault("not affected", 0);

            if (vulnerabilityStatusMapper == VULNERABILITY_STATUS_MAPPER_DEFAULT) {
                final int total = inReview + applicable + notApplicable + insignificant + voidCat;

                if (total == 0) {
                    severityMap.getValue().put("assessed", "n/a");
                } else {
                    final double ratio = ((double) (applicable + notApplicable + voidCat)) / total;
                    severityMap.getValue().put("assessed", String.format("%.1f %%", ratio * 100));
                }
            } else if (vulnerabilityStatusMapper == VULNERABILITY_STATUS_MAPPER_ABSTRACTED) {
                final int total = affected + potentiallyAffected + notAffected;

                if (total == 0) {
                    severityMap.getValue().put("assessed", "n/a");
                } else {
                    final double ratio = ((double) (affected + notAffected)) / total;
                    severityMap.getValue().put("assessed", String.format("%.1f %%", ratio * 100));
                }
            }
        }

        // remove 'none' if all but the 'assessed' column are 0
        final Map<String, Object> noneEntry = table.severityStatusCountMap.get("none");
        if (noneEntry != null) {
            // remove the 'none' entry if the content does not contribute any further
            // information. That is when only 0 and n/a are included.
            if (noneEntry.values().stream().allMatch(obj -> {
                final String str = String.valueOf(obj);
                return "0".equals(str) || "0 %".equals(str) || "n/a".equals(str);
            })) {
                table.severityStatusCountMap.remove("none");
            }
        }

        LOG.debug("Generated Overview Table for [{}] vulnerabilities:\n{}", vmds.size(), table);

        return table;
    }

    private void removeStatus(String status) {
        for (Map<String, Object> m : severityStatusCountMap.values()) {
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
            return "Applicable";
        }

        // FIXME: when whould the status be Potential Vulnerability?
        if ("Potential Vulnerability".equalsIgnoreCase(name)) {
            return "Applicable";
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
        if (StringUtils.isEmpty(name)) { // in review (implicit)
            return "potentially affected";
        }

        switch (name) {
            case VulnerabilityMetaData.STATUS_VALUE_APPLICABLE:
            case VulnerabilityMetaData.STATUS_VALUE_INSIGNIFICANT: // here the explicit set status insignificant
                return "affected";

            case VulnerabilityMetaData.STATUS_VALUE_NOTAPPLICABLE:
            case VulnerabilityMetaData.STATUS_VALUE_VOID:
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
