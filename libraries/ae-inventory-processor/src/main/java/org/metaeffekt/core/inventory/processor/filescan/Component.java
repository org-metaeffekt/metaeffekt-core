package org.metaeffekt.core.inventory.processor.filescan;

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

@Getter
@Setter
public class Component {

    private static final Logger LOG = LoggerFactory.getLogger(Component.class);

    private String identifier;
    private List<File> identifiedComponentFiles;
    private List<File> sharedIncludedFiles;
    private List<File> sharedExcludedFiles;
    private Set<ArtifactFile> artifactFiles;


    public static Component produceFileList(FilePatternQualifierMapper mapper) {
        Component component = new Component();
        component.setIdentifier(mapper.getQualifier());
        List<File> identifiedComponentFiles = new ArrayList<>();
        List<File> sharedIncludedFiles = new ArrayList<>();
        List<File> sharedExcludedFiles = new ArrayList<>();
        for (File file : mapper.getFiles()) {
            for (ComponentPatternData cpd : mapper.getComponentPatternDataList()) {
                String componentIncludePattern = cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN);
                String sharedIncludePattern = cpd.get(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN);
                String sharedExcludePattern = cpd.get(ComponentPatternData.Attribute.SHARED_EXCLUDE_PATTERN);
                if (componentIncludePattern != null && !componentIncludePattern.isEmpty()) {
                    if (FileUtils.matches(componentIncludePattern, FileUtils.normalizePathToLinux(file)) &&
                        !FileUtils.matches(sharedExcludePattern, FileUtils.normalizePathToLinux(file))) {
                        identifiedComponentFiles.add(file);
                    }
                }
                if (sharedIncludePattern != null && !sharedIncludePattern.isEmpty()) {
                    if (FileUtils.matches(sharedIncludePattern, FileUtils.normalizePathToLinux(file))) {
                        sharedIncludedFiles.add(file);
                    }
                }
                if (sharedExcludePattern != null && !sharedExcludePattern.isEmpty()) {
                    if (FileUtils.matches(sharedExcludePattern, FileUtils.normalizePathToLinux(file))) {
                        sharedExcludedFiles.add(file);
                    }
                }
            }
        }
        component.setIdentifiedComponentFiles(identifiedComponentFiles);
        component.setSharedIncludedFiles(sharedIncludedFiles);
        component.setSharedExcludedFiles(sharedExcludedFiles);
        return component;
    }
    
    public static void analyzeComponents(Inventory inventory, Component... components) {
        // Map to keep track of artifacts and the components they belong to
        Map<ArtifactFile, Set<Component>> artifactToComponentsMap = new HashMap<>();

        // Assign artifacts to components based on include and exclude patterns
        /*for (Artifact artifact : inventory.getArtifacts()) {
            for (Component component : components) {
                boolean included = artifact.getPathInAsset()
                boolean excluded = PatternMatcher.matches(artifact.getPath(), component.getExcludePatterns());

                if (included && !excluded) {
                    component.addArtifact(artifact);
                    artifact.addOwningComponent(component);
                }
            }
        }*/

        // Now, artifacts know which components they belong to
        // No need to remove shared artifacts; we retain all information
    }
}
