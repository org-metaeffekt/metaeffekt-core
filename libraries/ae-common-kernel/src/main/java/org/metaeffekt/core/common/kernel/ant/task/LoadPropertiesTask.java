/*
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.common.kernel.ant.task;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.metaeffekt.core.common.kernel.ant.util.PropertyUtils;

import java.io.File;
import java.util.Map;
import java.util.Properties;

public class LoadPropertiesTask extends Task {

    /**
     * The root property file. The root file may contain a include property to reference further
     * property files.
     */
    private File rootPropertyFile;

    /**
     * The global property file allows to define global property overwrites.
     */
    private File globalPropertyFile;

    private String prefix = null;

    private transient Properties properties;

    private boolean verbose = true;

    /**
     * Executes the task.
     *
     * @see Task#execute()
     */
    @Override
    public void execute() throws BuildException {
        // find all property files of relevance
        addProperties(PropertyUtils.loadProperties(rootPropertyFile, globalPropertyFile));

        // insert the properties into the project for later use
        Project project = getProject();
        populateProjectProperties(getProperties(), project);
    }

    protected void populateProjectProperties(Properties properties, Project project) {
        for (Object keyObj : properties.keySet()) {
            String key = (String) keyObj;
            String newKey = key;
            if (prefix != null) {
                newKey = prefix + "." + key;
            }
            project.setProperty(newKey, properties.getProperty(key));
        }
    }

    /**
     * Loads the properties as configured for this task.
     *
     * @return The loaded properties.
     */
    protected Properties loadProperties() {
        if (verbose) {
            log("Loading root properties: " + rootPropertyFile);
        }
        return PropertyUtils.loadProperties(rootPropertyFile, globalPropertyFile);
    }

    public File getRootPropertyFile() {
        return rootPropertyFile;
    }

    public void setRootPropertyFile(File rootPropertyFile) {
        this.rootPropertyFile = rootPropertyFile;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    protected Properties getProperties() {
        return properties;
    }

    protected void setProperties(Properties properties) {
        this.properties = properties;
    }

    public File getGlobalPropertyFile() {
        return globalPropertyFile;
    }

    public void setGlobalPropertyFile(File globalPropertyFile) {
        this.globalPropertyFile = globalPropertyFile;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void addProperties(Properties properties) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.putAll(properties);
    }

    public void addProperties(Map<String, String> properties) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.putAll(properties);
    }

}
