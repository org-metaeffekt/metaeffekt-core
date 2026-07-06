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
package org.metaeffekt.core.maven.inventory.mojo;

import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class AggregateSourceArchivesMojoTest {

    @Test
    public void testImplicitRules() {
        AggregateSourceArchivesMojo mojo = new AggregateSourceArchivesMojo();
        
        SourceAggregationConfig.ImplicitConfig includeConfig = new SourceAggregationConfig.ImplicitConfig();
        includeConfig.setLicenses(Arrays.asList("Apache-2.0", "MIT"));
        includeConfig.setPatterns(Arrays.asList("/^org\\.include\\..*/", "exact.match"));

        SourceAggregationConfig.ImplicitConfig excludeConfig = new SourceAggregationConfig.ImplicitConfig();
        excludeConfig.setLicenses(Arrays.asList("GPL-2.0", "GPL-3.0"));
        excludeConfig.setPatterns(Arrays.asList("/^org\\.exclude\\..*/"));

        Inventory inventory = new Inventory();

        // 1. Match license exactly
        Artifact artifact1 = new Artifact();
        artifact1.setLicense("Apache-2.0");
        assertTrue(mojo.matchesImplicitRules(artifact1, inventory, includeConfig));

        // 2. Match multiple licenses (comma separated)
        Artifact artifact2 = new Artifact();
        artifact2.setLicense("Unknown, MIT, Another");
        assertTrue(mojo.matchesImplicitRules(artifact2, inventory, includeConfig));

        // 3. Match regex pattern
        Artifact artifactRegex = new Artifact();
        artifactRegex.setGroupId("org.include.module");
        assertTrue(mojo.matchesImplicitRules(artifactRegex, inventory, includeConfig));

        // 4. Match literal pattern
        Artifact artifactLiteral = new Artifact();
        artifactLiteral.setGroupId("exact.match");
        assertTrue(mojo.matchesImplicitRules(artifactLiteral, inventory, includeConfig));

        // 5. No match
        Artifact noMatch = new Artifact();
        noMatch.setLicense("BSD-3-Clause");
        noMatch.setGroupId("com.example.module");
        assertFalse(mojo.matchesImplicitRules(noMatch, inventory, includeConfig));
        assertFalse(mojo.matchesImplicitRules(noMatch, inventory, excludeConfig));

        // 6. Exclude match license
        Artifact excludeMatch = new Artifact();
        excludeMatch.setLicense("GPL-3.0, SomeOther");
        assertTrue(mojo.matchesImplicitRules(excludeMatch, inventory, excludeConfig));
        
        // 7. Test missing license logic
        assertTrue(mojo.hasLicense(artifact1, inventory));
        assertFalse(mojo.hasLicense(new Artifact(), inventory));
    }

    @Test
    public void testExplicitModes() throws org.apache.maven.plugin.MojoExecutionException {
        AggregateSourceArchivesMojo mojo = new AggregateSourceArchivesMojo();
        mojo.setLog(new org.apache.maven.plugin.logging.SystemStreamLog());

        SourceAggregationConfig config = new SourceAggregationConfig();
        // Set config so everything is implicitly excluded by default
        config.setDefaultNoLicenseExclusion(true);

        Inventory inventory = new Inventory();

        Artifact excludeArtifact = new Artifact();
        excludeArtifact.set(Artifact.Attribute.SOURCE_AGGREGATION_MODE, "exclude");
        excludeArtifact.setLicense("Apache-2.0"); // Even with valid license, should be skipped

        Artifact includeArtifact = new Artifact();
        includeArtifact.set(Artifact.Attribute.SOURCE_AGGREGATION_MODE, "include");
        // No license, so normally excluded, but include overrides it
        
        Artifact acceptMissingArtifact = new Artifact();
        acceptMissingArtifact.set(Artifact.Attribute.SOURCE_AGGREGATION_MODE, "accept missing");

        assertFalse(mojo.shouldIncludeArtifact(excludeArtifact, inventory, config), "Exclude mode should skip artifact");
        assertTrue(mojo.shouldIncludeArtifact(includeArtifact, inventory, config), "Include mode should process artifact despite implicit excludes");
        assertTrue(mojo.shouldIncludeArtifact(acceptMissingArtifact, inventory, config), "Accept missing mode should process artifact");
    }

    @Test
    public void testAmbiguousImplicitRules() {
        AggregateSourceArchivesMojo mojo = new AggregateSourceArchivesMojo();
        mojo.setLog(new org.apache.maven.plugin.logging.SystemStreamLog());

        SourceAggregationConfig config = new SourceAggregationConfig();
        
        SourceAggregationConfig.ImplicitConfig includeConfig = new SourceAggregationConfig.ImplicitConfig();
        includeConfig.setLicenses(Arrays.asList("Apache-2.0"));
        config.setInclude(includeConfig);

        SourceAggregationConfig.ImplicitConfig excludeConfig = new SourceAggregationConfig.ImplicitConfig();
        excludeConfig.setPatterns(Arrays.asList("org.ambiguous.module"));
        config.setExclude(excludeConfig);

        Inventory inventory = new Inventory();

        Artifact ambiguousArtifact = new Artifact();
        ambiguousArtifact.setLicense("Apache-2.0");
        ambiguousArtifact.setGroupId("org.ambiguous.module");

        // The artifact matches both include and exclude rules, which should trigger a MojoExecutionException
        org.apache.maven.plugin.MojoExecutionException exception = assertThrows(
                org.apache.maven.plugin.MojoExecutionException.class,
                () -> mojo.shouldIncludeArtifact(ambiguousArtifact, inventory, config)
        );

        assertTrue(exception.getMessage().contains("Ambiguous implicit inclusion/exclusion for artifact"), 
                   "Exception message should mention ambiguity");
    }
}
