package org.metaeffekt.core.maven.inventory.depres;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RequirementsResult {
    private static final Logger LOG = LoggerFactory.getLogger(RequirementsResult.class);

    protected final Map<String, Set<String>> packageToRequiredPackages;
    protected final Map<String, Set<String>> packageToUnresolvedRequirements;

    public RequirementsResult(Map<String, Set<String>> packageToRequiredPackages,
                              Map<String, Set<String>> packageToUnresolvedRequirements) {
        this.packageToRequiredPackages = Collections.unmodifiableMap(packageToRequiredPackages);
        this.packageToUnresolvedRequirements = Collections.unmodifiableMap(packageToUnresolvedRequirements);
    }

    public Map<String, Set<String>> getPackageToRequiredPackages() {
        return packageToRequiredPackages;
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


            Map<String, Set<String>> significantUnresolvable = new TreeMap<>();

            for (Map.Entry<String, Set<String>> e : packageToUnresolvedRequirements.entrySet()) {
                boolean significant = e.getValue().stream().anyMatch((req) -> !req.startsWith("rpmlib("));

                if (significant) {
                    significantUnresolvable.put(e.getKey(), e.getValue());
                }
            }

            if (significantUnresolvable.size() == 0) {
                return;
            }

            LOG.error("Logging [{}] significant issues:", significantUnresolvable.size());

            for (Map.Entry<String, Set<String>> e : significantUnresolvable.entrySet()) {
                LOG.error("- " + e.getKey());
                for (String unresolvable : e.getValue()) {
                    LOG.error("  - " + unresolvable);
                }
            }
        } else {
            LOG.info("Map of unsatisfiable dependencies is empty.");
        }
    }

    // TODO: add option to output a "required but not found / but not installed" category
}
