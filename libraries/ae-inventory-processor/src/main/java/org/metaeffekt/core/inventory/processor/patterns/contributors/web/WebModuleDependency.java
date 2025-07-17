package org.metaeffekt.core.inventory.processor.patterns.contributors.web;

import lombok.Data;

/**
 * Represents a dependency of a WebModule. The qualifying boolean indicate the character of the dependency in the
 * context of the WenModule.
 */
@Data
public class WebModuleDependency {

    private String name;

    /**
     * The version qualifier may be imprecise.
     */
    private String versionRange;

    /**
     * The resolved version.
     */
    private String resolvedVersion;

    // NOTE: these details refer to the outer context
    private boolean isRuntimeDependency;
    private boolean isDevDependency;
    private boolean isPeerDependency;
    private boolean isOptionalDependency;
    private boolean isTestDependency;
    private boolean isOverwriteDependency;

    // NOTE: here no dependency tree is covered
}
