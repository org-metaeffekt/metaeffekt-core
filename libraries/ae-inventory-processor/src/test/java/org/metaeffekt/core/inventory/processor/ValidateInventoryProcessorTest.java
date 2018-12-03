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
package org.metaeffekt.core.inventory.processor;

import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.metaeffekt.core.inventory.processor.model.Constants.ASTERISK;

@Ignore // Integration Test; move out
public class ValidateInventoryProcessorTest {

    private static final Logger LOG = LoggerFactory.getLogger(ValidateInventoryProcessorTest.class);

    @Test
    public void testInventoryProcessor() throws IOException {

        Inventory commonInventory = new InventoryReader().readInventory(new File(
                "/Volumes/metaeffekt-admin/2018-11-23_backup_S-DIT/ae-workbench-input/common/inventory/ae-artifact-inventory.xls"));
        Inventory customerCommonInventory = new InventoryReader().readInventory(new File(
                "/Volumes/metaeffekt-admin/2018-11-23_backup_S-DIT/ae-workbench-input/S-DIT/common/inventory/s-dit-artifact-inventory.xls"));
        Inventory projectCommonInventory = new InventoryReader().readInventory(new File(
                "/Volumes/metaeffekt-admin/2018-11-23_backup_S-DIT/ae-workbench-input/S-DIT/S-DIT-002/common/inventory/venus-common-artifact-inventory.xls"));

        Inventory inventory = projectCommonInventory;

     //   customerCommonInventory.inheritArifacts(commonInventory, true);
     //   projectCommonInventory.inheritArifacts(customerCommonInventory, true);

        File licensesDir = new File("/Volumes/metaeffekt-admin/2018-11-23_backup_S-DIT/ae-workbench-input/S-DIT/S-DIT-002/common/licenses");
        File licensesTargetDir = new File("/Volumes/metaeffekt-admin/2018-11-23_backup_S-DIT/ae-workbench-output/S-DIT/S-DIT-002/common/licenses");

        Properties properties = new Properties();
        properties.setProperty(ValidateInventoryProcessor.LICENSES_DIR, licensesDir.getPath());
        properties.setProperty(ValidateInventoryProcessor.LICENSES_TARGET_DIR, licensesTargetDir.getPath());
        properties.setProperty(ValidateInventoryProcessor.FAIL_ON_ERROR, Boolean.FALSE.toString());
        properties.setProperty(ValidateInventoryProcessor.CREATE_LICENSE_FOLDERS, Boolean.TRUE.toString());
        properties.setProperty(ValidateInventoryProcessor.CREATE_COMPONENT_FOLDERS, Boolean.TRUE.toString());

        LOG.info("Validation properties:");
        properties.entrySet().forEach(e -> LOG.info("{}: {}", e.getKey(), e.getValue()));

        ValidateInventoryProcessor validateInventoryProcessor = new ValidateInventoryProcessor(properties);
        //validateInventoryProcessor.process(inventory);

        List<String> mirrors = new ArrayList<>();
        mirrors.add("http://ftp.gnome.org/mirror/eclipse.org/rt/rap/3.1/R-20160607-1451/plugins/");
        mirrors.add("https://ftp.fau.de/eclipse/nattable/releases/1.3.0/repository/plugins/");
        mirrors.add("https://ftp.gnome.org/mirror/eclipse.org/eclipse/updates/4.6/R-4.6.3-201703010400/plugins/");
        mirrors.add("https://ftp.gnome.org/mirror/eclipse.org/eclipse/updates/4.6/R-4.6.1-201609071200/plugins/");
        mirrors.add("http://mirrors.ibiblio.org/eclipse/modeling/emf/emf/updates/2.11.x/core/R201602080841/plugins/");
        mirrors.add("http://mirror.xmission.net/eclipse/efxclipse/runtime-released/3.0.0/site/plugins/");
        mirrors.add("http://ftp.gnome.org/mirror/eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/");
        mirrors.add("http://ftp.gnome.org/mirror/eclipse.org/tools/ajdt/45/dev/update/ajdt-e45-2.2.4.201604061446/plugins/");
        mirrors.add("http://ftp.gnome.org/mirror/eclipse.org/modeling/emf/transaction/updates/milestones/S201506010221/plugins/");
        mirrors.add("http://ftp.gnome.org/mirror/eclipse.org/modeling/emf/validation/updates/releases/R201505312255/plugins/");

        for (Artifact artifact : inventory.getArtifacts()) {

            artifact.deriveArtifactId();

            String artifactId = artifact.getId();
            if (artifactId.startsWith("org.eclipse") && artifactId.endsWith(".jar") && !artifactId.contains(ASTERISK)) {
                String sourceId = artifactId.replace("_", ".source_");

                boolean found = false;
                for (String mirror : mirrors) {


                    URL url = new URL(mirror + sourceId);
                    URLConnection urlConnection = url.openConnection();
                    String contentEncoding = urlConnection.getContentType();
                    if (contentEncoding.contains("application/java-archive") || contentEncoding.contains("application/x-java-archive")) {
                        System.out.println(url);
                        found = true;
                        break;
                    } else {
                        if (contentEncoding.contains("text/html")) {
                            // ignore
                        } else {
                            System.err.println("Don't know: " + contentEncoding);
                        }
                    }
                }
                if (!found) {
                    System.err.println("Cannot find source for " + sourceId);
                }
            }
        }
    }
}
