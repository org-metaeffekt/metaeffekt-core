package org.metaeffekt.core.maven.inventory;

import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.maven.inventory.depres.ExtractionToRequiredRpm;
import org.metaeffekt.core.maven.inventory.depres.RequirementsResult;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ExtractionToRequiredRpmTest {

    @Test
    public void testResolution001() {
        File extractionDir = new File("src/test/resources/testResolution001");

        ExtractionToRequiredRpm depResolver = new ExtractionToRequiredRpm(extractionDir);

        RequirementsResult result = depResolver.runResolution(Collections.singleton("fake-package"));

        Set<String> required = result.getRequiredPackages();
        assertTrue(required.contains("fake-package"));
        assertTrue(required.contains("coreutils"));
        assertTrue(required.contains("iamafake-lib"));
        assertTrue(required.contains("linux"));

        assertTrue(result.getPackageToUnresolvedRequirements().isEmpty());
    }

    @Test
    @Ignore("Used for manual testing")
    public void testManuallyWithRealExtract() {
        File extractionDir = new File("");
        List<String> mustHaves = Arrays.asList("acl", "openssl-libs", "hdparm", "coreutils", "dnf", "dbus");
        List<String> installedPackages = Arrays.asList("bash", "coreutils", "dummyNotRequired");

        ExtractionToRequiredRpm depResolver = new ExtractionToRequiredRpm(extractionDir);

        RequirementsResult result = depResolver.runResolution(mustHaves);

        System.out.println("required:");
        for (String required : result.getRequiredPackages()) {
            System.out.println("- " + required);
        }

        System.out.println("not required: ");
        for (String notRequired : result.getInstalledButNotRequired(installedPackages)) {
            System.out.println("- " + notRequired);
        }


        result.logNotResolvable();
    }
}
