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
package org.metaeffekt.core.security.cvss.processor;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.CvssSource;
import org.metaeffekt.core.security.cvss.CvssSource.CvssEntity;
import org.metaeffekt.core.security.cvss.CvssSource.CvssIssuingEntityRole;
import org.metaeffekt.core.security.cvss.CvssVector;
import org.metaeffekt.core.security.cvss.MultiScoreCvssVector;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * <h2>Effective CVSS Selection</h2>
 * <p>
 * This documentation entry aims to explain how the selection process of effective CVSS vectors for vulnerabilities using the central security policy configuration (may) take place, and how the effective scores and severities are calculated.<br>
 * For this, the <code>initialCvssSelector</code>, <code>contextCvssSelector</code>, <code>cvssVersionSelectionPolicy</code> and the <code>cvssSeverityRanges</code> properties of the security policy are used.
 * </p>
 * <p>The selection process happens in three steps, with another one added for calculating the CVSS scores:</p>
 * <ol>
 *    <li>
 *       <p>Collection of all CVSS vectors on the vulnerability</p>
 *       <ol>
 *          <li>
 *             <p>collect a set of all CVSS vectors on the vulnerability itself</p>
 *          </li>
 *          <li>
 *             <p>find the related security advisories and add all vectors to the set, evaluating the conditions if present</p>
 *          </li>
 *       </ol>
 *    </li>
 *    <li>
 *       <p>Use the CVSS Selectors to find one vector per schema version</p>
 *       <ol>
 *          <li>
 *             <p>evaluate the initialCvssSelector per version present in the set of vectors to obtain the selected (currently three versioned) vectors</p>
 *          </li>
 *          <li>
 *             <p>evaluate the contextCvssSelector per version present in the set of vectors to obtain the selected (currently three versioned) vectors</p>
 *          </li>
 *       </ol>
 *    </li>
 *    <li>
 *       <p>Use the cvssVersionSelectionPolicy to reduce the selected vectors to a single vector per selector</p>
 *    </li>
 *    <li>
 *       <p>Calculate your scores using the vectors and use the cvssSeverityRanges to determine the severity ranges of the scores.</p>
 *    </li>
 * </ol>
 * <p>Let’s assume that we have the following inventory:</p>
 * <table>
 *    <caption>Inventory of artifacts, vulnerabilities and security advisories</caption>
 *    <tbody>
 *       <tr>
 *          <th>
 *             <p><strong>Type</strong></p>
 *          </th>
 *          <th>
 *             <p><strong>Id</strong></p>
 *          </th>
 *          <th>
 *             <p><strong>CVSS Vectors</strong></p>
 *          </th>
 *          <th>
 *             <p><strong>Other properties</strong></p>
 *          </th>
 *       </tr>
 *       <tr>
 *          <td>
 *             <p>Artifact</p>
 *          </td>
 *          <td>
 *             <p>Windows 10 Pro</p>
 *          </td>
 *          <td>
 *             <p>&nbsp;</p>
 *          </td>
 *          <td>
 *             <p>MS Product ID = 5678<br>Vulnerabilities = CVE-2020-1234</p>
 *          </td>
 *       </tr>
 *       <tr>
 *          <td>
 *             <p>Vulnerability</p>
 *          </td>
 *          <td>
 *             <p>CVE-2020-1234</p>
 *          </td>
 *          <td>
 *             <p>CVSS:3.1 NVD-CNA-NVD, CVSS:2.0 NVD-CNA-GitHub Inc., CVSS:2.0 Assessment-all, CVSS:2.0 Assessment-lower</p>
 *          </td>
 *          <td>
 *             <p>Referenced Ids = GHSA-1234-5678-9101, MSRC-CVE-2020-1234 (simplified format)<br>Matching Source = Windows 10 Pro (simplified format)</p>
 *          </td>
 *       </tr>
 *       <tr>
 *          <td>
 *             <p>Security Advisory</p>
 *          </td>
 *          <td>
 *             <p>GHSA-1234-5678-9101</p>
 *          </td>
 *          <td>
 *             <p>CVSS:2.0 GitHub Inc.</p>
 *          </td>
 *          <td>
 *             <p>&nbsp;</p>
 *          </td>
 *       </tr>
 *       <tr>
 *          <td>
 *             <p>Security Advisory</p>
 *          </td>
 *          <td>
 *             <p>MSRC-CVE-2020-1234</p>
 *          </td>
 *          <td>
 *             <p>CVSS:3.1 Microsoft Corporation (MS Product ID = 1234), CVSS:3.1 Microsoft Corporation (MS Product ID = 5678)</p>
 *          </td>
 *          <td>
 *             <p>&nbsp;</p>
 *          </td>
 *       </tr>
 *    </tbody>
 * </table>
 * <p>Then the selection process on CVE-2020-1234 would be the following, assuming the default values of the security configuration:</p>
 * <table>
 *    <caption>General selection process</caption>
 *    <tbody>
 *       <tr>
 *          <th>
 *             <p><strong>Step</strong></p>
 *          </th>
 *          <th>
 *             <p><strong>Notes</strong></p>
 *          </th>
 *          <th>
 *             <p><strong>Result</strong></p>
 *          </th>
 *       </tr>
 *    </tbody>
 * </table>
 * <table>
 *    <caption>Selection process for CVSS vectors on CVE-2020-1234</caption>
 *    <tbody>
 *       <tr>
 *          <th>
 *             <p><strong>Step</strong></p>
 *          </th>
 *          <th>
 *             <p><strong>Notes</strong></p>
 *          </th>
 *          <th>
 *             <p><strong>Result</strong></p>
 *          </th>
 *       </tr>
 *       <tr>
 *          <td>
 *             <p>1. Collect vectors</p>
 *          </td>
 *          <td>
 *             <p>Pick all vectors directly on the vulnerability, add the ones from the related security advisories:</p>
 *             <ul>
 *                <li>
 *                   <p>CVE-2020-1234 → CVSS:3.1 NVD-CNA-NVD CVSS:2.0 NVD-CNA-GitHub Inc. CVSS:2.0 Assessment-all CVSS:2.0 Assessment-lower</p>
 *                </li>
 *                <li>
 *                   <p>GHSA-1234-5678-9101 → CVSS:2.0 GitHub Inc.</p>
 *                </li>
 *                <li>
 *                   <p>MSRC-CVE-2020-1234 → CVSS:3.1 Microsoft Corporation (MS Product ID = 5678) Which filters out CVSS:3.1 Microsoft Corporation (MS Product ID = 1234), since the vulnerability was not matched on an artifact with that product id.</p>
 *                </li>
 *             </ul>
 *          </td>
 *          <td>
 *             <p>CVSS:3.1 NVD-CNA-NVD CVSS:3.1 Microsoft Corporation (MS Product ID = 5678) CVSS:2.0 NVD-CNA-GitHub Inc. CVSS:2.0 GitHub Inc. CVSS:2.0 Assessment-all CVSS:2.0 Assessment-lower</p>
 *          </td>
 *       </tr>
 *       <tr>
 *          <td>
 *             <p>2. CVSS Selectors</p>
 *          </td>
 *          <td>
 *             <p>Evaluate the individual vectors using each vector version to (currently) obtain up to 6 vectors. In our case, this results in 3 vectors:</p>
 *             <ul>
 *                <li>
 *                   <p>initial:</p>
 *                   <ul>
 *                      <li>
 *                         <p>version 2.0:</p>
 *                         <ul>
 *                            <li>
 *                               <p>rule 1 selects the NVD-CNA-GitHub, Inc. source</p>
 *                            </li>
 *                         </ul>
 *                      </li>
 *                      <li>
 *                         <p>version 3.1:</p>
 *                         <ul>
 *                            <li>
 *                               <p>rule 1 selects the NVD-CNA-NVD source</p>
 *                            </li>
 *                         </ul>
 *                      </li>
 *                   </ul>
 *                </li>
 *                <li>
 *                   <p>context</p>
 *                   <ul>
 *                      <li>
 *                         <p>version 2.0:</p>
 *                         <ul>
 *                            <li>
 *                               <p>rule 1 selects the NVD-CNA-GitHub, Inc. source</p>
 *                            </li>
 *                            <li>
 *                               <p>rule 2 selects the Assessment-*-all source and applies all vector components on top of the current result vector</p>
 *                            </li>
 *                            <li>
 *                               <p>rule 3 selects the Assessment-*-lower source and applies vector components that lead to a lower or equal overall score on top of the current result vector</p>
 *                            </li>
 *                         </ul>
 *                      </li>
 *                      <li>
 *                         <p>version 3.1:</p>
 *                         <ul>
 *                            <li>
 *                               <p>rule 1 selects the NVD-CNA-NVD source</p>
 *                            </li>
 *                            <li>
 *                               <p>statistics evaluator 1 finds that no assessment vector was applied, therefore no vector is returned</p>
 *                            </li>
 *                         </ul>
 *                      </li>
 *                   </ul>
 *                </li>
 *             </ul>
 *          </td>
 *          <td>
 *             <ul>
 *                <li>
 *                   <p>initial:</p>
 *                   <ul>
 *                      <li>
 *                         <p>version 2.0: CVSS:2.0 NVD-CNA-GitHub Inc.</p>
 *                      </li>
 *                      <li>
 *                         <p>version 3.1: CVSS:3.1 NVD-CNA-NVD</p>
 *                      </li>
 *                   </ul>
 *                </li>
 *                <li>
 *                   <p>context:</p>
 *                   <ul>
 *                      <li>
 *                         <p>version 2.0: CVSS:2.0 NVD-CNA-GitHub Inc. + Assessment-all + Assessment-lower</p>
 *                      </li>
 *                      <li>
 *                         <p>version 3.1: no vector selected</p>
 *                      </li>
 *                   </ul>
 *                </li>
 *             </ul>
 *          </td>
 *       </tr>
 *       <tr>
 *          <td>
 *             <p>3. Version selection</p>
 *          </td>
 *          <td>
 *             <p>Since the default is latest, simply pick the latest CVSS vector version.</p>
 *             <ul>
 *                <li>
 *                   <p>The initial vector selection contains a 3.1 vector, which is newer than 2.0, so it is selected.</p>
 *                </li>
 *                <li>
 *                   <p>The context vector selection does not contain a 3.1 or 4.0 vector, so 2.0 is used as fallback.</p>
 *                </li>
 *             </ul>
 *          </td>
 *          <td>
 *             <ul>
 *                <li>
 *                   <p>initial:</p>
 *                   <ul>
 *                      <li>
 *                         <p>version 3.1: CVSS:3.1 NVD-CNA-NVD</p>
 *                      </li>
 *                   </ul>
 *                </li>
 *                <li>
 *                   <p>context:</p>
 *                   <ul>
 *                      <li>
 *                         <p>version 2.0: CVSS:2.0 NVD-CNA-GitHub Inc. + Assessment-all + Assessment-lower</p>
 *                      </li>
 *                   </ul>
 *                </li>
 *             </ul>
 *          </td>
 *       </tr>
 *       <tr>
 *          <td>
 *             <p>4. Score calculation</p>
 *          </td>
 *          <td>
 *             <p>Now the severity ranges can be used to determine the severity categories of the scores calculated by the selected CVSS vectors. This step will be skipped here, since we only used the sources property of the vectors, and did not actually use the actual vector components.</p>
 *          </td>
 *          <td>
 *             <p>&nbsp;</p>
 *          </td>
 *       </tr>
 *    </tbody>
 * </table>
 */
