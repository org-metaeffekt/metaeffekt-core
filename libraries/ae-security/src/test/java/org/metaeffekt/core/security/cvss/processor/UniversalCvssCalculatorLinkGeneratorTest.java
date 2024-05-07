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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.security.cvss.v2.Cvss2;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;
import org.metaeffekt.core.security.cvss.v4P0.Cvss4P0;

public class UniversalCvssCalculatorLinkGeneratorTest {

    @Test
    public void generateBasicLinkTest() {
        final UniversalCvssCalculatorLinkGenerator generator = new UniversalCvssCalculatorLinkGenerator();
        generator.setBaseUrl("https://metaeffekt.com/security/cvss/calculator/index.html");

        generator.addVector(new Cvss2("AV:L/AC:H/Au:S/C:C/I:P/A:N/E:U/RL:U/RC:C/CDP:LM/TD:M/CR:H/IR:H/AR:H")).setName("TEST-CVSS-001").setVisible(false);
        generator.addVector(new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:C/C:H/I:L/A:H/E:F/RL:U/RC:R")).setName("TEST-CVSS-002").setVisible(true);
        generator.addVector(new Cvss4P0("CVSS:4.0/AV:P/AC:L/AT:N/PR:N/UI:N/VC:H/VI:L/VA:L/SC:H/SI:H/SA:H")).setName("TEST-CVSS-003").setVisible(true).setInitialCvssVector(new Cvss4P0("CVSS:4.0/AV:P/AC:H/AT:N/PR:N/UI:N/VC:H/VI:L/VA:L/SC:H/SI:H/SA:H"));

        generator.addCve("CVE-2020-1234");
        generator.addCve("CVE-2020-5678");

        generator.setSelectedVector("TEST-CVSS-002");

        Assert.assertEquals(
                "https://metaeffekt.com/security/cvss/calculator/index.html?vector=%5B%5B%22TEST-CVSS-001%22%2Cfalse%2C%22AV%3AL%2FAC%3AH%2FAu%3AS%2FC%3AC%2FI%3AP%2FA%3AN%2FE%3AU%2FRL%3AU%2FRC%3AC%2FCDP%3ALM%2FTD%3AM%2FCR%3AH%2FIR%3AH%2FAR%3AH%22%2C%22CVSS%3A2.0%22%2Cnull%5D%2C%5B%22TEST-CVSS-002%22%2Ctrue%2C%22CVSS%3A3.1%2FAV%3AN%2FAC%3AL%2FPR%3AL%2FUI%3AN%2FS%3AC%2FC%3AH%2FI%3AL%2FA%3AH%2FE%3AF%2FRL%3AU%2FRC%3AR%22%2C%22CVSS%3A3.1%22%2Cnull%5D%2C%5B%22TEST-CVSS-003%22%2Ctrue%2C%22CVSS%3A4.0%2FAV%3AP%2FAC%3AL%2FAT%3AN%2FPR%3AN%2FUI%3AN%2FVC%3AH%2FVI%3AL%2FVA%3AL%2FSC%3AH%2FSI%3AH%2FSA%3AH%22%2C%22CVSS%3A4.0%22%2C%22CVSS%3A4.0%2FAV%3AP%2FAC%3AH%2FAT%3AN%2FPR%3AN%2FUI%3AN%2FVC%3AH%2FVI%3AL%2FVA%3AL%2FSC%3AH%2FSI%3AH%2FSA%3AH%22%5D%5D&selected=TEST-CVSS-002&cve=CVE-2020-1234%2CCVE-2020-5678",
                generator.generateLink()
        );
    }

    @Test
    @Ignore("Test disabled: GZIP compression differs between Java versions")
    public void compressedLinkTest() {
        final UniversalCvssCalculatorLinkGenerator generator = new UniversalCvssCalculatorLinkGenerator();
        generator.setBaseUrl("https://metaeffekt.com/security/cvss/calculator/index.html");

        generator.addVector(new Cvss2("AV:L/AC:H/Au:S/C:C/I:P/A:N/E:U/RL:U/RC:C/CDP:LM/TD:M/CR:H/IR:H/AR:H")).setName("TEST-CVSS-001").setVisible(false);
        generator.addVector(new Cvss3P1("CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:C/C:H/I:L/A:H/E:F/RL:U/RC:R")).setName("TEST-CVSS-002").setVisible(true);
        generator.addVector(new Cvss4P0("CVSS:4.0/AV:P/AC:L/AT:N/PR:N/UI:N/VC:H/VI:L/VA:L/SC:H/SI:H/SA:H")).setName("TEST-CVSS-003").setVisible(true).setInitialCvssVector(new Cvss4P0("CVSS:4.0/AV:P/AC:H/AT:N/PR:N/UI:N/VC:H/VI:L/VA:L/SC:H/SI:H/SA:H"));

        generator.addCve("CVE-2020-1234");
        generator.addCve("CVE-2020-5678");

        generator.setSelectedVector("TEST-CVSS-002");

        Assert.assertEquals(
                "https://metaeffekt.com/security/cvss/calculator/index.html?b64gzip=H4sIAAAAAAAAAJWRwYrCMBCG3yUHT2knTbvuMtBDiBULtZSm5iI9iMZTUVDr8zuDi1Lcy16-MGHyf5nkHva38yXfbkVXuC6y3rlIqUTI4264BimMxwqMxRWYER1YtFBiAwZrKHADbcXgXbtosFpDt8A12Jb6S4YhCCk4FnWshDyNw9DLqU0LebuM4bctjRMga83WCpqWsCmpdCzhXL4QrQUuX_pWvA__7UgnjixW7GieDtNRPInqp8izxbPGG4Lj0pUM8x4m42E-w1b_C-v72TUM9AXhkE9eZLa_h9z6ItJKqyjRaSZf1df8--cB_HtTibcBAAA",
                generator.generateBas64EncodedGzipCompressedLink()
        );
    }
}