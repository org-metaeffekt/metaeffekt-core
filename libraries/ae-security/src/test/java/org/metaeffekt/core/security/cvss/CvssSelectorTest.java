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
package org.metaeffekt.core.security.cvss;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.security.cvss.CvssSource.CvssIssuingEntityRole;
import org.metaeffekt.core.security.cvss.processor.CvssSelector;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.metaeffekt.core.security.cvss.processor.CvssSelector.*;

public class CvssSelectorTest {

    public final static CvssSelector CVSS_SELECTOR_INITIAL = new CvssSelector(Collections.singletonList(
            new CvssRule(MergingMethod.ALL,
                    // NIST NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD),
                    // MSRC
                    new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.MSRC),
                    // GHSA
                    new SourceSelectorEntry(KnownCvssEntities.GHSA, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA),

                    // OSV
                    new SourceSelectorEntry(KnownCvssEntities.OSV, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.GHSA),
                    new SourceSelectorEntry(KnownCvssEntities.OSV, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),

                    // CSAF
                    new SourceSelectorEntry(KnownCvssEntities.CSAF, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),

                    // other NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    // CERT-SEI
                    new SourceSelectorEntry(KnownCvssEntities.CERT_SEI, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    // any other, but not assessment
                    new SourceSelectorEntry(
                            Arrays.asList(new SourceSelectorEntryEntry<>(KnownCvssEntities.ASSESSMENT, true)),
                            Arrays.asList(new SourceSelectorEntryEntry<>(SourceSelectorEntry.ANY_ROLE)),
                            Arrays.asList(new SourceSelectorEntryEntry<>(SourceSelectorEntry.ANY_ENTITY))
                    )
            )
    ));

