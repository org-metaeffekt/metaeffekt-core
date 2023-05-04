package org.metaeffekt.core.maven.inventory.depres;

import java.util.Collections;
import java.util.Set;

public class ProviderLookupResult {
    /**
     * Package names, resolved requirements.
     */
    public final Set<String> requiredPackages;
    public final boolean resolverSuccess;

    public ProviderLookupResult(Set<String> requiredPackages, boolean resolverSuccess) {
        this.requiredPackages = requiredPackages;
        this.resolverSuccess = resolverSuccess;
    }

    public static ProviderLookupResult successWithNone() {
        return new ProviderLookupResult(Collections.emptySet(), true);
    }
}
