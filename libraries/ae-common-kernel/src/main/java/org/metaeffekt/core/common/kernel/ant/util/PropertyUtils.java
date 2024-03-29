/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.common.kernel.ant.util;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;
import org.metaeffekt.core.common.kernel.ant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class PropertyUtils {

    public static final String PROPERTY_SYSTEM_LEVEL = "system";

    public static final String PROPERTY_PROJECT_LEVEL = "project";

    private static final Logger LOG = LoggerFactory.getLogger(PropertyUtils.class);

    public static String getProperty(String key, Properties properties, Project project) {
        Object result = null;
        if (properties != null) {
            result = properties.get(key);
        }
        if (result == null && project != null) {
            result = project.getProperty(key);
        }
        if (result == null) {
            result = System.getProperty(key);
        }

        if (result != null)
            return ((String) result).trim();
        return null;
    }

    public static Properties loadPropertyFile(File file) {
        Properties properties = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            properties.load(in);
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            if (in != null) {
                FileUtils.close(in);
            }
        }
        return properties;
    }

    public static Properties loadPropertyFile(InputStream in) {
        final Properties properties = new Properties();

        if (in != null) {
            try {
                properties.load(in);
            } catch (IOException e) {
                throw new BuildException(e);
            } finally {
                FileUtils.close(in);
            }
        }

        return properties;
    }

    public static Properties loadProperties(File rootPropertyFile, File globalPropertyFile) {
        Properties properties = loadPropertyFile(rootPropertyFile);

        if (globalPropertyFile != null && globalPropertyFile.exists()
                && globalPropertyFile.isFile()) {
            LOG.debug("Loading global property file. '{}'.", globalPropertyFile);
            properties.putAll(loadPropertyFile(globalPropertyFile));
        }

        String path = rootPropertyFile.getAbsolutePath();
        path = path.substring(0, path.lastIndexOf(File.separatorChar));
        path = path.replaceAll("\\\\", "/");

        String includePropertyFiles = properties.getProperty(Constants.INCLUDE_PROPERTY_FILES_KEY);
        if (includePropertyFiles != null) {
            String[] files = includePropertyFiles.split(",");
            for (String file : files) {

                String filepath = file;

                if (file.contains("${basedir}")) {
                    filepath = file.replaceAll("\\$\\{basedir\\}", path);
                } else {
                    // assume file is relative
                    filepath = path + "/" + file;
                }

                LOG.debug("Loading included properties: {}", filepath);
                File propertyFile = new File(filepath);
                // check convention
                String filename = propertyFile.getName();
                if (!filename.toLowerCase().equals(filename)) {
                    LOG.warn("Included file name does not satisfy convention "
                            + "(properties are all lower case): [{}]", filename);
                }
                // be kind and load the property file
                Properties p = null;
                BuildException exception = null;
                try {
                    p = loadPropertyFile(propertyFile);
                } catch (BuildException e) {
                    exception = e;
                }

                // TODO remove this code once the convention violations are removed

                // in case the file is not found try the lower case version (helps to move to
                // convention)
                if (p == null) {
                    propertyFile = new File(propertyFile.getParent(), filename.toLowerCase());
                    if (propertyFile.exists()) {
                        p = loadPropertyFile(propertyFile);
                    }
                }

                // in case the file is not found try the lower case version (helps to move to
                // convention)
                if (p == null) {
                    propertyFile = new File(propertyFile.getParent(), filename.replaceAll(
                            "([A-Z])", ".$1").toLowerCase());
                    if (propertyFile.exists()) {
                        p = loadPropertyFile(propertyFile);
                    }
                }

                if (p == null) {
                    // throw the initial exception for the inital property file
                    throw exception;
                }

                // compose properties (considering priority decreases from root to includes)
                p.putAll(properties);
                properties = p;
            }
        }

        return properties;
    }

    public static void setProperty(String name, String value, String level, Project project) {
        if (PROPERTY_SYSTEM_LEVEL.equalsIgnoreCase(level)) {
            System.setProperty(name, value);
        } else {
            if (level == null || PROPERTY_PROJECT_LEVEL.equalsIgnoreCase(level)) {
                project.setProperty(name, value);
            } else {
                throw new BuildException("Unknown property level: [" + level + "].");
            }
        }
    }
}
