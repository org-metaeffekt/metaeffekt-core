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
package org.metaeffekt.core.security.cvss;

import org.junit.Test;
import org.metaeffekt.core.security.cvss.CvssSource.CvssIssuingEntityRole;
import org.metaeffekt.core.security.cvss.processor.CvssSelector;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.metaeffekt.core.security.cvss.processor.CvssSelector.*;

public class CvssSelectorTest {

    @Test
    public void overwritePreviousVectorTest() {
        CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL, new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY)),
                new CvssRule(MergingMethod.OVERWRITE, new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY))
        ));

        SourcedCvssVector<Cvss3P1> nvdVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, null, null, Cvss3P1.class),
                new Cvss3P1()
        );
        SourcedCvssVector<Cvss3P1> msrcVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.MSRC, null, null, Cvss3P1.class),
                new Cvss3P1()
        );

        Cvss3P1 effective = selector.selectVector(Arrays.asList(nvdVector, msrcVector)).getCvssVector();
        assertEquals(msrcVector.getCvssVector(), effective);
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

        SourcedCvssVector<Cvss3P1> baseMsrcVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.MSRC, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );
        SourcedCvssVector<Cvss3P1> baseNvdNvdVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );
        SourcedCvssVector<Cvss3P1> baseNvdGhsaVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> assessmentAllPartsVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_ALL, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/MAV:N")
        );
        SourcedCvssVector<Cvss3P1> assessmentLowerPartsMacVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/MAC:H")
        );
        SourcedCvssVector<Cvss3P1> assessmentHigherPartsMacVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_HIGHER, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/MAC:L")
        );
        SourcedCvssVector<Cvss3P1> assessmentHigherPartsMavVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_HIGHER, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/MAV:L")
        );

        // picks baseNvdNvdVector
        assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", reParsedSelector.selectVector(Arrays.asList(baseMsrcVector, baseNvdNvdVector, baseNvdGhsaVector)).getCvssVector().toString());

        // picks baseNvdGhsaVector + assessmentAllPartsVector
        assertEquals("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MAV:N", reParsedSelector.selectVector(Arrays.asList(baseMsrcVector, baseNvdGhsaVector, assessmentAllPartsVector)).getCvssVector().toString());

        // picks baseNvdGhsaVector + assessmentLowerPartsMacVector
        assertEquals("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MAC:H", reParsedSelector.selectVector(Arrays.asList(baseMsrcVector, baseNvdGhsaVector, assessmentLowerPartsMacVector)).getCvssVector().toString());

        // picks baseNvdGhsaVector + assessmentHigherPartsMacVector
        assertEquals("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MAC:L", reParsedSelector.selectVector(Arrays.asList(baseMsrcVector, baseNvdGhsaVector, assessmentHigherPartsMacVector)).getCvssVector().toString());

        // picks baseNvdNvdVector + assessmentHigherPartsMavVector, but assessmentHigherPartsMavVector is not applied because MAV:L is lower than MAV:N
        assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", reParsedSelector.selectVector(Arrays.asList(baseMsrcVector, baseNvdNvdVector, assessmentHigherPartsMavVector)).getCvssVector().toString());
    }

    @Test
    public void selectorEmptyEntityTest() {
        CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL,
                        new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY),
                        new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY),
                        new SourceSelectorEntry(SourceSelectorEntry.ANY_ENTITY, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY))
        ));

        SourcedCvssVector<Cvss3P1> nvdEmptyVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );
        SourcedCvssVector<Cvss3P1> nvdNvdVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> msrcEmptyVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.MSRC, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        // picks nvdEmptyVector
        assertEquals(nvdEmptyVector.getCvssVector().toString(), selector.selectVector(Arrays.asList(nvdEmptyVector, nvdNvdVector, msrcEmptyVector)).getCvssVector().toString());

        // picks msrcEmptyVector
        assertEquals(msrcEmptyVector.getCvssVector().toString(), selector.selectVector(Arrays.asList(nvdNvdVector, msrcEmptyVector)).getCvssVector().toString());

        // picks nvdNvdVector
        assertEquals(nvdNvdVector.getCvssVector().toString(), selector.selectVector(Collections.singletonList(nvdNvdVector)).getCvssVector().toString());
    }

    @Test
    public void invertedSelectorTest() {
        CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(MergingMethod.ALL,
                        new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY),
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY, true, false, false))
        ));

        SourcedCvssVector<Cvss3P1> nvdEmptyVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> assessmentAllPartsVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_ALL, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/MAV:N")
        );

        // picks nvdEmptyVector
        assertEquals(nvdEmptyVector.getCvssVector().toString(), selector.selectVector(Arrays.asList(nvdEmptyVector, assessmentAllPartsVector)).getCvssVector().toString());

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

        SourcedCvssVector<Cvss3P1> nvdVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );
        SourcedCvssVector<Cvss3P1> fullNvdVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> emptyVector = new SourcedCvssVector<>(
                new CvssSource<>(null, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        // picks fullNvdVector
        assertEquals(fullNvdVector.getCvssVector().toString(), baseSelector.selectVector(Arrays.asList(nvdVector, emptyVector, fullNvdVector)).getCvssVector().toString());

        // does not find any
        assertNull(baseSelector.selectVector(Arrays.asList(nvdVector, emptyVector)));

        final CvssSelector reParsedSelector = fromJson(baseSelector.toJson());

        // picks fullNvdVector
        assertEquals(fullNvdVector.getCvssVector().toString(), reParsedSelector.selectVector(Arrays.asList(nvdVector, emptyVector, fullNvdVector)).getCvssVector().toString());

        // does not find any
        assertNull(reParsedSelector.selectVector(Arrays.asList(nvdVector, emptyVector)));

        // picks nvdVector, as it is first no matter what
        assertEquals(nvdVector.getCvssVector().toString(), anySelector.selectVector(Arrays.asList(nvdVector, emptyVector, fullNvdVector)).getCvssVector().toString());
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

        SourcedCvssVector<Cvss3P1> nvdVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );
        SourcedCvssVector<Cvss3P1> fullNvdVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> ghsaVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.GHSA, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:A/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> emptyVector = new SourcedCvssVector<>(
                new CvssSource<>(null, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> assessmentVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.ASSESSMENT, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:L/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        // picks first that is not assessmentVector or ghsaVector
        assertEquals(nvdVector.getCvssVector().toString(), selector.selectVector(Arrays.asList(assessmentVector, nvdVector, emptyVector, fullNvdVector)).getCvssVector().toString());
        assertEquals(emptyVector.getCvssVector().toString(), selector.selectVector(Arrays.asList(assessmentVector, emptyVector, fullNvdVector)).getCvssVector().toString());
        assertEquals(fullNvdVector.getCvssVector().toString(), selector.selectVector(Arrays.asList(assessmentVector, fullNvdVector)).getCvssVector().toString());
        assertEquals(nvdVector.getCvssVector().toString(), selector.selectVector(Arrays.asList(ghsaVector, nvdVector, emptyVector, fullNvdVector)).getCvssVector().toString());
        assertEquals(emptyVector.getCvssVector().toString(), selector.selectVector(Arrays.asList(ghsaVector, emptyVector, fullNvdVector)).getCvssVector().toString());
        assertEquals(fullNvdVector.getCvssVector().toString(), selector.selectVector(Arrays.asList(ghsaVector, fullNvdVector)).getCvssVector().toString());

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
        SourcedCvssVector<Cvss3P1> msrcVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.MSRC, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> nvdVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:L/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> ghsaVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.GHSA, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> certSeiVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.CERT_SEI, null, null, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:H/UI:N/S:U/C:H/I:H/A:H")
        );

        try {
            failSelector.selectVector(Arrays.asList(msrcVector));
            fail("Expected no vector to match the rule and throw an IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected exception
        }
        assertNull(returnNullSelector.selectVector(Arrays.asList(msrcVector)));
        assertNull(returnPreviousSelector.selectVector(Arrays.asList(msrcVector)));

        assertEquals(ghsaVector.getCvssVector().toString(), failSelector.selectVector(Arrays.asList(msrcVector, ghsaVector)).getCvssVector().toString());
        assertEquals(ghsaVector.getCvssVector().toString(), returnNullSelector.selectVector(Arrays.asList(msrcVector, ghsaVector)).getCvssVector().toString());
        assertEquals(ghsaVector.getCvssVector().toString(), returnPreviousSelector.selectVector(Arrays.asList(msrcVector, ghsaVector)).getCvssVector().toString());

        assertEquals(nvdVector.getCvssVector().toString(), failSelector.selectVector(Arrays.asList(msrcVector, nvdVector, ghsaVector)).getCvssVector().toString());

        assertEquals(ghsaVector.getCvssVector().toString(), returnNullSelector.selectVector(Arrays.asList(msrcVector, nvdVector, ghsaVector)).getCvssVector().toString());
        assertEquals(ghsaVector.getCvssVector().toString(), returnPreviousSelector.selectVector(Arrays.asList(msrcVector, nvdVector, ghsaVector)).getCvssVector().toString());

        // will not apply CERT-SEI vector, as the previous one cancels the evaluation
        assertNull(returnNullSelector.selectVector(Arrays.asList(msrcVector, nvdVector, certSeiVector)));
        assertEquals(nvdVector.getCvssVector().toString(), returnPreviousSelector.selectVector(Arrays.asList(msrcVector, nvdVector, certSeiVector)).getCvssVector().toString());
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

        SourcedCvssVector<Cvss3P1> nvdVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
        );

        SourcedCvssVector<Cvss3P1> assessmentAllPartsVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_ALL, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/MAV:N")
        );
        SourcedCvssVector<Cvss3P1> assessmentLowerPartsMacVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/MAC:H")
        );
        SourcedCvssVector<Cvss3P1> assessmentLowerActuallyHigherPartsMacVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.ASSESSMENT, KnownCvssEntities.ASSESSMENT_LOWER, Cvss3P1.class),
                new Cvss3P1("CVSS:3.1/MS:C")
        );

        statsEvaluatorTestAssertions(cvssSelectorEffectivePresence, nvdVector, assessmentAllPartsVector, assessmentLowerPartsMacVector, assessmentLowerActuallyHigherPartsMacVector, cvssSelectorEffectiveAbsence);

        cvssSelectorEffectivePresence = fromJson(cvssSelectorEffectivePresence.toJson());
        cvssSelectorEffectiveAbsence = fromJson(cvssSelectorEffectiveAbsence.toJson());

        statsEvaluatorTestAssertions(cvssSelectorEffectivePresence, nvdVector, assessmentAllPartsVector, assessmentLowerPartsMacVector, assessmentLowerActuallyHigherPartsMacVector, cvssSelectorEffectiveAbsence);
    }

    private static void statsEvaluatorTestAssertions(CvssSelector cvssSelectorEffective, SourcedCvssVector<Cvss3P1> nvdVector, SourcedCvssVector<Cvss3P1> assessmentAllPartsVector, SourcedCvssVector<Cvss3P1> assessmentLowerPartsMacVector, SourcedCvssVector<Cvss3P1> assessmentLowerActuallyHigherPartsMacVector, CvssSelector cvssSelectorEffectiveAbsence) {
        // there is no assessment vector, so the later check will return null
        assertNull(cvssSelectorEffective.selectVector(Arrays.asList(nvdVector)));

        // vector base is not defined, so the later check will return null
        assertNull(cvssSelectorEffective.selectVector(Arrays.asList(assessmentAllPartsVector)));

        CvssVector expectedNvdAllPartsVector = nvdVector.getCvssVector().clone().applyVectorChain(assessmentAllPartsVector.getCvssVector());
        assertEquals(expectedNvdAllPartsVector.toString(), cvssSelectorEffective.selectVector(Arrays.asList(nvdVector, assessmentAllPartsVector)).getCvssVector().toString());

        CvssVector expectedNvdLowerPartsVector = nvdVector.getCvssVector().clone().applyVectorChain(assessmentLowerPartsMacVector.getCvssVector());
        assertEquals(expectedNvdLowerPartsVector.toString(), cvssSelectorEffective.selectVector(Arrays.asList(nvdVector, assessmentLowerPartsMacVector)).getCvssVector().toString());

        assertEquals(nvdVector.getCvssVector().toString(), cvssSelectorEffective.selectVector(Arrays.asList(nvdVector, assessmentLowerActuallyHigherPartsMacVector)).getCvssVector().toString());

        // there is no assessment vector, so the later check will return null
        assertNull(cvssSelectorEffectiveAbsence.selectVector(Arrays.asList(nvdVector)));

        // vector base is not defined, so the early check will return null
        assertNull(cvssSelectorEffectiveAbsence.selectVector(Arrays.asList(assessmentAllPartsVector)));

        assertNull(cvssSelectorEffectiveAbsence.selectVector(Arrays.asList(nvdVector, assessmentLowerPartsMacVector)));
        assertNull(cvssSelectorEffectiveAbsence.selectVector(Arrays.asList(nvdVector, assessmentAllPartsVector)));

        CvssVector expectedNvdLowerAllPartsMacVector = nvdVector.getCvssVector().clone().applyVectorChain(assessmentLowerPartsMacVector.getCvssVector()).applyVectorChain(assessmentAllPartsVector.getCvssVector());
        assertEquals(expectedNvdLowerAllPartsMacVector.toString(), cvssSelectorEffectiveAbsence.selectVector(Arrays.asList(nvdVector, assessmentLowerPartsMacVector, assessmentAllPartsVector)).getCvssVector().toString());
    }
}
