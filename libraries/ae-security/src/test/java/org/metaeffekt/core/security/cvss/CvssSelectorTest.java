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
import org.metaeffekt.core.security.cvss.CvssSelector.CvssRule;
import org.metaeffekt.core.security.cvss.CvssSelector.SourceSelectorEntry;
import org.metaeffekt.core.security.cvss.CvssSelector.SourceSelectorEntryEntry;
import org.metaeffekt.core.security.cvss.CvssSource.CvssIssuingEntityRole;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CvssSelectorTest {

    @Test
    public void overwritePreviousVectorTest() {
        CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(CvssSelector.MergingMethod.ALL, new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY)),
                new CvssRule(CvssSelector.MergingMethod.OVERWRITE, new SourceSelectorEntry(KnownCvssEntities.MSRC, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY))
        ));

        SourcedCvssVector<Cvss3P1> nvdVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.NVD, null, null, Cvss3P1.class),
                new Cvss3P1()
        );
        SourcedCvssVector<Cvss3P1> msrcVector = new SourcedCvssVector<>(
                new CvssSource<>(KnownCvssEntities.MSRC, null, null, Cvss3P1.class),
                new Cvss3P1()
        );

        Cvss3P1 effective = selector.calculateEffective(Arrays.asList(nvdVector, msrcVector));
        assertEquals(msrcVector.getCvssVector(), effective);
    }

    @Test
    public void combineVectorsWithToJsonTest() {
        final CvssSelector baseSelector = new CvssSelector(Arrays.asList(
                new CvssRule(CvssSelector.MergingMethod.ALL,
                        new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY),
                        new SourceSelectorEntry(KnownCvssEntities.NVD, CvssIssuingEntityRole.CNA, KnownCvssEntities.NVD),
                        new SourceSelectorEntry(KnownCvssEntities.NVD, SourceSelectorEntry.ANY_ROLE, SourceSelectorEntry.ANY_ENTITY)
                ),
                new CvssRule(CvssSelector.MergingMethod.ALL,
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_ALL)
                ),
                new CvssRule(CvssSelector.MergingMethod.LOWER,
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_LOWER)
                ),
                new CvssRule(CvssSelector.MergingMethod.HIGHER,
                        new SourceSelectorEntry(KnownCvssEntities.ASSESSMENT, SourceSelectorEntry.ANY_ROLE, KnownCvssEntities.ASSESSMENT_HIGHER)
                )
        ));

        final CvssSelector reParsedSelector = CvssSelector.fromJson(baseSelector.toJson());

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
        assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", reParsedSelector.calculateEffective(Arrays.asList(baseMsrcVector, baseNvdNvdVector, baseNvdGhsaVector)).toString());

        // picks baseNvdGhsaVector + assessmentAllPartsVector
        assertEquals("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MAV:N", reParsedSelector.calculateEffective(Arrays.asList(baseMsrcVector, baseNvdGhsaVector, assessmentAllPartsVector)).toString());

        // picks baseNvdGhsaVector + assessmentLowerPartsMacVector
        assertEquals("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MAC:H", reParsedSelector.calculateEffective(Arrays.asList(baseMsrcVector, baseNvdGhsaVector, assessmentLowerPartsMacVector)).toString());

        // picks baseNvdGhsaVector + assessmentHigherPartsMacVector
        assertEquals("CVSS:3.1/AV:P/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/MAC:L", reParsedSelector.calculateEffective(Arrays.asList(baseMsrcVector, baseNvdGhsaVector, assessmentHigherPartsMacVector)).toString());

        // picks baseNvdNvdVector + assessmentHigherPartsMavVector, but assessmentHigherPartsMavVector is not applied because MAV:L is lower than MAV:N
        assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", reParsedSelector.calculateEffective(Arrays.asList(baseMsrcVector, baseNvdNvdVector, assessmentHigherPartsMavVector)).toString());
    }

    @Test
    public void selectorEmptyEntityTest() {
        CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(CvssSelector.MergingMethod.ALL,
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
        assertEquals(nvdEmptyVector.getCvssVector().toString(), selector.calculateEffective(Arrays.asList(nvdEmptyVector, nvdNvdVector, msrcEmptyVector)).toString());

        // picks msrcEmptyVector
        assertEquals(msrcEmptyVector.getCvssVector().toString(), selector.calculateEffective(Arrays.asList(nvdNvdVector, msrcEmptyVector)).toString());

        // picks nvdNvdVector
        assertEquals(nvdNvdVector.getCvssVector().toString(), selector.calculateEffective(Collections.singletonList(nvdNvdVector)).toString());
    }

    @Test
    public void invertedSelectorTest() {
        CvssSelector selector = new CvssSelector(Arrays.asList(
                new CvssRule(CvssSelector.MergingMethod.ALL,
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
        assertEquals(nvdEmptyVector.getCvssVector().toString(), selector.calculateEffective(Arrays.asList(nvdEmptyVector, assessmentAllPartsVector)).toString());

        // picks none
        assertNull(selector.calculateEffective(Collections.singletonList(assessmentAllPartsVector)));
    }

    @Test
    public void emptyInvertedSelectorTest() {
        CvssSelector baseSelector = new CvssSelector(Arrays.asList(
                new CvssRule(CvssSelector.MergingMethod.ALL,
                        new SourceSelectorEntry(SourceSelectorEntry.EMPTY_ENTITY, SourceSelectorEntry.EMPTY_ROLE, SourceSelectorEntry.EMPTY_ENTITY, true, true, true))
        ));
        CvssSelector anySelector = new CvssSelector(Arrays.asList(
                new CvssRule(CvssSelector.MergingMethod.ALL,
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
        assertEquals(fullNvdVector.getCvssVector().toString(), baseSelector.calculateEffective(Arrays.asList(nvdVector, emptyVector, fullNvdVector)).toString());

        // does not find any
        assertNull(baseSelector.calculateEffective(Arrays.asList(nvdVector, emptyVector)));

        final CvssSelector reParsedSelector = CvssSelector.fromJson(baseSelector.toJson());

        // picks fullNvdVector
        assertEquals(fullNvdVector.getCvssVector().toString(), reParsedSelector.calculateEffective(Arrays.asList(nvdVector, emptyVector, fullNvdVector)).toString());

        // does not find any
        assertNull(reParsedSelector.calculateEffective(Arrays.asList(nvdVector, emptyVector)));

        // picks nvdVector, as it is first no matter what
        assertEquals(nvdVector.getCvssVector().toString(), anySelector.calculateEffective(Arrays.asList(nvdVector, emptyVector, fullNvdVector)).toString());
    }

    @Test
    public void multipleInvertedSelectorPartsTest() {
        CvssSelector baseSelector = new CvssSelector(Arrays.asList(
                new CvssRule(CvssSelector.MergingMethod.ALL,
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
        assertEquals(nvdVector.getCvssVector().toString(), baseSelector.calculateEffective(Arrays.asList(assessmentVector, nvdVector, emptyVector, fullNvdVector)).toString());
        assertEquals(emptyVector.getCvssVector().toString(), baseSelector.calculateEffective(Arrays.asList(assessmentVector, emptyVector, fullNvdVector)).toString());
        assertEquals(fullNvdVector.getCvssVector().toString(), baseSelector.calculateEffective(Arrays.asList(assessmentVector, fullNvdVector)).toString());
        assertEquals(nvdVector.getCvssVector().toString(), baseSelector.calculateEffective(Arrays.asList(ghsaVector, nvdVector, emptyVector, fullNvdVector)).toString());
        assertEquals(emptyVector.getCvssVector().toString(), baseSelector.calculateEffective(Arrays.asList(ghsaVector, emptyVector, fullNvdVector)).toString());
        assertEquals(fullNvdVector.getCvssVector().toString(), baseSelector.calculateEffective(Arrays.asList(ghsaVector, fullNvdVector)).toString());

        // does not find any
        assertNull(baseSelector.calculateEffective(Arrays.asList(assessmentVector)));
        assertNull(baseSelector.calculateEffective(Arrays.asList(ghsaVector)));
        assertNull(baseSelector.calculateEffective(Arrays.asList(assessmentVector, ghsaVector)));
    }
}