    public final static CvssSelector CVSS_SELECTOR_CONTEXT = new CvssSelector(Arrays.asList(
            new CvssRule(MergingMethod.ALL,
                    // NIST NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD),
                    // MSRC
                    new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.MSRC),
                    // GHSA
                    new SourceSelectorEntry(KnownCvssEntities.GHSA, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA),

                    // OSV
                    new SourceSelectorEntry(KnownCvssEntities.OSV, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.GHSA),
                    new SourceSelectorEntry(KnownCvssEntities.OSV, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),

                    // CSAF
                    new SourceSelectorEntry(KnownCvssEntities.CSAF, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),

                    // other NVD
                    new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    // CERT-SEI
                    new SourceSelectorEntry(KnownCvssEntities.CERT_SEI, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                    // any other, but not assessment
                    new SourceSelectorEntry(
                            Arrays.asList(new SourceSelectorEntryEntry<>(KnownCvssEntities.ASSESSMENT, true)),
                            Arrays.asList(new SourceSelectorEntryEntry<>(SourceSelectorEntry.ANY_ROLE)),
                            Arrays.asList(new SourceSelectorEntryEntry<>(SourceSelectorEntry.ANY_ENTITY))
                    )
            ),
            // assessment
            new CvssRule(MergingMethod.ALL,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_ALL)),
            new CvssRule(MergingMethod.LOWER,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER)),
            new CvssRule(MergingMethod.HIGHER,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_HIGHER)),
            new CvssRule(MergingMethod.LOWER_METRIC,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER_METRIC)),
            new CvssRule(MergingMethod.HIGHER_METRIC,
                    Collections.singletonList(new SelectorStatsCollector("assessment", CvssSelector.StatsCollectorProvider.PRESENCE, CvssSelector.StatsCollectorSetType.ADD)),
                    Collections.emptyList(),
                    new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_HIGHER_METRIC))
    ), Collections.singletonList(
            new SelectorStatsEvaluator("assessment", CvssSelector.StatsEvaluatorOperation.EQUAL, CvssSelector.EvaluatorAction.RETURN_NULL, 0)
    ), Collections.singletonList(
            new SelectorVectorEvaluator(CvssSelector.VectorEvaluatorOperation.IS_BASE_FULLY_DEFINED, true, CvssSelector.EvaluatorAction.RETURN_NULL)
    ));

    @Test
    public void overwritePreviousVectorTest() {
        CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL, new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY)),
                new CvssRule(MergingMethod.OVERWRITE, new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY))
        ));

        CvssVector nvdVector = new Cvss3P1("", new CvssSource(KnownCvssEntities.NVD, null, null, Cvss3P1.class));
        CvssVector msrcVector = new Cvss3P1("", new CvssSource(KnownCvssEntities.MSRC, null, null, Cvss3P1.class));

        CvssVector effective = selector.selectVector(Arrays.asList(nvdVector, msrcVector));
        assertEquals(msrcVector, effective);
    }

    @Test
    public void combineVectorsWithToJsonTest() {
        final CvssSelector baseSelector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL,
                        new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY),
                        new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD),
                        new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY)
                ),
                new CvssRule(MergingMethod.ALL,
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_ALL)
                ),
                new CvssRule(MergingMethod.LOWER,
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER)
                ),
                new CvssRule(MergingMethod.HIGHER,
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_HIGHER)
                )
        ));

        final CvssSelector reParsedSelector = fromJson(baseSelector.toJson());

        Cvss3P1 baseMsrcVector = new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.MSRC, null, null, Cvss3P1.class));
        Cvss3P1 baseNvdNvdVector = new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class));
        Cvss3P1 baseNvdGhsaVector = new Cvss3P1("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA, Cvss3P1.class));

        Cvss3P1 assessmentAllPartsVector = new Cvss3P1("CVSS:3.1/MAV:N", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_ALL, Cvss3P1.class));
        Cvss3P1 assessmentLowerPartsMacVector = new Cvss3P1("CVSS:3.1/MAC:H", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER, Cvss3P1.class));
        Cvss3P1 assessmentHigherPartsMacVector = new Cvss3P1("CVSS:3.1/MAC:L", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_HIGHER, Cvss3P1.class));
        Cvss3P1 assessmentHigherPartsMavVector = new Cvss3P1("CVSS:3.1/MAV:L", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_HIGHER, Cvss3P1.class));

        // picks baseNvdNvdVector
        assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", reParsedSelector.selectVector(Arrays.asList(baseMsrcVector, baseNvdNvdVector, baseNvdGhsaVector)).toString());

        // picks baseNvdGhsaVector + assessmentAllPartsVector
        assertEquals("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MAV:N", reParsedSelector.selectVector(Arrays.asList(baseMsrcVector, baseNvdGhsaVector, assessmentAllPartsVector)).toString());

        // picks baseNvdGhsaVector + assessmentLowerPartsMacVector
        assertEquals("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MAC:H", reParsedSelector.selectVector(Arrays.asList(baseMsrcVector, baseNvdGhsaVector, assessmentLowerPartsMacVector)).toString());

        // picks baseNvdGhsaVector + assessmentHigherPartsMacVector
        assertEquals("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MAC:L", reParsedSelector.selectVector(Arrays.asList(baseMsrcVector, baseNvdGhsaVector, assessmentHigherPartsMacVector)).toString());

        // picks baseNvdNvdVector + assessmentHigherPartsMavVector, but assessmentHigherPartsMavVector is not applied because MAV:L is lower than MAV:N
        assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", reParsedSelector.selectVector(Arrays.asList(baseMsrcVector, baseNvdNvdVector, assessmentHigherPartsMavVector)).toString());
    }

    @Test
    public void selectorEmptyEntityTest() {
        CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL,
                        new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY),
                        new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY),
                        new SourceSelectorEntry(SourceSelectorEntry.ANY_ENTITY, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY))
        ));
        CvssSelector reParsedSelector = fromJson(selector.toJson());

        Cvss3P1 nvdEmptyVector = new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, null, null, Cvss3P1.class));
        Cvss3P1 nvdNvdVector = new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class));
        Cvss3P1 msrcEmptyVector = new Cvss3P1("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.MSRC, null, null, Cvss3P1.class));


        // picks nvdEmptyVector
        assertEquals(nvdEmptyVector.toString(), selector.selectVector(Arrays.asList(nvdEmptyVector, nvdNvdVector, msrcEmptyVector)).toString());
        assertEquals(nvdEmptyVector.toString(), reParsedSelector.selectVector(Arrays.asList(nvdEmptyVector, nvdNvdVector, msrcEmptyVector)).toString());

        // picks msrcEmptyVector
        assertEquals(msrcEmptyVector.toString(), selector.selectVector(Arrays.asList(nvdNvdVector, msrcEmptyVector)).toString());
        assertEquals(msrcEmptyVector.toString(), reParsedSelector.selectVector(Arrays.asList(nvdNvdVector, msrcEmptyVector)).toString());

        // picks nvdNvdVector
        assertEquals(nvdNvdVector.toString(), selector.selectVector(Collections.singletonList(nvdNvdVector)).toString());
        assertEquals(nvdNvdVector.toString(), reParsedSelector.selectVector(Collections.singletonList(nvdNvdVector)).toString());
    }

    @Test
    public void invertedSelectorTest() {
        CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL,
                        new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY, true, false, false))
        ));

        Cvss3P1 nvdEmptyVector = new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, null, null, Cvss3P1.class));
        Cvss3P1 assessmentAllPartsVector = new Cvss3P1("CVSS:3.1/MAV:N", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_ALL, Cvss3P1.class));

        // picks nvdEmptyVector
        assertEquals(nvdEmptyVector.toString(), selector.selectVector(Arrays.asList(nvdEmptyVector, assessmentAllPartsVector)).toString());

        // picks none
        assertNull(selector.selectVector(Collections.singletonList(assessmentAllPartsVector)));
    }

    @Test
    public void emptyInvertedSelectorTest() {
        CvssSelector baseSelector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL,
                        new SourceSelectorEntry(SourceSelectorEntry.EMPTY_ENTITY, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY, true, true, true))
        ));
        CvssSelector anySelector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL,
                        new SourceSelectorEntry(SourceSelectorEntry.ANY_ENTITY, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY))
        ));

        Cvss3P1 nvdVector = new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, null, null, Cvss3P1.class));
        Cvss3P1 fullNvdVector = new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class));
        Cvss3P1 emptyVector = new Cvss3P1("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(null, null, null, Cvss3P1.class));

        // picks fullNvdVector
        assertEquals(fullNvdVector.toString(), baseSelector.selectVector(Arrays.asList(nvdVector, emptyVector, fullNvdVector)).toString());

        // does not find any
        assertNull(baseSelector.selectVector(Arrays.asList(nvdVector, emptyVector)));

        final CvssSelector reParsedSelector = fromJson(baseSelector.toJson());

        // picks fullNvdVector
        assertEquals(fullNvdVector.toString(), reParsedSelector.selectVector(Arrays.asList(nvdVector, emptyVector, fullNvdVector)).toString());

        // does not find any
        assertNull(reParsedSelector.selectVector(Arrays.asList(nvdVector, emptyVector)));

        // picks nvdVector, as it is first no matter what
        assertEquals(nvdVector.toString(), anySelector.selectVector(Arrays.asList(nvdVector, emptyVector, fullNvdVector)).toString());
    }

    @Test
    public void multipleInvertedSelectorPartsTest() {
        CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL,
                        new SourceSelectorEntry(
                                Arrays.asList(
                                        new SourceSelectorEntryEntry<>(KnownCvssEntities.ASSESSMENT, true),
                                        new SourceSelectorEntryEntry<>(KnownCvssEntities.GHSA, true)
                                ),
                                Arrays.asList(new SourceSelectorEntryEntry<>(SourceSelectorEntry.ANY_ROLE, false)),
                                Arrays.asList(new SourceSelectorEntryEntry<>(SourceSelectorEntry.ANY_ENTITY, false))
                        ))
        ));

        Cvss3P1 nvdVector = new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, null, null, Cvss3P1.class));
        Cvss3P1 fullNvdVector = new Cvss3P1("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class));
        Cvss3P1 ghsaVector = new Cvss3P1("CVSS:3.1/AV:A/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.GHSA, null, null, Cvss3P1.class));
        Cvss3P1 emptyVector = new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(null, null, null, Cvss3P1.class));
        Cvss3P1 assessmentVector = new Cvss3P1("CVSS:3.1/AV:L/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.ASSESSMENT, null, null, Cvss3P1.class));

        // picks first that is not assessmentVector or ghsaVector
        assertEquals(nvdVector.toString(), selector.selectVector(Arrays.asList(assessmentVector, nvdVector, emptyVector, fullNvdVector)).toString());
        assertEquals(emptyVector.toString(), selector.selectVector(Arrays.asList(assessmentVector, emptyVector, fullNvdVector)).toString());
        assertEquals(fullNvdVector.toString(), selector.selectVector(Arrays.asList(assessmentVector, fullNvdVector)).toString());
        assertEquals(nvdVector.toString(), selector.selectVector(Arrays.asList(ghsaVector, nvdVector, emptyVector, fullNvdVector)).toString());
        assertEquals(emptyVector.toString(), selector.selectVector(Arrays.asList(ghsaVector, emptyVector, fullNvdVector)).toString());
        assertEquals(fullNvdVector.toString(), selector.selectVector(Arrays.asList(ghsaVector, fullNvdVector)).toString());

        // does not find any
        assertNull(selector.selectVector(Arrays.asList(assessmentVector)));
        assertNull(selector.selectVector(Arrays.asList(ghsaVector)));
        assertNull(selector.selectVector(Arrays.asList(assessmentVector, ghsaVector)));
    }

    @Test
    public void notFoundStrategyTest() {
        CvssSelector failSelector = new CvssSelector(Arrays.asList(new CvssRule(MergingMethod.ALL, Collections.emptyList(),
                Arrays.asList(new SelectorVectorEvaluator(VectorEvaluatorOperation.IS_NULL, EvaluatorAction.FAIL)),
                new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY))));
        CvssSelector returnNullSelector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL, Collections.emptyList(), Collections.emptyList(), new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY)),
                new CvssRule(MergingMethod.ALL, Collections.emptyList(),
                        Arrays.asList(new SelectorVectorEvaluator(VectorEvaluatorOperation.IS_NULL, EvaluatorAction.RETURN_NULL)),
                        new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA)),
                new CvssRule(MergingMethod.ALL, Collections.emptyList(), Collections.emptyList(), new SourceSelectorEntry(KnownCvssEntities.CERT_SEI, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY))
        ));
        CvssSelector returnPreviousSelector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL, Collections.emptyList(), Collections.emptyList(), new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY)),
                new CvssRule(MergingMethod.ALL, Collections.emptyList(),
                        Arrays.asList(new SelectorVectorEvaluator(VectorEvaluatorOperation.IS_NULL, EvaluatorAction.RETURN_PREVIOUS)),
                        new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA)),
                new CvssRule(MergingMethod.ALL, Collections.emptyList(), Collections.emptyList(), new SourceSelectorEntry(KnownCvssEntities.CERT_SEI, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY))
        ));

        notFoundStrategyTestAssertions(failSelector, returnNullSelector, returnPreviousSelector);

        failSelector = fromJson(failSelector.toJson());
        returnNullSelector = fromJson(returnNullSelector.toJson());
        returnPreviousSelector = fromJson(returnPreviousSelector.toJson());

        notFoundStrategyTestAssertions(failSelector, returnNullSelector, returnPreviousSelector);
    }

    private static void notFoundStrategyTestAssertions(CvssSelector failSelector, CvssSelector returnNullSelector, CvssSelector returnPreviousSelector) {
        // never matches
        Cvss3P1 msrcVector = new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.MSRC, null, null, Cvss3P1.class));
        Cvss3P1 nvdVector = new Cvss3P1("CVSS:3.1/AV:L/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, null, null, Cvss3P1.class));
        Cvss3P1 ghsaVector = new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA, Cvss3P1.class));
        Cvss3P1 certSeiVector = new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:H/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.CERT_SEI, null, null, Cvss3P1.class));

        try {
            failSelector.selectVector(Arrays.asList(msrcVector));
            fail("Expected no vector to match the rule and throw an IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected exception
        }
        assertNull(returnNullSelector.selectVector(Arrays.asList(msrcVector)));
        assertNull(returnPreviousSelector.selectVector(Arrays.asList(msrcVector)));

        assertEquals(ghsaVector.toString(), failSelector.selectVector(Arrays.asList(msrcVector, ghsaVector)).toString());
        assertEquals(ghsaVector.toString(), returnNullSelector.selectVector(Arrays.asList(msrcVector, ghsaVector)).toString());
        assertEquals(ghsaVector.toString(), returnPreviousSelector.selectVector(Arrays.asList(msrcVector, ghsaVector)).toString());

        assertEquals(nvdVector.toString(), failSelector.selectVector(Arrays.asList(msrcVector, nvdVector, ghsaVector)).toString());

        assertEquals(ghsaVector.toString(), returnNullSelector.selectVector(Arrays.asList(msrcVector, nvdVector, ghsaVector)).toString());
        assertEquals(ghsaVector.toString(), returnPreviousSelector.selectVector(Arrays.asList(msrcVector, nvdVector, ghsaVector)).toString());

        // will not apply CERT-SEI vector, as the previous one cancels the evaluation
        assertNull(returnNullSelector.selectVector(Arrays.asList(msrcVector, nvdVector, certSeiVector)));
        assertEquals(nvdVector.toString(), returnPreviousSelector.selectVector(Arrays.asList(msrcVector, nvdVector, certSeiVector)).toString());
    }

    @Test
    public void statsEvaluatorTest() {
        CvssSelector cvssSelectorEffectivePresence = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL,
                        new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD)
                ),
                // assessment
                new CvssRule(MergingMethod.ALL,
                        Collections.singletonList(new SelectorStatsCollector("assessment", StatsCollectorProvider.PRESENCE, StatsCollectorSetType.ADD)),
                        Collections.emptyList(),
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_ALL)),
                new CvssRule(MergingMethod.LOWER,
                        Collections.singletonList(new SelectorStatsCollector("assessment", StatsCollectorProvider.PRESENCE, StatsCollectorSetType.ADD)),
                        Collections.emptyList(),
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER)),
                new CvssRule(MergingMethod.HIGHER,
                        Collections.singletonList(new SelectorStatsCollector("assessment", StatsCollectorProvider.PRESENCE, StatsCollectorSetType.ADD)),
                        Collections.emptyList(),
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_HIGHER))
        ), Arrays.asList(
                new SelectorStatsEvaluator("assessment", StatsEvaluatorOperation.EQUAL, EvaluatorAction.RETURN_NULL, 0)
        ), Arrays.asList(
                new SelectorVectorEvaluator(VectorEvaluatorOperation.IS_BASE_PARTIALLY_DEFINED, true, EvaluatorAction.RETURN_NULL)
        ));

        CvssSelector cvssSelectorEffectiveAbsence = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL, Collections.emptyList(),
                        Arrays.asList(
                                new SelectorVectorEvaluator(VectorEvaluatorOperation.IS_BASE_PARTIALLY_DEFINED, true, EvaluatorAction.RETURN_NULL)
                        ),
                        new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD)
                ),
                new CvssRule(MergingMethod.ALL,
                        Collections.singletonList(new SelectorStatsCollector("absence-test", StatsCollectorProvider.ABSENCE, StatsCollectorSetType.ADD)),
                        Collections.emptyList(),
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER)),
                new CvssRule(MergingMethod.ALL,
                        Collections.singletonList(new SelectorStatsCollector("absence-test", StatsCollectorProvider.ABSENCE, StatsCollectorSetType.ADD)),
                        Collections.emptyList(),
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_ALL))
        ), Arrays.asList(
                new SelectorStatsEvaluator("absence-test", StatsEvaluatorOperation.GREATER_OR_EQUAL, EvaluatorAction.RETURN_NULL, 1)
        ), Collections.emptyList());

        Cvss3P1 nvdVector = new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class));
        Cvss3P1 assessmentAllPartsVector = new Cvss3P1("CVSS:3.1/MAV:N", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_ALL, Cvss3P1.class));
        Cvss3P1 assessmentLowerPartsMacVector = new Cvss3P1("CVSS:3.1/MAC:H", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER, Cvss3P1.class));
        Cvss3P1 assessmentLowerActuallyHigherPartsMacVector = new Cvss3P1("CVSS:3.1/MS:C", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER, Cvss3P1.class));

        statsEvaluatorTestAssertions(cvssSelectorEffectivePresence, nvdVector, assessmentAllPartsVector, assessmentLowerPartsMacVector, assessmentLowerActuallyHigherPartsMacVector, cvssSelectorEffectiveAbsence);

        cvssSelectorEffectivePresence = fromJson(cvssSelectorEffectivePresence.toJson());
        cvssSelectorEffectiveAbsence = fromJson(cvssSelectorEffectiveAbsence.toJson());

        statsEvaluatorTestAssertions(cvssSelectorEffectivePresence, nvdVector, assessmentAllPartsVector, assessmentLowerPartsMacVector, assessmentLowerActuallyHigherPartsMacVector, cvssSelectorEffectiveAbsence);
    }

    private static void statsEvaluatorTestAssertions(CvssSelector cvssSelectorEffective, Cvss3P1 nvdVector, Cvss3P1 assessmentAllPartsVector, Cvss3P1 assessmentLowerPartsMacVector, Cvss3P1 assessmentLowerActuallyHigherPartsMacVector, CvssSelector cvssSelectorEffectiveAbsence) {
        // there is no assessment vector, so the later check will return null
        assertNull(cvssSelectorEffective.selectVector(Arrays.asList(nvdVector)));

        // vector base is not defined, so the later check will return null
        assertNull(cvssSelectorEffective.selectVector(Arrays.asList(assessmentAllPartsVector)));

        CvssVector expectedNvdAllPartsVector = nvdVector.clone().applyVectorAndReturn(assessmentAllPartsVector);
        assertEquals(expectedNvdAllPartsVector.toString(), cvssSelectorEffective.selectVector(Arrays.asList(nvdVector, assessmentAllPartsVector)).toString());

        CvssVector expectedNvdLowerPartsVector = nvdVector.clone().applyVectorAndReturn(assessmentLowerPartsMacVector);
        assertEquals(expectedNvdLowerPartsVector.toString(), cvssSelectorEffective.selectVector(Arrays.asList(nvdVector, assessmentLowerPartsMacVector)).toString());

        assertEquals(nvdVector.toString(), cvssSelectorEffective.selectVector(Arrays.asList(nvdVector, assessmentLowerActuallyHigherPartsMacVector)).toString());

        // there is no assessment vector, so the later check will return null
        assertNull(cvssSelectorEffectiveAbsence.selectVector(Arrays.asList(nvdVector)));

        // vector base is not defined, so the early check will return null
        assertNull(cvssSelectorEffectiveAbsence.selectVector(Arrays.asList(assessmentAllPartsVector)));

        assertNull(cvssSelectorEffectiveAbsence.selectVector(Arrays.asList(nvdVector, assessmentLowerPartsMacVector)));
        assertNull(cvssSelectorEffectiveAbsence.selectVector(Arrays.asList(nvdVector, assessmentAllPartsVector)));

        CvssVector expectedNvdLowerAllPartsMacVector = nvdVector.clone().applyVectorAndReturn(assessmentLowerPartsMacVector).applyVectorAndReturn(assessmentAllPartsVector);
        assertEquals(expectedNvdLowerAllPartsMacVector.toString(), cvssSelectorEffectiveAbsence.selectVector(Arrays.asList(nvdVector, assessmentLowerPartsMacVector, assessmentAllPartsVector)).toString());
    }

    @Test
    public void applyLowerChangesOnlyIfPartAppliedTest() {
        final CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL,
                        // NIST NVD
                        new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD)
                ),
                // assessment
                new CvssRule(MergingMethod.LOWER,
                        Collections.singletonList(new SelectorStatsCollector("assessment", StatsCollectorProvider.APPLIED_PARTS_COUNT, StatsCollectorSetType.ADD)),
                        Collections.emptyList(),
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER))
        ), Collections.singletonList(
                new SelectorStatsEvaluator("assessment", StatsEvaluatorOperation.EQUAL, EvaluatorAction.RETURN_NULL, 0)
        ), Collections.singletonList(
                new SelectorVectorEvaluator(VectorEvaluatorOperation.IS_BASE_FULLY_DEFINED, true, EvaluatorAction.RETURN_NULL)
        ));

        final Cvss3P1 nvdVector = new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class));
        final Cvss3P1 assessmentLowerPartsMavVector = new Cvss3P1("CVSS:3.1/MAV:A", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER, Cvss3P1.class));

        // the resulting vector is not lower, so the later check will return null
        assertNull(selector.selectVector(Arrays.asList(nvdVector, assessmentLowerPartsMavVector)));
    }

    @Test
    public void defaultCvssSelectorTest() {
        final Cvss3P1 vector1 = new Cvss3P1("CVSS:3.1/MAV:A/MC:N/MI:N", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_ALL, Cvss3P1.class));
        final Cvss3P1 vector2 = new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:H", new CvssSource(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class));

        Assert.assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:H", CVSS_SELECTOR_INITIAL.selectVector(Arrays.asList(vector1, vector2)).toString());
        Assert.assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:H/MAV:A/MC:N/MI:N", CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(vector1, vector2)).toString());
    }

    @Test
    public void applyMetricLowerHigherSelectorTest() {
        final Cvss4P0 input = new Cvss4P0("CVSS:4.0/AV:L/AC:H/AT:N/PR:N/UI:N/VC:N/VI:L/VA:L/SC:L/SI:L/SA:L", new CvssSource(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss4P0.class));
        final Cvss4P0 modificationLower = new Cvss4P0("CVSS:4.0/MAV:A/MAC:L/MAT:P/MPR:L/MUI:N/MVC:N/MVI:L/MVA:N/MSC:N/MSI:X/MSA:S", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER_METRIC, Cvss4P0.class));
        final Cvss4P0 modificationHigher = new Cvss4P0("CVSS:4.0/MAV:A/MAC:L/MAT:P/MPR:L/MUI:N/MVC:N/MVI:L/MVA:N/MSC:N/MSI:X/MSA:S", new CvssSource(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_HIGHER_METRIC, Cvss4P0.class));

        Assert.assertEquals("CVSS:4.0/AV:L/AC:H/AT:N/PR:N/UI:N/VC:N/VI:L/VA:L/SC:L/SI:L/SA:L/MAT:P/MPR:L/MVA:N/MSC:N", CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(input, modificationLower)).toString());
        Assert.assertEquals("CVSS:4.0/AV:L/AC:H/AT:N/PR:N/UI:N/VC:N/VI:L/VA:L/SC:L/SI:L/SA:L/MAV:A/MAC:L/MUI:N/MVC:N/MVI:L/MSA:S", CVSS_SELECTOR_CONTEXT.selectVector(Arrays.asList(input, modificationHigher)).toString());
    }
}
