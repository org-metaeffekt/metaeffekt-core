package org.metaeffekt.core.maven.inventory.depres;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.assertTrue;

public class ResolutionRunTest {

    private Set<String> readInstalled(File extractionDir) {
        Set<String> installedPackages = new TreeSet<>();
        File packagesNameOnlyFile = new File(extractionDir, "packages_rpm-name-only.txt");

        try (InputStream inputStream = new FileInputStream(packagesNameOnlyFile);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            String readFile = IOUtils.toString(reader);

            if (StringUtils.isBlank(readFile)) {
                System.err.println("Read " + packagesNameOnlyFile.getName() + " is empty. This is likely an error.");
            }

            for (String packageName : readFile.split("\n")) {
                if (!packageName.trim().isEmpty()) {
                    installedPackages.add(packageName);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("file " + packagesNameOnlyFile.getName() + " not in analysis dir.", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return installedPackages;
    }

    @Test
    public void testResolution001() {
        File extractionDir = new File("src/test/resources/testResolution001");

        ResolutionRun depResolver = new ResolutionRun(extractionDir, Collections.singleton("fake-package"));

        RequirementsResult result = depResolver.runResolution();

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
        File extractionDir = new File("/Users/jkranzke/Desktop/almalinux-testrun/analysis");

        Set<String> mustHaves = new HashSet<>(Arrays.asList("coreutils-single", "qemu-kvm", "firefox"));

        ResolutionRun depResolver = new ResolutionRun(extractionDir, mustHaves);

        RequirementsResult result = depResolver.runResolution();

        Set<String> requiredPackages = result.getRequiredPackages();
        System.out.println("required (" + requiredPackages.size() + "):");
        for (String required : requiredPackages) {
            System.out.println("- " + required);
        }

        Collection<String> installedButNotRequired = result.getInstalledButNotRequired();
        System.out.println("installed but not required (" + installedButNotRequired.size() + "):");
        for (String notRequired : installedButNotRequired) {
            System.out.println("- " + notRequired);
        }

        if (!result.getPackageToUnresolvedRequirements().isEmpty()) {
            result.logNotResolvable();
            System.err.println("with mustHaves: " + mustHaves);
            throw new RuntimeException();
        }
    }
}
