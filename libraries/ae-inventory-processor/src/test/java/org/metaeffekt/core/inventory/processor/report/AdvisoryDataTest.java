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

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AdvisoryDataTest {

    @Test
    public void fromCertFr() throws IOException {
        final Inventory inventory = loadInventory();

        final VulnerabilityMetaData vulnerabilityMetaData = selectVulnerability(inventory, "CVE-2019-17571");

        final List<AdvisoryData> advisoryDataList = AdvisoryData.fromCertFr(
                vulnerabilityMetaData.getComplete("CertFr"));
        final AdvisoryData advisoryData = advisoryDataList.get(0);

        Assert.assertEquals("CERTFR-2020-AVI-350", advisoryData.getId());
        Assert.assertEquals("https://www.cert.ssi.gouv.fr/avis/CERTFR-2020-AVI-350", advisoryData.getUrl());
        Assert.assertEquals("CERT-FR", advisoryData.getSource());
        Assert.assertEquals("Multiples vulnérabilités dans les produits SAP", advisoryData.getOverview());
        Assert.assertEquals("notice", advisoryData.getType());
    }

    @Test
    public void fromMsrc() throws IOException {
        final Inventory inventory = loadInventory();

        final VulnerabilityMetaData vulnerabilityMetaData = selectVulnerability(inventory, "CVE-2021-44228");

        // FIXME: change column name
        final List<AdvisoryData> advisoryDataList = AdvisoryData.fromMsrc(
                vulnerabilityMetaData.getComplete("MS Vulnerability Information"), vulnerabilityMetaData);
        final AdvisoryData advisoryData = advisoryDataList.get(0);

        Assert.assertEquals("MSRC-CVE-2021-44228", advisoryData.getId());
        Assert.assertEquals("https://msrc.microsoft.com/update-guide/en-US/vulnerability/CVE-2021-44228", advisoryData.getUrl());
        Assert.assertEquals("MSRC", advisoryData.getSource());
        Assert.assertEquals("Apache Log4j Remote Code Execution Vulnerability", advisoryData.getOverview());
        Assert.assertEquals("alert", advisoryData.getType());
    }

    @Test
    public void fromCertSei() throws IOException {
        final Inventory inventory = loadInventory();

        final VulnerabilityMetaData vulnerabilityMetaData = selectVulnerability(inventory, "CVE-2021-44228");

        // FIXME: change column name
        final List<AdvisoryData> advisoryDataList = AdvisoryData.fromCertSei(
                vulnerabilityMetaData.getComplete("CertSei"));
        final AdvisoryData advisoryData = advisoryDataList.get(0);

        Assert.assertEquals("VU#930724", advisoryData.getId());
        Assert.assertEquals("https://kb.cert.org/vuls/id/930724", advisoryData.getUrl());
        Assert.assertEquals("Apache Log4j allows insecure JNDI lookups", advisoryData.getOverview());
        Assert.assertEquals("2022-01-07", advisoryData.getUpdateDate());
        Assert.assertEquals("alert", advisoryData.getType());
    }

    private VulnerabilityMetaData selectVulnerability(Inventory inventory, String cve) {
        return inventory.getVulnerabilityMetaData().stream()
                .filter(vmd -> vmd.get(VulnerabilityMetaData.Attribute.NAME).equalsIgnoreCase(cve))
                .findFirst().get();
    }

    private Inventory loadInventory() throws IOException {
        final File inventoryFile = new File("src/test/resources/test-inventory-03/artifact-inventory-03.xls");
        return new InventoryReader().readInventory(inventoryFile);
    }

}