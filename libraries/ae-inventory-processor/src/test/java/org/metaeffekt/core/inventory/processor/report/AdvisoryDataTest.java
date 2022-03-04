package org.metaeffekt.core.inventory.processor.report;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class AdvisoryDataTest {

    @Test
    public void fromCertFr() throws IOException {

        File inventoryFile = new File("src/test/resources/test-inventory-03/artifact-inventory-03.xls");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        final VulnerabilityMetaData vulnerabilityMetaData = inventory.getVulnerabilityMetaData().stream().filter(vmd -> vmd.get(VulnerabilityMetaData.Attribute.NAME).equalsIgnoreCase("CVE-2019-17571")).findFirst().get();

        List<AdvisoryData> advisoryDataList = AdvisoryData.fromCertFr(vulnerabilityMetaData.get("CertFr"));

        AdvisoryData advisoryData = advisoryDataList.get(0);

        Assert.assertEquals("CERTFR-2020-AVI-350", advisoryData.getId());
        Assert.assertEquals("https://www.cert.ssi.gouv.fr/alerte/CERTFR-2020-AVI-350", advisoryData.getUrl());
    }

    @Test
    public void fromMsrc() {
    }

    @Test
    public void fromCertSei() {
    }
}