public class CvssSelector implements Cloneable {

    private final static Logger LOG = LoggerFactory.getLogger(CvssSelector.class);

    private final List<CvssRule> rules;
    private final List<SelectorStatsEvaluator> statsEvaluatorActions;
    private final List<SelectorVectorEvaluator> selectorVectorEvaluators;

    public CvssSelector(List<CvssRule> rules, List<SelectorStatsEvaluator> statsEvaluatorActions, List<SelectorVectorEvaluator> selectorVectorEvaluators) {
        if (rules == null) throw new IllegalArgumentException("rules must not be null");
        if (statsEvaluatorActions == null) throw new IllegalArgumentException("statsEvaluatorActions must not be null");
        if (selectorVectorEvaluators == null)
            throw new IllegalArgumentException("selectorVectorEvaluators must not be null");
        this.rules = rules;
        this.statsEvaluatorActions = statsEvaluatorActions;
        this.selectorVectorEvaluators = selectorVectorEvaluators;
    }

    public CvssSelector(List<CvssRule> rules) {
        this(rules, Collections.emptyList(), Collections.emptyList());
    }

    public <T extends CvssVector> T selectVector(Collection<T> vectors) {
        T effective = null;
        final Map<String, Integer> stats = new HashMap<>();

        for (CvssRule rule : rules) {
            final T chosenVector = rule.getSourceSelector().selectVector(vectors);

            final List<SelectorVectorEvaluator> vectorEvaluators = rule.getVectorEvaluators();

            boolean skip = false;
            for (SelectorVectorEvaluator vectorEvaluator : vectorEvaluators) {
                if (vectorEvaluator.evaluate(chosenVector)) {
                    final EvaluatorAction action = vectorEvaluator.getAction();
                    if (action == EvaluatorAction.RETURN_NULL) {
                        return null;
                    } else if (action == EvaluatorAction.FAIL) {
                        throw new IllegalStateException("Evaluator action failed: " + vectorEvaluator + " on " + this);
                    } else if (action == EvaluatorAction.SKIP) {
                        skip = true;
                        break;
                    } else if (action == EvaluatorAction.RETURN_PREVIOUS) {
                        return effective;
                    }
                }
            }
            if (chosenVector == null && vectorEvaluators.isEmpty()) {
                for (SelectorStatsCollector collector : rule.getStatsCollectors()) {
                    collector.apply(stats, 0, 1, () -> 0);
                }
                skip = true;
            }
            if (skip) continue;

            if (chosenVector != null) {
                if (effective == null) {
                    effective = (T) chosenVector.clone();

                    for (SelectorStatsCollector collector : rule.getStatsCollectors()) {
                        collector.apply(stats, 1, 0, effective::size);
                    }
                } else {
                    final Pair<CvssVector, Integer> result = rule.getMergingMethod().mergeVectors(effective, chosenVector);
                    effective = (T) result.getLeft().deriveAddSource(chosenVector.getCvssSource());

                    for (SelectorStatsCollector collector : rule.getStatsCollectors()) {
                        collector.apply(stats, 1, 0, result::getRight);
                    }
                }

            } else {
                for (SelectorStatsCollector collector : rule.getStatsCollectors()) {
                    collector.apply(stats, 0, 1, () -> 0);
                }
            }
        }

        for (SelectorStatsEvaluator statsEvaluatorAction : statsEvaluatorActions) {
            final Integer value = stats.getOrDefault(statsEvaluatorAction.getAttributeName(), 0);
            if (statsEvaluatorAction.getComparator().test(value, statsEvaluatorAction.getComparisonValue())) {
                final EvaluatorAction action = statsEvaluatorAction.getAction();
                if (action == EvaluatorAction.RETURN_NULL) {
                    return null;
                } else if (action == EvaluatorAction.FAIL) {
                    throw new IllegalStateException("Evaluator action failed: " + statsEvaluatorAction + " on " + this);
                }
            }
        }

        for (SelectorVectorEvaluator vectorEvaluator : selectorVectorEvaluators) {
            if (vectorEvaluator.evaluate(effective)) {
                final EvaluatorAction action = vectorEvaluator.getAction();
                if (action == EvaluatorAction.RETURN_NULL) {
                    return null;
                } else if (action == EvaluatorAction.FAIL) {
                    throw new IllegalStateException("Evaluator action failed: " + vectorEvaluator + " on " + this);
                }
            }
        }

        return effective;
    }

