package org.metaeffekt.core.inventory.processor.filescan;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class ArtifactFile {
    private String path; // File path
    private Set<Component> owningComponents; // Components this artifact belongs to

    public ArtifactFile(String path) {
        this.path = path;
        this.owningComponents = new HashSet<>();
    }

    public void addOwningComponent(Component component) {
        owningComponents.add(component);
    }

    public boolean isShared() {
        return owningComponents.size() > 1;
    }
}
