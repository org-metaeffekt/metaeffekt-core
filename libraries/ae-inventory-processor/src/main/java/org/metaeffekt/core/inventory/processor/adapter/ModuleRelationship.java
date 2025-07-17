package org.metaeffekt.core.inventory.processor.adapter;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.poifs.macros.Module;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ModuleRelationship<M extends Module> {

    /**
     * The module for which dependency relationships are described
     */
    private M module;

    /**
     * ModuleQualifiers for this module. Externally defined, may vary due to context. Many qualifiers may map here.
     */
    private List<ModuleQualifier> moduleQualifiers;

    private List<ModuleQualifier> runtimeDependencies = new ArrayList<>();
    private List<ModuleQualifier> devDependencies = new ArrayList<>();
    private List<ModuleQualifier> peerDependencies = new ArrayList<>();
    private List<ModuleQualifier> optionalDependencies = new ArrayList<>();

}
