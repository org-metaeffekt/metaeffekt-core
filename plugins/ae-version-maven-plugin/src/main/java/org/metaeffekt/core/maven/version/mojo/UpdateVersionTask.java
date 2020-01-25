/**
 * Copyright 2009-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.maven.version.mojo;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.optional.ReplaceRegExp;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;

import java.io.File;
import java.util.Map;

/**
 * Class supporting the version replacement in POMs.
 * 
 * @author Karsten Klein
 */
public class UpdateVersionTask {

    private static final String VALID_VERSION = "[^<^>^\\{^\\}]{1,}";

    private String projectVersion;

    private Map<String, String> propertyVersionMap;

    private Map<String, String> groupIdVersionMap;

    private File projectPath;

    private String[] includes;

    private String[] excludes;

    public void updateVersions() {
        LoggingProjectAdapter project = new LoggingProjectAdapter();
        project.setBaseDir(projectPath);

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(projectPath);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.scan();

        String[] files = scanner.getIncludedFiles();

        for (String file : files) {
            udpateVersions(project, new File(projectPath, file));
        }
    }

    private void udpateVersions(LoggingProjectAdapter project, File file) {
        project.log("Updating versions in file: " + file.getAbsolutePath());

        for (Map.Entry<String, String> groupIdVersionEntry : groupIdVersionMap.entrySet()) {
            String groupId = groupIdVersionEntry.getKey();
            String version = groupIdVersionEntry.getValue();

            ReplaceRegExp replaceTask = createRegExpTask(file, project, "s");
            replaceTask.setMatch("(<parent>.*" +
                    "<groupId>" + groupId + "</groupId>" +
                    ".*<version>)" + VALID_VERSION + "(</version>.*</parent>)");
            replaceTask.setReplace("\\1" + version + "\\2");
            replaceTask.execute();

            // do it again now with group id after version variant
            replaceTask = createRegExpTask(file, project, "s");
            replaceTask.setMatch("(<parent>" +
                    ".*<version>)" + VALID_VERSION + "(</version>.*" +
                    "<groupId>" + groupId + "</groupId>" + ".*</parent>)");
            replaceTask.setReplace("\\1" + version + "\\2");
            replaceTask.execute();
        }

        for (Map.Entry<String, String> groupIdVersionEntry : propertyVersionMap.entrySet()) {
            String property = groupIdVersionEntry.getKey();
            String version = groupIdVersionEntry.getValue();

            ReplaceRegExp replaceTask = createRegExpTask(file, project, "sg");
            replaceTask.setMatch("(<" + property + ">)" + VALID_VERSION + "(</" + property + ">)");
            replaceTask.setReplace("\\1" + version + "\\2");
            replaceTask.execute();
        }

        if (projectVersion != null) {
            // mark all occurrences of <parent>
            ReplaceRegExp replaceTask = createRegExpTask(file, project, "sg");
            replaceTask.setMatch("<parent>");
            replaceTask.setReplace("<parent_>");
            replaceTask.execute();
            
            // mark all occurrences of <packaging>
            replaceTask = createRegExpTask(file, project, "sg");
            replaceTask.setMatch("<packaging>");
            replaceTask.setReplace("<packaging_>");
            replaceTask.execute();
            
            // unmark first occurrance of <parent>
            replaceTask = createRegExpTask(file, project, "s");
            replaceTask.setMatch("(<parent_>)");
            replaceTask.setReplace("<parent>");
            replaceTask.execute();

            // unmark first occurrance of <packaging>
            replaceTask = createRegExpTask(file, project, "s");
            replaceTask.setMatch("(<packaging_>)");
            replaceTask.setReplace("<packaging>");
            replaceTask.execute();
            
            // perform version replacements
            replaceTask = createRegExpTask(file, project, "s");
            replaceTask.setMatch("(<project [^>]*>.*<version>)" + VALID_VERSION
                    + "(</version>.*<parent>.*)");
            replaceTask.setReplace("\\1" + projectVersion + "\\2");
            replaceTask.execute();
            
            replaceTask = createRegExpTask(file, project, "s");
            replaceTask.setMatch("(<project [^>]*>.*<version>)" + VALID_VERSION
                    + "(</version>.*<packaging>.*)");
            replaceTask.setReplace("\\1" + projectVersion + "\\2");
            replaceTask.execute();

            replaceTask = createRegExpTask(file, project, "s");
            replaceTask.setMatch("(<project [^>]*>.*<version>)" + VALID_VERSION
                    + "(</version>.*<!-- PROJECT-VERSION-MARKER -->.*)");
            replaceTask.setReplace("\\1" + projectVersion + "\\2");
            replaceTask.execute();
            
            // NOTE: the latter replacement expects that every pom has a parent or a packaging tag 
            // AFTER the version element. In case this it not true no replacement will be performed.
            // In order to enforce a replacement one could simply change the order or provide a 
            // commented empty parent // tag <!-- <parent></parent> --> or by specifying a 
            // (redundant) packaging element (recommended).
            
            // unmark all remaining <parent>
            replaceTask = createRegExpTask(file, project, "sg");
            replaceTask.setMatch("(<parent_>)");
            replaceTask.setReplace("<parent>");
            replaceTask.execute();

            // unmark all remaining <packaging>
            replaceTask = createRegExpTask(file, project, "sg");
            replaceTask.setMatch("(<packaging_>)");
            replaceTask.setReplace("<packaging>");
            replaceTask.execute();

        }
    }

    protected ReplaceRegExp createRegExpTask(File file, LoggingProjectAdapter project, String flags) {
        ReplaceRegExp replaceTask = new ReplaceRegExp();
        replaceTask.setProject(project);
        replaceTask.setByLine(false);
        replaceTask.setFlags(flags);
        replaceTask.setFile(file);
        return replaceTask;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    public Map<String, String> getPropertyVersionMap() {
        return propertyVersionMap;
    }

    public void setPropertyVersionMap(Map<String, String> propertyVersionMap) {
        this.propertyVersionMap = propertyVersionMap;
    }

    public Map<String, String> getGroupIdVersionMap() {
        return groupIdVersionMap;
    }

    public void setGroupIdVersionMap(Map<String, String> groupIdVersionMap) {
        this.groupIdVersionMap = groupIdVersionMap;
    }

    public File getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(File projectPath) {
        this.projectPath = projectPath;
    }

    public String[] getIncludes() {
        return includes;
    }

    public void setIncludes(String[] includes) {
        this.includes = includes;
    }

    public String[] getExcludes() {
        return excludes;
    }

    public void setExcludes(String[] excludes) {
        this.excludes = excludes;
    }

}
