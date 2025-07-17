package org.metaeffekt.core.inventory.processor.patterns.contributors.web;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class collect different information items on a web module.
 */
@Data
public class WebModule implements Comparable<WebModule> {

    public File packageJsonFile;
    public File composerJsonFile;

    // TODO
    private String path;
    private String folder;

    // the anchor may be any file that belongs to the module
    private File anchor;
    private String anchorChecksum;

    // there may be one or more lock files for a component; these support to determine the transitive dependencies and
    // version
    private List<File> lockFiles = new ArrayList<>();

    // in case name or version are not defined; we consider the web module as abstract
    private String name;
    private String version;

    private String license;

    private String url;

    @Override
    public int compareTo(WebModule o) {
        return path.compareToIgnoreCase(o.path);
    }

    public boolean hasData() {
//        if (StringUtils.isBlank(name)) return false;
        if (StringUtils.isBlank(version)) return false;
        return anchor != null;
    }

    /**
     * Direct dependencies detected for the web module. An empty list means not specified.
     */
    private List<WebModuleDependency> directDependencies = new ArrayList<>();

}
