package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.logging.Log;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMirror extends IdentifiableComponent {
    private List<String> mirrorUrls = new ArrayList<>();

    public List<String> getMirrorUrls() {
        return mirrorUrls;
    }

    public void setMirrorUrls(List<String> mirrorUrls) {
        this.mirrorUrls = mirrorUrls;
    }

    public void dumpConfig(Log log, String prefix) {
        super.dumpConfig(log, prefix);
        log.debug(prefix + "  mirrorUrls: " + getMirrorUrls());
    }

}
