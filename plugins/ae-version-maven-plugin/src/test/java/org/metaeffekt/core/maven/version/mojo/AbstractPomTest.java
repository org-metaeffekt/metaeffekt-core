/*
 * Copyright 2009-2022 the original author or authors.
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

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public abstract class AbstractPomTest {

    @Before
    public void preparePoms() {
        Project p = new LoggingProjectAdapter();
        Copy copy = new Copy();
        copy.setProject(p);
        copy.setOverwrite(true);
        FileSet fileSet = new FileSet();
        fileSet.setDir(new File(getSourcePath()));
        fileSet.setIncludes("**/pom.xml");
        copy.addFileset(fileSet);
        copy.setTodir(new File(getTargetPath()));
        copy.execute();
    }


    @Test
    public void testReplacement() throws IOException {
        UpdateVersionTask task = new UpdateVersionTask();
        task.setProjectPath(new File(getTargetPath()));
        task.setIncludes(new String[]{"pom.xml"});
        task.setExcludes(new String[]{"**/target/**"});

        task.setProjectVersion("PROJECT-SNAPSHOT");

        Map<String, String> propertyVersionMap = new HashMap<String, String>();
        propertyVersionMap.put("ae.core.version", "XYZ-SNAPSHOT");
        propertyVersionMap.put("ae.other.version", "ae-SNAPSHOT");

        Map<String, String> groupIdVersionMap = new HashMap<String, String>();
        groupIdVersionMap.put("org.metaeffekt", "ae-SNAPSHOT");
        groupIdVersionMap.put("org.metaeffekt.core", "XYZ-SNAPSHOT");

        task.setGroupIdVersionMap(groupIdVersionMap);
        task.setPropertyVersionMap(propertyVersionMap);

        task.updateVersions();

        // verify
        String expected = FileUtils.readFileToString(new File(getSourcePath(), "pom.xml_expected"));
        String actual = FileUtils.readFileToString(new File(getTargetPath(), "pom.xml"));

        Assert.assertEquals(expected, actual);
    }

    public abstract String getSourcePath();

    public abstract String getTargetPath();

}
