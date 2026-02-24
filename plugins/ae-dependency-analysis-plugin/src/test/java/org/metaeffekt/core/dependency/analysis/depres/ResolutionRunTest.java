/*
 * Copyright 2009-2026 the original author or authors.
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
package org.metaeffekt.core.dependency.analysis.depres;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResolutionRunTest {

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
    @Disabled("Used for manual testing")
    public void testManuallyWithRealExtract() {
        File extractionDir = new File("<path>");

        Set<String> mustHaves = new HashSet<>(Arrays.asList("firefox"));

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

        try {
            result.exportToInventory(extractionDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
