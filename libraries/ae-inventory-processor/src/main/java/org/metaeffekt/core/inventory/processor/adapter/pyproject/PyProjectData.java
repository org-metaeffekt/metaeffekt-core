package org.metaeffekt.core.inventory.processor.adapter.pyproject;

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;

import java.util.List;

/**
 * Class for storing parsed data for py projects.
 */
@Setter
@Getter
public class PyProjectData {
    private ResolvedModule projectModule;

    private List<UnresolvedModule> directRuntimeDependencies;

    private List<UnresolvedModule> directDevelopmentDependencies;

    private List<ResolvedModule> resolvedModulesFromLockFile;
}
