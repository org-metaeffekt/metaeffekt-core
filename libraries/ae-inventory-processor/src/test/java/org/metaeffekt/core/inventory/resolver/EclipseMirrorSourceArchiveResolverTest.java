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
package org.metaeffekt.core.inventory.resolver;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EclipseMirrorSourceArchiveResolverTest {

    @Test
    public void testMatchAndReplace() {
        EclipseMirrorSourceArchiveResolver resolver = new EclipseMirrorSourceArchiveResolver();
        Artifact artifact = new Artifact();
        artifact.setId("artifact_1.1.0.v201812140000.jar");
        List<String> sourceArtifactId = resolver.matchAndReplace(artifact);
        Assert.assertEquals("artifact.source_1.1.0.v201812140000.jar", sourceArtifactId.get(0));
    }

    @Test
    public void testResolveTryRun() {
        EclipseMirrorSourceArchiveResolver resolver = new EclipseMirrorSourceArchiveResolver();
        String artifactId = "artifact_1.1.0.v201812140000.jar";

        Artifact artifact = new Artifact();
        artifact.setId(artifactId);

        List<String> mirrorBaseUrls = new ArrayList<>();
        /*
        mirrorBaseUrls.add("http://ftp.gnome.org/mirror/eclipse.org/rt/rap/3.1/R-20160607-1451/plugins/");
        mirrorBaseUrls.add("https://ftp.fau.de/eclipse/nattable/releases/1.3.0/repository/plugins/");
        mirrorBaseUrls.add("https://ftp.gnome.org/mirror/eclipse.org/eclipse/updates/4.6/R-4.6.3-201703010400/plugins/");
        mirrorBaseUrls.add("https://ftp.gnome.org/mirror/eclipse.org/eclipse/updates/4.6/R-4.6.1-201609071200/plugins/");
        mirrorBaseUrls.add("http://mirrors.ibiblio.org/eclipse/modeling/emf/emf/updates/2.11.x/core/R201602080841/plugins/");
        mirrorBaseUrls.add("http://mirror.xmission.net/eclipse/efxclipse/runtime-released/3.0.0/site/plugins/");
        mirrorBaseUrls.add("http://ftp.gnome.org/mirror/eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/plugins/");
        mirrorBaseUrls.add("http://ftp.gnome.org/mirror/eclipse.org/tools/ajdt/45/dev/update/ajdt-e45-2.2.4.201604061446/plugins/");
        mirrorBaseUrls.add("http://ftp.gnome.org/mirror/eclipse.org/modeling/emf/transaction/updates/milestones/S201506010221/plugins/");
        mirrorBaseUrls.add("http://ftp.gnome.org/mirror/eclipse.org/modeling/emf/validation/updates/releases/R201505312255/plugins/");
        mirrorBaseUrls.add("http://ftp.gnome.org/mirror/eclipse.org/birt/update-site/4.2-interim/plugins/");
        mirrorBaseUrls.add("https://ftp-stud.hs-esslingen.de/Mirrors/eclipse/birt/update-site/oxygen-interim/plugins/");
        mirrorBaseUrls.add("https://alfred.diamond.ac.uk/sites/download.eclipse.org/nattable/releases/1.4.0/repository/plugins/");
        mirrorBaseUrls.add("http://ftp.gnome.org/mirror/eclipse.org/modeling/emft/cdo/drops/R20130613-1157/plugins/");
        mirrorBaseUrls.add("http://www.mirrorservice.org/sites/download.eclipse.org/eclipseMirror/scout/releases/4.0/4.0.201/scout.rap/R8-20151103-1108/plugins/");
        mirrorBaseUrls.add("http://odysseus.informatik.uni-oldenburg.de/updatesite/sparql/origin/development/update_585/plugins/");
        mirrorBaseUrls.add("http://ftp.gnome.org/mirror/eclipse.org/modeling/emf/compare/updates/releases/3.1/R201509120604/plugins/");
        mirrorBaseUrls.add("http://www.simantics.org/update/tools/orbit/downloads/drops/R20170516192513_old/repository/plugins/");
        mirrorBaseUrls.add("http://ftp.mirrorservice.org/sites/download.eclipse.org/eclipseMirror/recommenders/updates/stable/plugins/");
        mirrorBaseUrls.add("http://odysseus.informatik.uni-oldenburg.de/updatesite/origin.removed/development/update_612/plugins/");
        mirrorBaseUrls.add("http://ftp.musicbrainz.org/pub/eclipse/scout/releases/4.2/4.2.0/scout.rap/R34-20170126-1338/plugins/");
        mirrorBaseUrls.add("http://ftp.gnome.org/mirror/eclipse.org/buildship/updates/e46/releases/2.x/2.0.0.v20170111-1029/plugins/");
*/

        mirrorBaseUrls.add("file:src/test/resources/resolver/dummy-repo/");

        resolver.setMirrorBaseUrls(mirrorBaseUrls);

        File targetDir = new File("target/test-downloads");
        Assert.assertNotNull(resolver.resolveArtifactSourceArchive(artifact, targetDir));
    }

    @Test
    public void testResolveOnline() {
        EclipseMirrorSourceArchiveResolver resolver = new EclipseMirrorSourceArchiveResolver();
        resolver.setUriResolver(new RemoteUriResolver(new Properties()));
        String artifactId = "org.eclipse.emf.ant_2.8.0.v20160208-0841.jar";

        Artifact artifact = new Artifact();
        artifact.setId(artifactId);

        List<String> mirrorBaseUrls = new ArrayList<>();
        mirrorBaseUrls.add("http://mirrors.ibiblio.org/eclipse/modeling/emf/emf/updates/2.11.x/core/R201602080841/plugins/");

        resolver.setMirrorBaseUrls(mirrorBaseUrls);

        File targetDir = new File("target/test-downloads");
        Assert.assertNotNull(resolver.resolveArtifactSourceArchive(artifact, targetDir));
    }


}
