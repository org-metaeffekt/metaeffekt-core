package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.logging.Log;
import org.metaeffekt.core.inventory.resolver.ArtifactPattern;

import java.util.List;
import java.util.Properties;

/**
 * Maven configurable delegate for {@link org.metaeffekt.core.inventory.resolver.ArtifactSourceRepository}.
 */
public class SourceRepository extends IdentifiableComponent {

    private String targetFolder;

    private EclipseMirror eclipseMirror;

    private ComponentMirror componentMirror;

    private List<String> patterns;

    private boolean ignoreMatches;

    public org.metaeffekt.core.inventory.resolver.ArtifactSourceRepository constructDelegate() {

        org.metaeffekt.core.inventory.resolver.ArtifactSourceRepository artifactSourceRepository = new org.metaeffekt.core.inventory.resolver.ArtifactSourceRepository();
        artifactSourceRepository.setId(getId());
        artifactSourceRepository.setTargetFolder(targetFolder);
        artifactSourceRepository.setIgnoreMatches(ignoreMatches);

        if (getPatterns() != null) {
            for (String pattern : patterns) {
                String[] split = pattern.split(":");
                artifactSourceRepository.register(new ArtifactPattern(extractPattern(0, split),
                    extractPattern(1, split), extractPattern(2, split), extractPattern(3, split)));
            }
        }

        if (eclipseMirror != null && componentMirror != null) {
            throw new IllegalStateException(String.format(
                    "Cannot configure source repository with id %s. Only one mirror can be configured.", getId()));
        }

        Properties p = new Properties();
        if (getProperties() != null) {
            p.putAll(getProperties());
        }

        if (eclipseMirror != null) {
            artifactSourceRepository.setSourceArchiveResolver(eclipseMirror.createResolver(p));
        }

        if (componentMirror != null) {
            artifactSourceRepository.setSourceArchiveResolver(componentMirror.createResolver(p));
        }

        return artifactSourceRepository;
    }

    public String getTargetFolder() {
        return targetFolder;
    }

    public void setTargetFolder(String targetFolder) {
        this.targetFolder = targetFolder;
    }

    public EclipseMirror getEclipseMirror() {
        return eclipseMirror;
    }

    public void setEclipseMirror(EclipseMirror eclipseMirror) {
        this.eclipseMirror = eclipseMirror;
    }

    public ComponentMirror getComponentMirror() {
        return componentMirror;
    }

    public void setComponentMirror(ComponentMirror componentMirror) {
        this.componentMirror = componentMirror;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public void dumpConfig(Log log, String prefix) {
        super.dumpConfig(log, prefix);
        log.debug(prefix + "  targetFolder: " + getTargetFolder());
        log.debug(prefix + "  patterns: " + getPatterns());
        if (eclipseMirror != null) eclipseMirror.dumpConfig(log, prefix + "  ");
        if (componentMirror != null) componentMirror.dumpConfig(log, prefix + "  ");
    }
}
