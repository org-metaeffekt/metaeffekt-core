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
package org.metaeffekt.core.maven.inventory.extractor;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

public class AlpineInventoryExtractor extends AbstractInventoryExtractor {

    public static final String FILE_PACKAGES_APK_TXT = "packages_apk.txt";

    @Override
    public boolean applies(File analysisDir) {
        return new File(analysisDir, FILE_PACKAGES_APK_TXT).exists();
    }

    @Override
    public void extendInventory(File analysisDir, Inventory inventory) throws IOException {
        List<String> packages = parsePackages(analysisDir);

        initializeInventory(packages, inventory);

        for (Artifact artifact : inventory.getArtifacts()) {
            String id = artifact.getId();

            String path = "package-meta/" + id + "_apk.txt";
            File packageDetailsFile = new File(analysisDir, path);

            if (!packageDetailsFile.exists()) {
                throw new IllegalStateException(String.format("Expected package file %s does not exist.", id));
            }

            List<String> lines = FileUtils.readLines(packageDetailsFile, FileUtils.ENCODING_UTF_8);
            extractVersion(id, artifact, lines);
            extractLicense(artifact, lines);
            extractUrl(artifact, lines);

            artifact.set(KEY_TYPE, ARTIFACT_TYPE_PACKAGE);
        }

        for (Artifact artifact : inventory.getArtifacts()) {
            File shareFolder = new File(analysisDir, FOLDER_USR_SHARE_DOC);
            File packageFolder = new File(shareFolder, artifact.getId());

            if (packageFolder.exists()) {
                PackageInfo packageReference = new PackageInfo();
                Artifact analysis = packageReference.createArtifact(analysisDir);
            }

        }
    }

    public void initializeInventory(List<String> packages, Inventory inventory) {
        // initialize the inventory with just the package names
        for (String packageDescriptor : packages) {
            Artifact artifact = new Artifact();
            artifact.setId(packageDescriptor);
            artifact.setComponent(packageDescriptor);

            inventory.getArtifacts().add(artifact);
        }
    }

    public void extractUrl(Artifact artifact, List<String> lines) {
        {
            int urlIndex = getIndex(lines, "URL         :");
            if (urlIndex != -1) {
                String url = lines.get(urlIndex).substring("URL         :".length() + 1).trim();
                if (!StringUtils.isEmpty(url)) {
                    artifact.setUrl(url);
                    return;
                }
            }
        }

        {
            int homepageIndex = getIndex(lines, "Homepage:");
            if (homepageIndex != -1) {
                String url = lines.get(homepageIndex).substring("Homepage:".length() + 1).trim();
                if (!StringUtils.isEmpty(url)) {
                    artifact.setUrl(url);
                    return;
                }
            }
        }

        int webpageIndex = getIndex(lines, "webpage:");
        if (webpageIndex != -1) {
            String url = lines.get(webpageIndex + 1);
            artifact.setUrl(url);
        }
    }

    public List<String> parsePackages(File benchOutput) throws IOException {
        File packageFile = new File(benchOutput, FILE_PACKAGES_APK_TXT);
        return FileUtils.readLines(packageFile, FileUtils.ENCODING_UTF_8);
    }

    public int getIndex(List<String> lines, String key) {
        int index = -1;
        for (String line : lines) {
            index++;
            if (line.contains(key)) {
                return index;
            }
        }
        return -1;
    }

    public void extractLicense(Artifact artifact, List<String> lines) {
        int licenseIndex = getIndex(lines, "License     :");
        if (licenseIndex != -1) {
            String license = lines.get(licenseIndex).substring("License     :".length() + 1).trim();
            if (!StringUtils.isEmpty(license)) {
                artifact.set(KEY_DERIVED_LICENSE_PACKAGE, license);
                return;
            }
        }


        int lIndex = getIndex(lines, "license:");
        if (lIndex != -1) {
            String licenseExtract = lines.get(lIndex + 1);
            licenseExtract = licenseExtract.replace(" and ", " ");
            licenseExtract = licenseExtract.replace(" ", ", ");
            artifact.set(KEY_DERIVED_LICENSE_PACKAGE, licenseExtract);
        }
    }

    public void extractVersion(String id, Artifact artifact, List<String> lines) {
        {
            int vIndex = getIndex(lines, "Version     :");
            if (vIndex != -1) {
                String versionExtract = lines.get(vIndex);
                versionExtract = versionExtract.substring("Version     :".length() + 1).trim();
                artifact.setVersion(versionExtract);
                artifact.setId(id + "-" + versionExtract);
                return;
            }
        }

        {
            int vIndex = getIndex(lines, "Version:");
            if (vIndex != -1) {
                String versionExtract = lines.get(vIndex);
                versionExtract = versionExtract.substring("Version:".length() + 1).trim();
                artifact.setVersion(versionExtract);
                artifact.setId(id + "-" + versionExtract);
                return;
            }
        }

        int dIndex = getIndex(lines, "description:");
        if (dIndex != -1) {
            String versionExtract = lines.get(dIndex);
            if (!versionExtract.startsWith(id) || !versionExtract.contains(" description:")) {
                throw new IllegalStateException(String.format("Format assertion not matched: %s", versionExtract));
            }
            versionExtract = versionExtract.substring(id.length() + 1, versionExtract.indexOf(" description:"));
            artifact.setVersion(versionExtract);
            artifact.setId(id + "-" + versionExtract);
        }
    }

}
