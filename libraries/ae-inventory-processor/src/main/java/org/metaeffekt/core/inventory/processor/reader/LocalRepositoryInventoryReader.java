/**
 * Copyright 2009-2018 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.reader;

import org.apache.tools.ant.DirectoryScanner;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalRepositoryInventoryReader {

    private String[] includes = new String[]{"**/*"};

    private String[] excludes = new String[]{"**/*.pom", "**/*.sha1"};

    public Inventory readInventory(File file, String project) throws FileNotFoundException,
            IOException {

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(file);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.scan();

        Inventory inventory = new Inventory();
        List<Artifact> artifacts = new ArrayList<>();
        inventory.setArtifacts(artifacts);

        for (String path : scanner.getIncludedFiles()) {

            path = path.replace('\\', '/');

            String[] splitPath = path.split("\\/");

            if (splitPath.length > 3) {
                Artifact artifact = new Artifact();
                int index = splitPath.length - 1;
                artifact.setId(splitPath[index--]);
                artifact.setVersion(splitPath[index--]);
                artifact.setArtifactId(splitPath[index--]);
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i <= index; i++) {
                    if (i != 0) {
                        sb.append('.');
                    }
                    sb.append(splitPath[i]);
                }
                artifact.setGroupId(sb.toString());
                artifact.addProject(project);
                artifacts.add(artifact);
            }
        }

        inventory.mergeDuplicates();

        return inventory;
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
