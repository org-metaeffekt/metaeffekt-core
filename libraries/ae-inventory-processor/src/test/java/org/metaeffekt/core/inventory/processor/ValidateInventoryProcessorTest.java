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
package org.metaeffekt.core.inventory.processor;

import org.apache.commons.lang3.StringUtils;
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

import static org.metaeffekt.core.inventory.processor.model.Constants.ASTERISK;

@Ignore // Integration Test; move out
public class ValidateInventoryProcessorTest {

    private static final Logger LOG = LoggerFactory.getLogger(ValidateInventoryProcessorTest.class);

    @Test
    public void testInventoryProcessor() throws IOException {
        File baseDir = new File("XXX");
        Inventory commonInventory = new InventoryReader().readInventory(new File(baseDir, "XXX"));
        Inventory customerCommonInventory = new InventoryReader().readInventory(new File(baseDir, "XXX"));

        File inventoryFile = new File(baseDir, "XXX");

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
        mirrors.add("http://ftp.gnome.org/mirror/eclipse.org/birt/update-site/4.2-interim/plugins/");
        mirrors.add("https://ftp-stud.hs-esslingen.de/Mirrors/eclipse/birt/update-site/oxygen-interim/plugins/");
        mirrors.add("https://alfred.diamond.ac.uk/sites/download.eclipse.org/nattable/releases/1.4.0/repository/plugins/");
        mirrors.add("http://ftp.gnome.org/mirror/eclipse.org/modeling/emft/cdo/drops/R20130613-1157/plugins/");
        mirrors.add("http://www.mirrorservice.org/sites/download.eclipse.org/eclipseMirror/scout/releases/4.0/4.0.201/scout.rap/R8-20151103-1108/plugins/");
        mirrors.add("http://odysseus.informatik.uni-oldenburg.de/updatesite/sparql/origin/development/update_585/plugins/");
        mirrors.add("http://ftp.gnome.org/mirror/eclipse.org/modeling/emf/compare/updates/releases/3.1/R201509120604/plugins/");
        mirrors.add("http://www.simantics.org/update/tools/orbit/downloads/drops/R20170516192513_old/repository/plugins/");
        mirrors.add("http://ftp.mirrorservice.org/sites/download.eclipse.org/eclipseMirror/recommenders/updates/stable/plugins/");
        mirrors.add("http://odysseus.informatik.uni-oldenburg.de/updatesite/origin.removed/development/update_612/plugins/");
        mirrors.add("http://ftp.musicbrainz.org/pub/eclipse/scout/releases/4.2/4.2.0/scout.rap/R34-20170126-1338/plugins/");
        mirrors.add("http://ftp.gnome.org/mirror/eclipse.org/buildship/updates/e46/releases/2.x/2.0.0.v20170111-1029/plugins/");


        for (Artifact artifact : new InventoryReader().readInventory(inventoryFile).getArtifacts()) {

            if (!StringUtils.isEmpty(artifact.getLicense()) && artifact.getLicense().contains("Proprietary")) {
                continue;
            }

            artifact.deriveArtifactId();

            String artifactId = artifact.getId();
            if (artifactId.contains("_") && artifactId.endsWith(".jar") && !artifactId.contains(ASTERISK)) {
                String sourceId;
                if (artifactId.contains(".source_")) {
                    sourceId = artifactId;
                } else {
                    sourceId = artifactId.replaceFirst("_", ".source_");
                }

                boolean found = false;
                for (String mirror : mirrors) {
                    URL url = new URL(mirror + sourceId);
                    URLConnection urlConnection = url.openConnection();
                    String contentEncoding = urlConnection.getContentType();
                    if (contentEncoding.contains("application/java-archive") || contentEncoding.contains("application/x-java-archive")) {
                        LOG.info("URL: {}", url);
                        found = true;
                        break;
                    } else {
                        if (contentEncoding.contains("text/html")) {
                            // ignore
                        } else {
                            LOG.error("Don't know encoding: {}", contentEncoding);
                        }
                    }
                }
                if (!found) {
                    LOG.error("Cannot find source for {}.", sourceId);
                }
            }
        }
    }
}
