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
package org.metaeffekt.core.maven.inventory.depres;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RequirementsResult {
    private static final Logger LOG = LoggerFactory.getLogger(RequirementsResult.class);

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
        packageToRequiredPackages.values().forEach(requiredPackages::addAll);

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
}
