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
package org.metaeffekt.core.dependency.analysis.depres;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RequirementsResult {
    private static final Logger LOG = LoggerFactory.getLogger(RequirementsResult.class);

    protected static final String statusMarkKey = "Requirement Evaluation";

    protected final Map<String, Set<String>> packageToRequiredPackages;
    protected final Map<String, Set<String>> packageToUnresolvedRequirements;
    protected final Set<String> conditionallyRequired;
    protected final SortedSet<String> installedPackages;

    public RequirementsResult(Map<String, Set<String>> packageToRequiredPackages,
                              Map<String, Set<String>> packageToUnresolvedRequirements,
                              Set<String> conditionallyRequired,
                              Set<String> installedPackages) {
        Objects.requireNonNull(packageToRequiredPackages);
        Objects.requireNonNull(packageToUnresolvedRequirements);
        Objects.requireNonNull(conditionallyRequired);
        Objects.requireNonNull(installedPackages);

        this.packageToRequiredPackages = Collections.unmodifiableMap(new HashMap<>(packageToRequiredPackages));
        this.packageToUnresolvedRequirements =
                Collections.unmodifiableMap(new HashMap<>(packageToUnresolvedRequirements));
        this.conditionallyRequired = Collections.unmodifiableSortedSet(new TreeSet<>(conditionallyRequired));
        this.installedPackages = Collections.unmodifiableSortedSet(new TreeSet<>(installedPackages));
    }

    public Map<String, Set<String>> getPackageToRequiredPackages() {
        return packageToRequiredPackages;
    }

    public Set<String> getConditionallyRequired() {
        return conditionallyRequired;
    }

    public Map<String, Set<String>> getPackageToUnresolvedRequirements() {
        return packageToUnresolvedRequirements;
    }

    public SortedSet<String> getRequiredPackages() {
        SortedSet<String> requiredPackages = new TreeSet<>(packageToRequiredPackages.keySet());
        // add values too, in case there are unresolvable but known package requirements
        packageToRequiredPackages.values().forEach((set) -> {
            for (String requiredPackage : set) {
                if (requiredPackages.add(requiredPackage)) {
                    LOG.warn("Oddity: required package [{}] wasn't a key (not resolved).", requiredPackage);
                }
            }
        });

        return requiredPackages;
    }

    public SortedSet<String> getInstalledButNotRequired() {
        return getInstalledButNotRequired(installedPackages);
    }

    public SortedSet<String> getInstalledButNotRequired(Collection<String> installedPackages) {
        // construct a list of packages that were installed but not confirmed as "required" by resolution
        SortedSet<String> installedButNotRequired = new TreeSet<>(installedPackages);
        installedButNotRequired.removeAll(getRequiredPackages());

        return installedButNotRequired;
    }

    public void logNotResolvable() {
        if (!packageToUnresolvedRequirements.isEmpty()) {
            LOG.warn("Unsatisfied (or unresolvable with current data) requirements in [{}] packages.",
                    packageToUnresolvedRequirements.size());

            LOG.error("Logging [{}] issues:", packageToUnresolvedRequirements.size());

            for (Map.Entry<String, Set<String>> e : packageToUnresolvedRequirements.entrySet()) {
                LOG.error("- " + e.getKey());
                for (String unresolvable : e.getValue()) {
                    LOG.error("  - " + unresolvable);
                }
            }
        } else {
            LOG.info("Map of unsatisfiable dependencies is empty.");
        }
    }

    /**
     * This exports the results to an inventory.<br>
     * It can't properly be read back into RequirementsResult as some data about package relationships is lost.
     * @param outputDirectory where output files may be put.
     * @throws IOException on write error.
     */
    public void exportToInventory(File outputDirectory) throws IOException {
        String outputFileName = "RpmRequirementsResult.xlsx";

        Map<String, StatusMark> packageNameToMark = new TreeMap<>();

        // at first, mark all installed as optional
        for (String installedPackage : installedPackages) {
            packageNameToMark.put(installedPackage, StatusMark.OPTIONAL);
        }

        // mark conditionally required, there may still be duplicates in there whose mark may be overridden
        for (String foundInConditional : conditionallyRequired) {
            packageNameToMark.put(foundInConditional, StatusMark.CONDITIONAL);
        }

        // mark required as required
        for (String requiredPackage : getRequiredPackages()) {
            packageNameToMark.put(requiredPackage, StatusMark.REQUIRED);
        }

        List<Artifact> artifacts = packageNameToMark.entrySet().stream().map(e -> {
            Artifact artifact = new Artifact();
            artifact.setId(e.getKey());
            artifact.set(statusMarkKey, e.getValue().toString());

            return artifact;
        }).collect(Collectors.toCollection(ArrayList::new));

        Inventory inventory = new Inventory();
        inventory.setArtifacts(artifacts);
        InventoryWriter writer = new InventoryWriter();
        File outputFile = new File(outputDirectory, outputFileName);
        writer.writeInventory(inventory, outputFile);
    }
}
