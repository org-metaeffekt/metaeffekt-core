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
package org.metaeffekt.core.itest.javaartifacts;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.download.UrlPreparer;
import org.metaeffekt.core.itest.inventory.Analysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.GROUPID;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.VERSION;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.IdStartsWith.idStartsWith;

public class PdfboxWarTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox-war/1.8.17/pdfbox-war-1.8.17.war")
                .setName(PdfboxWarTest.class.getName());
    }


    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(preparer.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception {
        Assert.assertTrue(preparer.rebuildInventory());
    }

    @Test
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory = preparer.getInventory();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts(idStartsWith("pdfbox")).hasSizeOf(3);
        analysis.selectArtifacts(idStartsWith("fontbox")).hasSizeOf(1);
        analysis.selectArtifacts(idStartsWith("commons")).hasSizeOf(1);
        analysis.selectArtifacts(idStartsWith("jempbox")).hasSizeOf(1);




        analysis.selectArtifacts(attributeValue(GROUPID, "org.apache.pdfbox")).hasSizeOf(5);
        analysis.selectArtifacts(attributeValue(GROUPID, "commons-logging")).hasSizeOf(1);

        analysis.selectArtifacts(attributeValue(VERSION, "1.8.17")).hasSizeOf(5);
        analysis.selectArtifacts(attributeValue(VERSION, "1.1.1")).hasSizeOf(1);
        }

}
