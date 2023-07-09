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

import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CentOSInventoryExtractor extends AbstractInventoryExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(CentOSInventoryExtractor.class);

    public static final String FILE_PACKAGES_RPM_TXT = "packages_rpm.txt";

    @Override
    public boolean applies(File analysisDir) {
        return new File(analysisDir, FILE_PACKAGES_RPM_TXT).exists();
    }

    @Override
    public void extendInventory(File analysisDir, Inventory inventory) throws IOException {
        // find packages from the provided inputs
        List<PackageInfo> packageReferences = scan(analysisDir);
        packageReferences.forEach(p -> addOrMerge(analysisDir, inventory, p));
    }

    public List<PackageInfo> scan(File analysisDir) throws IOException {
        Map<String, PackageInfo> idToPackageReferenceMap = new HashMap<>();

        // NOTE: primary content driver is the package list and package meta files

        // generate (complementary) package list from scripts output
        parseRpmPackageList(analysisDir, idToPackageReferenceMap);

        // NOTE: secondary content are the content in the doc and license folders

        // generate package list from files in documentation dir
        packagesFromDocumentationDir(analysisDir, new File(analysisDir, FOLDER_USR_SHARE_DOC), idToPackageReferenceMap, true);

        // generate package list from directories in dedicated licenses dir
        packagesFromDocumentationDir(analysisDir, new File(analysisDir, FOLDER_USR_SHARE_LICENSE), idToPackageReferenceMap, false);

        // add additional details from package metadata files
        parseRpmPackageFiles(analysisDir, idToPackageReferenceMap);

        return new ArrayList<>(idToPackageReferenceMap.values());
    }

    private void parseRpmPackageFiles(File analysisDir, Map<String, PackageInfo> nameToPackageReferenceMap) throws IOException {
        for (Map.Entry<String, PackageInfo> entry : nameToPackageReferenceMap.entrySet()) {
            PackageInfo packageInfo = entry.getValue();
            String packageId = entry.getKey();
            String packageName = packageInfo.component;

            // check with version; backward compatibility
            File packageFile = new File(analysisDir, "package-meta/" + packageId + "_rpm.txt");
            if (!packageFile.exists()) {
                // check without version
                packageFile = new File(analysisDir, "package-meta/" + packageName + "_rpm.txt");
            }

            if (packageFile.exists()) {
                List<String> fileContentLines = FileUtils.readLines(packageFile, FileUtils.ENCODING_UTF_8);

                packageInfo.url = ParsingUtils.getValue(fileContentLines, "URL         :");
                packageInfo.summary = ParsingUtils.getValue(fileContentLines, "Summary     :");
                packageInfo.description = ParsingUtils.getValue(fileContentLines, "Description :");
                packageInfo.arch = ParsingUtils.getValue(fileContentLines, "Architecture:");
            } else {
                LOG.info("File {} does not exist.", packageFile);
            }
        }
    }


    public void parseRpmPackageList(File shareDir, Map<String, PackageInfo> idToPackageInfoMap) throws IOException {
        File rpmPackagesFile = new File(shareDir, FILE_PACKAGES_RPM_TXT);
        String packageFileContent = FileUtils.readFileToString(rpmPackagesFile, FileUtils.ENCODING_UTF_8);
        String[] lines = packageFileContent.split("\\n");

        for (String line : lines) {
            if (StringUtils.isEmpty(line)) continue;

            String[] elements = line.split("\\|");

            String name = elements[1].trim();
            String version = elements[2].trim();

            PackageInfo packageInfo = idToPackageInfoMap.get(name);
            if (packageInfo == null) {
                packageInfo = new PackageInfo();
            }

            packageInfo.version = version;
            packageInfo.id = name + "-" + version;
            packageInfo.component = name;
            packageInfo.name = name;
            packageInfo.version = version;
            packageInfo.license = elements[3].trim();
            if (elements.length > 4)
                packageInfo.url = elements[4].trim();
            if (elements.length > 5)
                packageInfo.arch = elements[5].trim();
            if (elements.length > 6)
                packageInfo.description = elements[6].trim();

            registerPackageInfo(packageInfo, idToPackageInfoMap);
        }
    }

}
