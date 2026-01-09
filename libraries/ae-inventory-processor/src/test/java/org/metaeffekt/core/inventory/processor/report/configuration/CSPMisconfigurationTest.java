package org.metaeffekt.core.inventory.processor.report.configuration;

import org.junit.Assert;
import org.junit.Test;

public class CSPMisconfigurationTest {

    //TODO: Ensure that the severity ranges misconfiguration are validated sensibly and add more tests for all kinds of misconfiguration of all of the CSP properties

    @Test
    public void misconfigurationTest() {
        CentralSecurityPolicyConfiguration csp = new CentralSecurityPolicyConfiguration();

        // overlaying severity ranges
        csp.setCvssSeverityRanges("None:pastel-gray::0.0,Low:strong-yellow:0.1:8.9,Medium:strong-light-orange:4.0:6.9,High:strong-dark-orange:7.0:8.9,Critical:strong-red:9.0:");
        Assert.assertEquals(1, csp.collectMisconfigurations().size());

        // misspelled color property
        csp.setCvssSeverityRanges("None:pastel-gray::0.0,Low:strong-yeow:0.1:3.9,Medium:strong-light-orange:4.0:6.9,High:strong-dark-orange:7.0:8.9,Critical:strong-red:9.0:");
        Assert.assertEquals(1, csp.collectMisconfigurations().size());


    }
}
