package org.metaeffekt.core.inventory.processor.adapter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ToString
@Getter
public class NpmModule extends Module {

    /**
     * Name of the module.
     */
    final String name;

    /**
     * Path within the given context or source file the module is detected/located.
     */
    final String path;

    // FIXME-KKL: is this the resolved version; check usage
    @Setter
    String version;

    @Setter
    String url;

    @Setter
    String hash;

    @Setter
    boolean resolved;

    @Setter
    private boolean isRuntimeDependency;

    @Setter
    private boolean isDevDependency;

    @Setter
    private boolean isPeerDependency;

    @Setter
    private boolean isOptionalDependency;

    @Setter
    private Map<String, String> devDependencies;

    @Setter
    private Map<String, String> runtimeDependencies;

    @Setter
    private Map<String, String> peerDependencies;

    @Setter
    private Map<String, String> optionalDependencies;

    @Setter
    private List<NpmModule> dependentModules = new ArrayList<>();

    public NpmModule(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String deriveQualifier() {
        if (version != null) {
            return name + "-" + version;
        }
        return name;
    }
}
