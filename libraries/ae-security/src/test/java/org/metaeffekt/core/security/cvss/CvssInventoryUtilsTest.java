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

public class CvssInventoryUtilsTest {

    /*@Test
    public void applyNewCvssInformationOnVulnerabilityMetaDataTest() {
        {
            final VulnerabilityMetaData vmd = new VulnerabilityMetaData();
            CvssInventoryUtils.applyNewCvssInformationOnVulnerabilityMetaData(vmd, new Cvss2("AV:N/AC:L/Au:N/C:P/I:P/A:P"), null, null, CvssSeverityRanges.CVSS_3_SEVERITY_RANGES, false);
            CvssInventoryUtils.applyNewCvssInformationOnVulnerabilityMetaData(vmd, new Cvss2("E:U/RL:TF/RC:ND"), new Cvss3("CVSS:3.1/MAV:N/MAC:H/MPR:H"), null, CvssSeverityRanges.CVSS_3_SEVERITY_RANGES, true);

            for (String attribute : vmd.getAttributes()) {
                Assert.assertFalse(attribute.contains("(v3)"));
            }
        }

        {
            final VulnerabilityMetaData vmd = new VulnerabilityMetaData();
            CvssInventoryUtils.applyNewCvssInformationOnVulnerabilityMetaData(vmd, new Cvss2("AV:N/AC:L/Au:N/C:P/I:P/A:P"), new Cvss3("AV:N/AC:H/PR:L/UI:N/S:C/C:L/I:N/A:L"), null, CvssSeverityRanges.CVSS_3_SEVERITY_RANGES, false);
            CvssInventoryUtils.applyNewCvssInformationOnVulnerabilityMetaData(vmd, new Cvss2("E:U/RL:TF/RC:ND"), new Cvss3("CVSS:3.1/MAV:N/MAC:H/MPR:H"), null, CvssSeverityRanges.CVSS_3_SEVERITY_RANGES, true);

            Assert.assertEquals("CVSS:3.1/AV:N/AC:H/PR:L/UI:N/S:C/C:L/I:N/A:L/MAV:N/MAC:H/MPR:H", vmd.get(InventoryAttribute.CVSS_MODIFIED_VECTOR_V3.getKey()));
            Assert.assertEquals("4.9", vmd.get(InventoryAttribute.CVSS_MODIFIED_BASE_V3.getKey()));
        }
    }*/

}