    public <T extends CvssVector> T selectVector(Collection<?> vectors, Class<T> vectorClass) {
        final List<T> checkVectors = vectors.stream()
                .filter(v -> vectorClass.isAssignableFrom(v.getClass()))
                .map(v -> (T) v)
                .collect(Collectors.toList());

        return selectVector(checkVectors);
    }

    public <T extends CvssVector> T selectVector(CvssVectorSet vectors, Class<T> vectorClass) {
        return selectVector(vectors.getCvssVectors(), vectorClass);
    }

    public JSONObject toJson() {
        final JSONArray rules = new JSONArray();
        for (CvssRule rule : this.rules) {
            rules.put(rule.toJson());
        }

        final JSONArray stats = new JSONArray();
        for (SelectorStatsEvaluator statsEvaluatorAction : statsEvaluatorActions) {
            stats.put(statsEvaluatorAction.toJson());
        }

        final JSONArray vectorEvaluators = new JSONArray(selectorVectorEvaluators.stream().map(SelectorVectorEvaluator::toJson).collect(Collectors.toList()));

        return new JSONObject()
                .put("rules", rules)
                .put("stats", stats)
                .put("vectorEval", vectorEvaluators);
    }

    public static CvssSelector fromJson(String jsonString) {
        final JSONObject json;
        try {
            json = new JSONObject(jsonString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse json string as JSONObject for CvssSelector: " + jsonString, e);
        }
        return fromJson(json);
    }

    public static CvssSelector fromJson(JSONObject json) {
        final JSONArray jsonRules = json.optJSONArray("rules");
        final List<CvssRule> rules;
        if (jsonRules != null) {
            rules = new ArrayList<>();
            for (int i = 0; i < jsonRules.length(); i++) {
                rules.add(CvssRule.fromJson(jsonRules.getJSONObject(i)));
            }
        } else {
            rules = Collections.emptyList();
        }

        final JSONArray jsonStats = json.optJSONArray("stats");
        final List<SelectorStatsEvaluator> statsEvaluatorActions;
        if (jsonStats != null) {
            statsEvaluatorActions = new ArrayList<>();
            for (int i = 0; i < jsonStats.length(); i++) {
                statsEvaluatorActions.add(SelectorStatsEvaluator.fromJson(jsonStats.getJSONObject(i)));
            }
        } else {
            statsEvaluatorActions = Collections.emptyList();
        }

        return new CvssSelector(rules, statsEvaluatorActions, SelectorVectorEvaluator.fromParentJson(json));
    }

    public List<CvssRule> getRules() {
        return rules;
    }

    public List<SelectorStatsEvaluator> getStatsEvaluatorActions() {
        return statsEvaluatorActions;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public CvssSelector clone() {
        return new CvssSelector(this.rules.stream().map(CvssRule::clone).collect(Collectors.toList()));
    }

    public String explain() {
        return explain(null);
    }

    public String explain(String selectorName) {
        final StringBuilder explanation = new StringBuilder();

        if (!rules.isEmpty()) {
            if (selectorName != null) {
                explanation.append("The [").append(selectorName).append("]");
            } else {
                explanation.append("The");
            }
            explanation.append(" CVSS Selector contains [").append(rules.size()).append("] rule").append(rules.size() == 1 ? "" : "s").append(" that will be applied in the following order:\n");
            final StringJoiner rulesExplanation = new StringJoiner("\n- ", "- ", "");

            for (CvssRule rule : rules) {
                final StringBuilder ruleExplanation = new StringBuilder();

                {
                    final StringJoiner ruleSegmentsJoiner = new StringJoiner(", ");

                    for (SourceSelectorEntry source : rule.getSourceSelector().getPreferredSources()) {
                        final StringJoiner ruleSegments = new StringJoiner("-", "[", "]");

                        if (source.getHostingEntities().isEmpty()) {
                            ruleSegments.add("*");
                        } else if (source.getHostingEntities().size() == 1) {
                            ruleSegments.add(source.getHostingEntities().get(0).toString());
                        } else {
                            ruleSegments.add(source.getHostingEntities().stream().map(Object::toString).collect(Collectors.joining(" AND ", "(", ")")));
                        }
                        if (source.getIssuingEntityRoles().isEmpty()) {
                            ruleSegments.add("*");
                        } else if (source.getIssuingEntityRoles().size() == 1) {
                            ruleSegments.add(source.getIssuingEntityRoles().get(0).toString());
                        } else {
                            ruleSegments.add(source.getIssuingEntityRoles().stream().map(Object::toString).collect(Collectors.joining(" AND ", "(", ")")));
                        }
                        if (source.getIssuingEntities().isEmpty()) {
                            ruleSegments.add("*");
                        } else if (source.getIssuingEntities().size() == 1) {
                            ruleSegments.add(source.getIssuingEntities().get(0).toString());
                        } else {
                            ruleSegments.add(source.getIssuingEntities().stream().map(Object::toString).collect(Collectors.joining(" AND ", "(", ")")));
                        }

                        ruleSegmentsJoiner.add(ruleSegments.toString());
                    }

                    if (rule.getSourceSelector().getPreferredSources().size() == 1) {
                        ruleExplanation.append("If present, the ").append(ruleSegmentsJoiner).append(" vector is selected.");
                    } else {
                        ruleExplanation.append("The first matching vector is selected: ").append(ruleSegmentsJoiner);
                    }
                }

                {
                    if (!rule.getVectorEvaluators().isEmpty()) {
                        ruleExplanation.append("\nThe following [").append(rule.getVectorEvaluators().size()).append("]").append(" vector evaluator").append(rule.getVectorEvaluators().size() == 1 ? "" : "s").append(" will be applied to the selected vector before applying it to the resulting vector:\n");

                        final StringJoiner vectorEvaluatorsJoiner = new StringJoiner("\n- ", "- ", "");

                        for (SelectorVectorEvaluator vectorEvaluator : rule.getVectorEvaluators()) {
                            final Map<VectorEvaluatorOperation, Boolean> operations = vectorEvaluator.getOperations();
                            final EvaluatorAction action = vectorEvaluator.getAction();

                            final StringBuilder operationsBuilder = new StringBuilder();

                            if (operations.isEmpty()) {
                                operationsBuilder.append("Since no selector is specified, the following is always applied: ");
                            } else {
                                operationsBuilder.append("If the selected vector is ")
                                        .append(operations.entrySet().stream().map(e -> e.getKey().toPotentiallyInvertedPrettyString(e.getValue())).collect(Collectors.joining("] AND [", "[", "]")))
                                        .append(", then ");
                            }

                            if (action == EvaluatorAction.RETURN_NULL) {
                                operationsBuilder.append("[no vector is returned].");
                            } else if (action == EvaluatorAction.RETURN_PREVIOUS) {
                                operationsBuilder.append("the [previous vector is returned].");
                            } else if (action == EvaluatorAction.FAIL) {
                                operationsBuilder.append("the [evaluation fails].");
                            } else if (action == EvaluatorAction.SKIP) {
                                operationsBuilder.append("the [evaluation is skipped].");
                            } else {
                                operationsBuilder.append("[no (known) operation is specified].");
                            }

                            vectorEvaluatorsJoiner.add(operationsBuilder.toString());
                        }

                        ruleExplanation.append(vectorEvaluatorsJoiner);
                    }
                }

                ruleExplanation.append("\nFrom the selected vector, ");

                if (rule.getMergingMethod() == MergingMethod.ALL) {
                    ruleExplanation.append("[all] vector components are applied to the resulting vector.");
                } else if (rule.getMergingMethod() == MergingMethod.LOWER) {
                    ruleExplanation.append("only vector components that lead to a [lower or equal] score on the resulting vector are applied.");
                } else if (rule.getMergingMethod() == MergingMethod.HIGHER) {
                    ruleExplanation.append("only vector components that lead to a [higher or equal] score on the resulting vector are applied.");
                } else if (rule.getMergingMethod() == MergingMethod.OVERWRITE) {
                    ruleExplanation.append("the resulting vector is [overwritten].");
                } else {
                    ruleExplanation.append("No (known) operation is specified for the merging method.");
                }

                {
                    if (!rule.getStatsCollectors().isEmpty()) {
                        ruleExplanation.append("\nThe following [").append(rule.getStatsCollectors().size()).append("] statistics collector").append(rule.getStatsCollectors().size() == 1 ? "" : "s").append(" will also be applied to the selected vector:\n");

                        final StringJoiner statsJoiner = new StringJoiner("\n- ", "- ", "");

                        for (SelectorStatsCollector statsCollector : rule.getStatsCollectors()) {
                            final StringBuilder statsExplanation = new StringBuilder();

                            if (statsCollector.getProvider() == StatsCollectorProvider.ABSENCE) {
                                statsExplanation.append("If no vector is returned from the selection, ");
                            } else if (statsCollector.getProvider() == StatsCollectorProvider.PRESENCE) {
                                statsExplanation.append("If a vector is returned from the selection, ");
                            } else if (statsCollector.getProvider() == StatsCollectorProvider.APPLIED_PARTS_COUNT) {
                                statsExplanation.append("If a vector is returned from the selection, ");
                            }

                            if (statsCollector.getSetType() == StatsCollectorSetType.MAX) {
                                statsExplanation.append("the larger value between ");
                            } else if (statsCollector.getSetType() == StatsCollectorSetType.MIN) {
                                statsExplanation.append("the smaller value between ");
                            }

                            if (statsCollector.getProvider() == StatsCollectorProvider.ABSENCE) {
                                statsExplanation.append("[1] ");
                            } else if (statsCollector.getProvider() == StatsCollectorProvider.PRESENCE) {
                                statsExplanation.append("[1] ");
                            } else if (statsCollector.getProvider() == StatsCollectorProvider.APPLIED_PARTS_COUNT) {
                                statsExplanation.append("the [amount of applied vector components] ");
                            }

                            if (statsCollector.getSetType() == StatsCollectorSetType.ADD) {
                                statsExplanation.append("is added to ");
                            } else if (statsCollector.getSetType() == StatsCollectorSetType.SUBTRACT) {
                                statsExplanation.append("is subtracted from ");
                            } else if (statsCollector.getSetType() == StatsCollectorSetType.MAX) {
                                statsExplanation.append("and the previous value is set to ");
                            } else if (statsCollector.getSetType() == StatsCollectorSetType.MIN) {
                                statsExplanation.append("and the previous value is set to ");
                            } else if (statsCollector.getSetType() == StatsCollectorSetType.SET) {
                                statsExplanation.append("is set to ");
                            }

                            statsExplanation.append("the stats collector attribute [" + statsCollector.getAttributeName() + "].");

                            statsJoiner.add(statsExplanation);
                        }

                        ruleExplanation.append(statsJoiner);
                    }
                }

                rulesExplanation.add(ruleExplanation.toString().replace("\n", "\n  "));
            }

            explanation.append(rulesExplanation);

        } else { // rules.isEmpty()
            explanation.append("The CVSS Selector contains no rules. The resulting vector will be [not defined].");
        }

        if (!statsEvaluatorActions.isEmpty()) {
            if (explanation.length() > 0) {
                explanation.append("\n\n");
            }
            explanation.append("After finishing the cvss selection, [").append(statsEvaluatorActions.size()).append("] statistics evaluator").append(statsEvaluatorActions.size() == 1 ? "" : "s").append(" will be applied to the resulting vector:\n");

            final StringJoiner statsEvaluatorJoiner = new StringJoiner("\n- ", "- ", "");

            for (SelectorStatsEvaluator statsEvaluatorAction : statsEvaluatorActions) {
                final StringBuilder statsEvaluatorExplanation = new StringBuilder();

                statsEvaluatorExplanation.append("If the stats collector attribute [").append(statsEvaluatorAction.getAttributeName()).append("] is [")
                        .append(statsEvaluatorAction.getComparator().name().replace("_", " ").toLowerCase()).append("] to [").append(statsEvaluatorAction.getComparisonValue()).append("], then ");

                if (statsEvaluatorAction.getAction() == EvaluatorAction.RETURN_NULL) {
                    statsEvaluatorExplanation.append("[no vector is returned].");
                } else if (statsEvaluatorAction.getAction() == EvaluatorAction.FAIL) {
                    statsEvaluatorExplanation.append("the [evaluation fails].");
                } else if (statsEvaluatorAction.getAction() == EvaluatorAction.SKIP) {
                    statsEvaluatorExplanation.append("the [evaluation would be skipped], this action is not supported for stats evaluators however so nothing will happen.");
                } else if (statsEvaluatorAction.getAction() == EvaluatorAction.RETURN_PREVIOUS) {
                    statsEvaluatorExplanation.append("the [previous vector would be returned], this action is not supported for stats evaluators however so nothing will happen.");
                } else {
                    statsEvaluatorExplanation.append("[no (known) operation is specified].");
                }

                statsEvaluatorJoiner.add(statsEvaluatorExplanation);
            }

            explanation.append(statsEvaluatorJoiner);
        }

        {
            if (!selectorVectorEvaluators.isEmpty()) {
                if (!statsEvaluatorActions.isEmpty()) {
                    explanation.append("\nAdditionally, [");
                } else {
                    explanation.append("\n\nAfter finishing the cvss selection, [");
                }
                explanation.append(selectorVectorEvaluators.size()).append("] vector evaluator").append(selectorVectorEvaluators.size() == 1 ? "" : "s").append(" will be applied to the resulting vector:\n");

                final StringJoiner vectorEvaluatorsJoiner = new StringJoiner("\n- ", "- ", "");

                for (SelectorVectorEvaluator vectorEvaluator : selectorVectorEvaluators) {
                    final Map<VectorEvaluatorOperation, Boolean> operations = vectorEvaluator.getOperations();
                    final EvaluatorAction action = vectorEvaluator.getAction();

                    final StringBuilder operationsBuilder = new StringBuilder();

                    if (operations.isEmpty()) {
                        operationsBuilder.append("Since no selector is specified, the following is always applied: ");
                    } else {
                        operationsBuilder.append("If the resulting vector is ")
                                .append(operations.entrySet().stream().map(e -> e.getKey().toPotentiallyInvertedPrettyString(e.getValue())).collect(Collectors.joining("] AND [", "[", "]")))
                                .append(", then ");
                    }

                    if (action == EvaluatorAction.RETURN_NULL) {
                        operationsBuilder.append("[no vector is returned].");
                    } else if (action == EvaluatorAction.RETURN_PREVIOUS) {
                        operationsBuilder.append("the [previous vector is returned].");
                    } else if (action == EvaluatorAction.FAIL) {
                        operationsBuilder.append("the [evaluation fails].");
                    } else if (action == EvaluatorAction.SKIP) {
                        operationsBuilder.append("the [evaluation is skipped].");
                    } else {
                        operationsBuilder.append("[no (known) operation is specified].");
                    }

                    vectorEvaluatorsJoiner.add(operationsBuilder.toString());
                }

                explanation.append(vectorEvaluatorsJoiner);
            }
        }

        return explanation.toString();
    }

    public static class CvssRule implements Cloneable {
        private final SourceSelector sourceSelector;
        private final MergingMethod mergingMethod;
        private final List<SelectorStatsCollector> statsCollectors;
        private final List<SelectorVectorEvaluator> vectorEvaluators;

        public CvssRule(MergingMethod mergingMethod, List<SelectorStatsCollector> statsCollectors, List<SelectorVectorEvaluator> vectorEvaluators, SourceSelector sourceSelector) {
            this.mergingMethod = mergingMethod;
            this.statsCollectors = statsCollectors;
            this.vectorEvaluators = vectorEvaluators;
            this.sourceSelector = sourceSelector;
        }

        public CvssRule(MergingMethod mergingMethod, List<SelectorStatsCollector> statsCollectors, List<SelectorVectorEvaluator> vectorEvaluators, SourceSelectorEntry... preferredSources) {
            this.mergingMethod = mergingMethod;
            this.statsCollectors = statsCollectors;
            this.vectorEvaluators = vectorEvaluators;
            this.sourceSelector = new SourceSelector(preferredSources);
        }

        public CvssRule(MergingMethod mergingMethod, SourceSelector sourceSelector) {
            this(mergingMethod, Collections.emptyList(), Collections.emptyList(), sourceSelector);
        }

        public CvssRule(MergingMethod mergingMethod, SourceSelectorEntry... preferredSources) {
            this(mergingMethod, Collections.emptyList(), Collections.emptyList(), preferredSources);
        }

        public JSONObject toJson() {
            final JSONArray vectorEvaluators = new JSONArray(this.vectorEvaluators.stream().map(SelectorVectorEvaluator::toJson).collect(Collectors.toList()));
            return new JSONObject()
                    .put("vectorEval", vectorEvaluators)
                    .put("selector", sourceSelector.toJson())
                    .put("stats", statsCollectors.stream().map(SelectorStatsCollector::toJson).collect(Collectors.toList()))
                    .put("method", mergingMethod.name());
        }

        public static CvssRule fromJson(JSONObject json) {
            final JSONArray stats = json.optJSONArray("stats");
            final List<SelectorStatsCollector> statsCollectors;
            if (stats != null) {
                statsCollectors = new ArrayList<>();
                for (int i = 0; i < stats.length(); i++) {
                    statsCollectors.add(SelectorStatsCollector.fromJson(stats.getJSONObject(i)));
                }
            } else {
                statsCollectors = Collections.emptyList();
            }

            try {
                return new CvssRule(
                        MergingMethod.valueOf(ObjectUtils.firstNonNull(json.optString("method", null), json.optString("mergingMethod", null))),
                        statsCollectors,
                        SelectorVectorEvaluator.fromParentJson(json),
                        SourceSelector.fromJson(ObjectUtils.firstNonNull(json.opt("selector"), json.opt("sourceSelector")))
                );
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse CvssRule from json with keys " + json.keySet() + ": " + json, e);
            }
        }

        public SourceSelector getSourceSelector() {
            return sourceSelector;
        }

        public MergingMethod getMergingMethod() {
            return mergingMethod;
        }

        public List<SelectorStatsCollector> getStatsCollectors() {
            return statsCollectors;
        }

        public List<SelectorVectorEvaluator> getVectorEvaluators() {
            return vectorEvaluators;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public CvssRule clone() {
            return new CvssRule(
                    this.mergingMethod,
                    this.getStatsCollectors().stream().map(SelectorStatsCollector::clone).collect(Collectors.toList()),
                    this.getVectorEvaluators().stream().map(SelectorVectorEvaluator::clone).collect(Collectors.toList()),
                    this.sourceSelector.clone()
            );
        }
    }

    public static class SourceSelector implements Cloneable {
        private final List<SourceSelectorEntry> preferredSources;

        public SourceSelector(List<SourceSelectorEntry> preferredSources) {
            this.preferredSources = preferredSources;
        }

        public SourceSelector(SourceSelectorEntry... preferredSources) {
            this.preferredSources = Arrays.asList(preferredSources);
        }

        public <T extends CvssVector> T selectVector(Collection<T> vectors) {
            for (SourceSelectorEntry source : preferredSources) {
                for (T vector : vectors) {
                    if (source.matches(vector.getCvssSource())) {
                        return vector;
                    }
                }
            }
            return null;
        }

        public CvssVector selectVectorUnchecked(Collection<CvssVector> vectors) {
            for (SourceSelectorEntry source : preferredSources) {
                for (CvssVector vector : vectors) {
                    if (source.matches(vector.getCvssSource())) {
                        return vector;
                    }
                }
            }
            return null;
        }

        public JSONArray toJson() {
            final JSONArray preferredSources = new JSONArray();
            for (SourceSelectorEntry entry : this.preferredSources) {
                preferredSources.put(entry.toJson());
            }
            return preferredSources;
        }

        public static SourceSelector fromJson(Object json) {
            if (json instanceof JSONArray) {
                return fromJson((JSONArray) json);
            } else if (json instanceof JSONObject) {
                return fromJson((JSONObject) json);
            } else {
                throw new IllegalArgumentException(SourceSelector.class.getSimpleName() + ": json must be either a JSONArray or JSONObject");
            }
        }

        public static SourceSelector fromJson(JSONArray json) {
            if (json == null) {
                throw new IllegalArgumentException(SourceSelector.class.getSimpleName() + ": json must not be null when creating from json");
            }
            final List<SourceSelectorEntry> entries = new ArrayList<>();
            for (int i = 0; i < json.length(); i++) {
                entries.add(SourceSelectorEntry.fromJson(json.getJSONObject(i)));
            }
            return new SourceSelector(entries);
        }

        public static SourceSelector fromJson(JSONObject json) {
            final JSONArray preferredSources = json.optJSONArray("preferredSources");
            if (preferredSources == null) {
                throw new IllegalArgumentException(SourceSelector.class.getSimpleName() + ": json must contain a 'preferredSources' array");
            }
            return fromJson(preferredSources);
        }

        public List<SourceSelectorEntry> getPreferredSources() {
            return preferredSources;
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public SourceSelector clone() {
            return new SourceSelector(this.preferredSources.stream().map(SourceSelectorEntry::clone).collect(Collectors.toList()));
        }
    }

    public static class SourceSelectorEntry implements Cloneable {
        private final List<SourceSelectorEntryEntry<CvssEntity>> hostingEntities;
        private final List<SourceSelectorEntryEntry<CvssEntity>> issuingEntities;
        private final List<SourceSelectorEntryEntry<CvssIssuingEntityRole>> issuingEntityRoles;

        public SourceSelectorEntry(List<SourceSelectorEntryEntry<CvssEntity>> hostingEntities, List<SourceSelectorEntryEntry<CvssIssuingEntityRole>> issuingEntityRoles, List<SourceSelectorEntryEntry<CvssEntity>> issuingEntities) {
            this.hostingEntities = hostingEntities;
            this.issuingEntityRoles = issuingEntityRoles;
            this.issuingEntities = issuingEntities;
        }

        public SourceSelectorEntry(CvssEntity hostingEntity, CvssIssuingEntityRole issuingEntityRole, CvssEntity issuingEntity,
                                   boolean invertedHostingEntity, boolean invertedIssuingEntity, boolean invertedIssuingEntityRole) {
            this.hostingEntities = hostingEntity == null && !invertedHostingEntity ? Collections.emptyList() : Collections.singletonList(new SourceSelectorEntryEntry<>(hostingEntity, invertedHostingEntity));
            this.issuingEntityRoles = issuingEntityRole == null && !invertedIssuingEntity ? Collections.emptyList() : Collections.singletonList(new SourceSelectorEntryEntry<>(issuingEntityRole, invertedIssuingEntityRole));
            this.issuingEntities = issuingEntity == null && !invertedIssuingEntityRole ? Collections.emptyList() : Collections.singletonList(new SourceSelectorEntryEntry<>(issuingEntity, invertedIssuingEntity));
        }

        public SourceSelectorEntry(CvssEntity hostingEntity, CvssIssuingEntityRole issuingEntityRole, CvssEntity issuingEntity) {
            this(hostingEntity, issuingEntityRole, issuingEntity, false, false, false);
        }

        public List<SourceSelectorEntryEntry<CvssEntity>> getHostingEntities() {
            return hostingEntities;
        }

        public List<SourceSelectorEntryEntry<CvssEntity>> getIssuingEntities() {
            return issuingEntities;
        }

        public List<SourceSelectorEntryEntry<CvssIssuingEntityRole>> getIssuingEntityRoles() {
            return issuingEntityRoles;
        }

        public boolean matches(CvssSource source) {
            if (source == null) return false;

            final boolean matchesHost = matchListWithSource(hostingEntities, source.getHostingEntity());
            final boolean matchesIssuer = matchListWithSource(issuingEntities, source.getIssuingEntity());
            final boolean matchesRole = matchListWithSource(issuingEntityRoles, source.getIssuingEntityRole());

            return matchesHost && matchesIssuer && matchesRole;
        }

        private <T extends CvssSource.EntityNameProvider> boolean matchListWithSource(List<SourceSelectorEntryEntry<T>> list, T sourceAttribute) {
            // all of these operators have been chosen carefully, please only change them if you know what you are doing
            if (sourceAttribute == null && list.isEmpty()) {
                return true;
            } else {
                return !list.isEmpty() && list.stream().allMatch(entry -> entry.matches(sourceAttribute));
            }
        }

        public JSONObject toJson() {
            return new JSONObject()
                    .put("host", new JSONArray(this.hostingEntities.stream().map(String::valueOf).collect(Collectors.toList())))
                    .put("issuerRole", new JSONArray(this.issuingEntityRoles.stream().map(String::valueOf).collect(Collectors.toList())))
                    .put("issuer", new JSONArray(this.issuingEntities.stream().map(String::valueOf).collect(Collectors.toList())));
        }

        public static SourceSelectorEntry fromJson(JSONObject json) {
            try {
                final List<SourceSelectorEntryEntry<CvssEntity>> hostingEntities = new ArrayList<>();
                final List<SourceSelectorEntryEntry<CvssIssuingEntityRole>> issuingEntityRoles = new ArrayList<>();
                final List<SourceSelectorEntryEntry<CvssEntity>> issuingEntities = new ArrayList<>();

                // {"hostingEntities":[{"inverted":false,"value":{"organizationTypes":[],"reportSteps":[],"name":"NVD"}}],"issuingEntityRoles":[{"inverted":false,"value":{"name":"CNA"}}],"issuingEntities":[{"inverted":false,"value":{"organizationTypes":[],"reportSteps":[],"name":"NVD"}}]}

                final JSONArray host = ObjectUtils.firstNonNull(json.optJSONArray("host"), json.optJSONArray("hostingEntities"));
                for (int i = 0; i < host.length(); i++) {
                    hostingEntities.add(SourceSelectorEntryEntry.from(host.get(i), CvssEntity::new));
                }
                final JSONArray issuerRole = ObjectUtils.firstNonNull(json.optJSONArray("issuerRole"), json.optJSONArray("issuingEntityRoles"));
                for (int i = 0; i < issuerRole.length(); i++) {
                    issuingEntityRoles.add(SourceSelectorEntryEntry.from(issuerRole.get(i), CvssIssuingEntityRole::new));
                }
                final JSONArray issuer = ObjectUtils.firstNonNull(json.optJSONArray("issuer"), json.optJSONArray("issuingEntities"));
                for (int i = 0; i < issuer.length(); i++) {
                    issuingEntities.add(SourceSelectorEntryEntry.from(issuer.get(i), CvssEntity::new));
                }

                return new SourceSelectorEntry(hostingEntities, issuingEntityRoles, issuingEntities);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse SourceSelectorEntry from json: " + json, e);
            }
        }

        private static String getPotentiallyInvertedOrNullName(CvssSource.EntityNameProvider nameProvider, boolean inverted) {
            if (nameProvider == null) return null;
            if (inverted) {
                return "not:" + nameProvider.getName();
            } else {
                return nameProvider.getName();
            }
        }

        private static <T> Pair<T, Boolean> extractPotentiallyInverted(String value, Function<String, T> extractor) {
            if (value == null) return Pair.of(null, false);
            if (value.startsWith("not:")) {
                return Pair.of(extractor.apply(value.substring(4)), true);
            } else {
                return Pair.of(extractor.apply(value), false);
            }
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public SourceSelectorEntry clone() {
            return new SourceSelectorEntry(this.hostingEntities.stream().map(SourceSelectorEntryEntry::clone).collect(Collectors.toList()),
                    this.issuingEntityRoles.stream().map(SourceSelectorEntryEntry::clone).collect(Collectors.toList()),
                    this.issuingEntities.stream().map(SourceSelectorEntryEntry::clone).collect(Collectors.toList()));
        }

        public final static CvssEntity ANY_ENTITY = new CvssEntity("*");
        public final static CvssIssuingEntityRole ANY_ROLE = new CvssIssuingEntityRole("*");

        public final static CvssEntity EMPTY_ENTITY = null;
        public final static CvssIssuingEntityRole EMPTY_ROLE = null;
    }

    public static class SourceSelectorEntryEntry<T extends CvssSource.EntityNameProvider> implements Cloneable {
        private final T value;
        private final boolean inverted;

        public SourceSelectorEntryEntry(T value, boolean inverted) {
            this.value = value;
            this.inverted = inverted;
        }

        public SourceSelectorEntryEntry(T value) {
            this(value, false);
        }

        protected boolean potentiallyInvert(boolean value) {
            return inverted != value;
        }

        /**
         * Follows these rules. Horizontal is this instance, vertical is the parameter.
         * <pre>
         * |       | ANY | value | null |
         * |-------|-----|-------|------|
         * | ANY   | Y   | Y     | Y    |
         * | value | Y   | ==    | N    |
         * | null  | N   | N     | Y    |
         * </pre>
         * <p>
         * Inverted if {@link #inverted} is true.
         *
         * @param checkValue The value to compare this instances value to.
         * @return true if the values match, false otherwise.
         */
        public boolean matches(CvssSource.EntityNameProvider checkValue) {
            final String checkName = checkValue == null ? null : checkValue.getName();
            final String thisName = value == null ? null : value.getName();

            if (checkName == null && thisName == null) return potentiallyInvert(true);

            if (SourceSelectorEntry.ANY_ENTITY.getName().equals(thisName)) {
                return potentiallyInvert(true);
            }

            if (checkName == null || thisName == null) return potentiallyInvert(false);

            if (SourceSelectorEntry.ANY_ENTITY.getName().equals(checkName)) {
                return potentiallyInvert(true);
            }

            if (checkName.equals(thisName)) return potentiallyInvert(true);

            return potentiallyInvert(false);
        }

        public T getValue() {
            return value;
        }

        public boolean isInverted() {
            return inverted;
        }

        @Override
        public String toString() {
            return (inverted ? "not:" : "") + (value == null ? "" : value.getName());
        }

        public static <T extends CvssSource.EntityNameProvider> SourceSelectorEntryEntry<T> from(Object value, Function<String, T> extractor) {
            if (value == null) return null;
            if (value instanceof String) {
                return fromString((String) value, extractor);
            } else if (value instanceof JSONObject) {
                return fromJson((JSONObject) value, extractor);
            } else {
                throw new IllegalArgumentException("Cannot create SourceSelectorEntryEntry from " + value);
            }
        }

        protected static <T extends CvssSource.EntityNameProvider> SourceSelectorEntryEntry<T> fromString(String value, Function<String, T> extractor) {
            if (value == null) return null;
            if (value.startsWith("not:")) {
                final String param = value.substring(4);
                return new SourceSelectorEntryEntry<>(extractor.apply(param.isEmpty() ? null : param), true);
            } else {
                return new SourceSelectorEntryEntry<>(extractor.apply(value.isEmpty() ? null : value), false);
            }
        }

        protected static <T extends CvssSource.EntityNameProvider> SourceSelectorEntryEntry<T> fromJson(JSONObject json, Function<String, T> extractor) {
            final boolean inverted = json.optBoolean("inverted", false);
            final JSONObject valueJson = json.optJSONObject("value");
            if (valueJson == null) {
                throw new IllegalArgumentException("SourceSelectorEntryEntry 'value' must be a JSONObject: " + json);
            }
            return new SourceSelectorEntryEntry<>(extractor.apply(valueJson.optString("name", null)), inverted);
        }

        @Override
        public SourceSelectorEntryEntry<T> clone() {
            return new SourceSelectorEntryEntry<>(this.value, this.inverted);
        }
    }

    public static class SelectorStatsCollector implements Cloneable {
        private final String attributeName;
        private final StatsCollectorProvider provider;
        private final StatsCollectorSetType setType;

        public SelectorStatsCollector(String attributeName, StatsCollectorProvider provider, StatsCollectorSetType setType) {
            this.attributeName = attributeName;
            this.provider = provider;
            this.setType = setType;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public StatsCollectorProvider getProvider() {
            return provider;
        }

        public StatsCollectorSetType getSetType() {
            return setType;
        }

        public void apply(Map<String, Integer> stats, int presence, int absence, Supplier<Integer> sizeSupplier) {
            final int value;

            if (provider == StatsCollectorProvider.PRESENCE) {
                value = presence;
            } else if (provider == StatsCollectorProvider.ABSENCE) {
                value = absence;
            } else if (provider == StatsCollectorProvider.APPLIED_PARTS_COUNT) {
                value = sizeSupplier.get();
            } else {
                throw new IllegalStateException("Unknown provider: " + provider);
            }

            if (setType == StatsCollectorSetType.ADD) {
                stats.merge(attributeName, value, Integer::sum);
            } else if (setType == StatsCollectorSetType.SUBTRACT) {
                stats.merge(attributeName, value, (a, b) -> a - b);
            } else if (setType == StatsCollectorSetType.SET) {
                stats.put(attributeName, value);
            } else if (setType == StatsCollectorSetType.MAX) {
                stats.merge(attributeName, value, Integer::max);
            } else if (setType == StatsCollectorSetType.MIN) {
                stats.merge(attributeName, value, Integer::min);
            } else {
                throw new IllegalStateException("Unknown set type: " + setType);
            }
        }

        public JSONObject toJson() {
            return new JSONObject()
                    .put("attribute", attributeName)
                    .put("provider", provider.name())
                    .put("setType", setType.name());
        }

        public static SelectorStatsCollector fromJson(JSONObject json) {
            return new SelectorStatsCollector(
                    json.getString("attribute"),
                    StatsCollectorProvider.valueOf(json.getString("provider")),
                    StatsCollectorSetType.valueOf(json.getString("setType"))
            );
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public SelectorStatsCollector clone() {
            return new SelectorStatsCollector(this.attributeName, this.provider, this.setType);
        }
    }

    public static class SelectorVectorEvaluator implements Cloneable {
        private final Map<VectorEvaluatorOperation, Boolean> operations; // AND; can be inverted
        private final EvaluatorAction action;

        public SelectorVectorEvaluator(Map<VectorEvaluatorOperation, Boolean> operations, EvaluatorAction action) {
            this.operations = operations;
            this.action = action;
        }

        public SelectorVectorEvaluator(VectorEvaluatorOperation operation, boolean inverted, EvaluatorAction action) {
            this.operations = Collections.singletonMap(operation, inverted);
            this.action = action;
        }

        public SelectorVectorEvaluator(VectorEvaluatorOperation operation, EvaluatorAction action) {
            this.operations = Collections.singletonMap(operation, false);
            this.action = action;
        }

        public boolean evaluate(CvssVector vector) {
            for (Map.Entry<VectorEvaluatorOperation, Boolean> entry : operations.entrySet()) {
                final VectorEvaluatorOperation operation = entry.getKey();
                final Boolean isInverted = entry.getValue();
                if (vector == null) {
                    if (operation == VectorEvaluatorOperation.IS_NULL) {
                        return !isInverted;
                    } else {
                        return isInverted;
                    }
                }
                final boolean result = operation.test(vector);
                final boolean effective = isInverted != result;
                if (!effective) return false;
            }
            return true;
        }

        public Map<VectorEvaluatorOperation, Boolean> getOperations() {
            return operations;
        }

        public EvaluatorAction getAction() {
            return action;
        }

        public JSONObject toJson() {
            return new JSONObject()
                    .put("action", action.name())
                    .put("and", new JSONArray(operations.entrySet().stream().map(e -> e.getKey().toPotentiallyInvertedString(e.getValue())).collect(Collectors.toList())));
        }

        public static SelectorVectorEvaluator fromJson(JSONObject json) {
            final Map<VectorEvaluatorOperation, Boolean> operations = new HashMap<>();
            final JSONArray jsonOperations = json.getJSONArray("and");
            for (int i = 0; i < jsonOperations.length(); i++) {
                final String operation = jsonOperations.getString(i);
                final Pair<VectorEvaluatorOperation, Boolean> pair = VectorEvaluatorOperation.extractPotentiallyInverted(operation);
                operations.put(pair.getLeft(), pair.getRight());
            }
            return new SelectorVectorEvaluator(operations, EvaluatorAction.valueOf(json.getString("action")));
        }

        private static List<SelectorVectorEvaluator> fromParentJson(JSONObject json) {
            final JSONArray vectorEvaluators = json.optJSONArray("vectorEval");
            if (vectorEvaluators != null) {
                final List<SelectorVectorEvaluator> selectorVectorEvaluators = new ArrayList<>();
                for (int i = 0; i < vectorEvaluators.length(); i++) {
                    selectorVectorEvaluators.add(SelectorVectorEvaluator.fromJson(vectorEvaluators.getJSONObject(i)));
                }
                return selectorVectorEvaluators;
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public SelectorVectorEvaluator clone() {
            return new SelectorVectorEvaluator(new HashMap<>(this.operations), action);
        }
    }

    public static class SelectorStatsEvaluator implements Cloneable {
        private final String attributeName;
        private final StatsEvaluatorOperation comparator;
        private final EvaluatorAction action;
        private final Integer comparisonValue;

        public SelectorStatsEvaluator(String attributeName, StatsEvaluatorOperation comparator, EvaluatorAction action, Integer comparisonValue) {
            this.attributeName = attributeName;
            this.comparator = comparator;
            this.action = action;
            this.comparisonValue = comparisonValue;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public StatsEvaluatorOperation getComparator() {
            return comparator;
        }

        public EvaluatorAction getAction() {
            return action;
        }

        public Integer getComparisonValue() {
            return comparisonValue;
        }

        public JSONObject toJson() {
            return new JSONObject()
                    .put("attribute", attributeName)
                    .put("comparator", comparator.name())
                    .put("action", action.name())
                    .put("value", comparisonValue);
        }

        public static SelectorStatsEvaluator fromJson(JSONObject json) {
            return new SelectorStatsEvaluator(
                    json.getString("attribute"),
                    StatsEvaluatorOperation.valueOf(json.getString("comparator")),
                    EvaluatorAction.valueOf(json.getString("action")),
                    json.getInt("value")
            );
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public SelectorStatsEvaluator clone() {
            return new SelectorStatsEvaluator(this.attributeName, this.comparator, this.action, comparisonValue);
        }
    }

    public enum MergingMethod {
        /**
         * Represents a merging method that applies all parts of the newCvssVector to the base CvssVector.
         */
        ALL((base, newVector) -> {
            final CvssVector clone = base.clone();
            final int parts = clone.applyVector(newVector.toString());
            return Pair.of(clone, parts);
        }),
        /**
         * Represents a merging method that selectively includes attributes.
         * Apply all parts of the newVector to the base CvssVector if it results in a lower/equal overall score.
         */
        LOWER((base, newVector) -> {
            final CvssVector clone = base.clone();
            final int parts = clone.applyVectorPartsIfLower(newVector.toString(), CvssVector::getOverallScore);
            return Pair.of(clone, parts);
        }),
        /**
         * Represents a merging method that selectively includes attributes.
         * Apply all parts of the newVector to the base CvssVector if it results in a higher/equal overall score.
         */
        HIGHER((base, newVector) -> {
            final CvssVector clone = base.clone();
            final int parts = clone.applyVectorPartsIfHigher(newVector.toString(), CvssVector::getOverallScore);
            return Pair.of(clone, parts);
        }),
        /**
         * Represents a merging method that selectively includes attributes.
         * Apply all parts of the newVector to the base CvssVector if the metric is ranked as less/equal severe.
         */
        LOWER_METRIC((base, newVector) -> {
            final CvssVector clone = base.clone();
            final int parts = clone.applyVectorPartsIfMetricsLower(newVector.toString());
            return Pair.of(clone, parts);
        }),
        /**
         * Represents a merging method that selectively includes attributes.
         * Apply all parts of the newVector to the base CvssVector if the metric is ranked as more/equal severe.
         */
        HIGHER_METRIC((base, newVector) -> {
            final CvssVector clone = base.clone();
            final int parts = clone.applyVectorPartsIfMetricsHigher(newVector.toString());
            return Pair.of(clone, parts);
        }),
        /**
         * Represents a merging method that overwrites the full base CvssVector with the newVector.
         */
        OVERWRITE((base, newVector) -> Pair.of(newVector.clone(), 0));

        private final BiFunction<CvssVector, CvssVector, Pair<CvssVector, Integer>> mergingFunction;

        MergingMethod(BiFunction<CvssVector, CvssVector, Pair<CvssVector, Integer>> mergingFunction) {
            this.mergingFunction = mergingFunction;
        }

        public <T extends CvssVector> Pair<T, Integer> mergeVectors(CvssVector base, CvssVector newVector) {
            return (Pair<T, Integer>) mergingFunction.apply(base, newVector);
        }
    }

    public enum StatsCollectorProvider {
        PRESENCE,
        ABSENCE,
        APPLIED_PARTS_COUNT
    }

    public enum StatsCollectorSetType {
        ADD,
        SUBTRACT,
        SET,
        MAX,
        MIN
    }

    public enum StatsEvaluatorOperation {
        EQUAL(Objects::equals),
        SMALLER((left, right) -> left < right),
        SMALLER_OR_EQUAL((left, right) -> left <= right),
        GREATER((left, right) -> left > right),
        GREATER_OR_EQUAL((left, right) -> left >= right);

        private final BiPredicate<Integer, Integer> predicate;

        StatsEvaluatorOperation(BiPredicate<Integer, Integer> predicate) {
            this.predicate = predicate;
        }

        public boolean test(int left, int right) {
            return predicate.test(left, right);
        }
    }

    public enum VectorEvaluatorOperation {
        IS_NULL(Objects::isNull),
        IS_BASE_FULLY_DEFINED(CvssVector::isBaseFullyDefined),
        IS_BASE_PARTIALLY_DEFINED(CvssVector::isAnyBaseDefined),
        IS_ENVIRONMENTAL_PARTIALLY_DEFINED(vector -> vector instanceof MultiScoreCvssVector ? (((MultiScoreCvssVector) vector).isAnyEnvironmentalDefined()) : (vector instanceof Cvss4P0 ? ((Cvss4P0) vector).isAnyEnvironmentalDefined() : false)),
        IS_TEMPORAL_PARTIALLY_DEFINED(vector -> vector instanceof MultiScoreCvssVector ? (((MultiScoreCvssVector) vector).isAnyTemporalDefined()) : false),
        IS_THREAT_PARTIALLY_DEFINED(vector -> vector instanceof MultiScoreCvssVector ? false : (vector instanceof Cvss4P0 ? ((Cvss4P0) vector).isAnyThreatDefined() : false));

        private final Predicate<CvssVector> predicate;

        VectorEvaluatorOperation(Predicate<CvssVector> predicate) {
            this.predicate = predicate;
        }

        public boolean test(CvssVector vector) {
            return predicate.test(vector);
        }

        public String toPotentiallyInvertedString(boolean inverted) {
            return (inverted ? "not:" : "") + name();
        }

        public String toPotentiallyInvertedPrettyString(boolean inverted) {
            return (inverted ? "not " : "") + name().toLowerCase().replace("_", " ").replace("is", "").replaceAll(" +", " ").replace("null", "not defined").trim();
        }

        public static Pair<VectorEvaluatorOperation, Boolean> extractPotentiallyInverted(String value) {
            if (value == null) return Pair.of(null, false);
            if (value.startsWith("not:")) {
                return Pair.of(VectorEvaluatorOperation.valueOf(value.substring(4)), true);
            } else {
                return Pair.of(VectorEvaluatorOperation.valueOf(value), false);
            }
        }
    }

    public enum EvaluatorAction {
        FAIL,
        RETURN_NULL,
        SKIP,
        RETURN_PREVIOUS
    }
}
