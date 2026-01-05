/*
 * Copyright 2009-2026 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebianInventoryExtractor extends AbstractInventoryExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(DebianInventoryExtractor.class);

    public static final String FILE_PACKAGES_DPKG_TXT = "packages_dpkg.txt";

    public static final String STATUS_PREPARED = "pi";
    public static final String STATUS_INSTALLED = "ii";

    public static final String SEPARATOR_COLON = ":";
    public static final String SEPARATOR_DASH = "-";
    public static final String SEPARATOR_3PLUS = "+++";

    @Override
    public boolean applies(File analysisDir) {
        return new File(analysisDir, FILE_PACKAGES_DPKG_TXT).exists();
    }

    @Override
    public void extendInventory(File analysisDir, Inventory inventory) throws IOException {
        // find packages from the provided inputs
        final List<PackageInfo> packageReferences = scan(analysisDir);

        // process each
        packageReferences.forEach(p -> addOrMerge(analysisDir, inventory, p));
    }

    public List<PackageInfo> scan(File analysisDir) throws IOException {
        final Map<String, PackageInfo> packageInfoMap = new HashMap<>();

        // generate (complementary) package list from scripts output
        parseDebianPackageList(analysisDir, packageInfoMap);

        // generate package list from files in dir
        packagesFromDocumentationDir(analysisDir, new File(analysisDir, FOLDER_USR_SHARE_DOC), packageInfoMap, true);

        // generate package list from directories in dedicated licenses dir
        packagesFromDocumentationDir(analysisDir, new File(analysisDir, FOLDER_USR_SHARE_LICENSE), packageInfoMap, false);

        // parse / merge package metadata
        parseDebianPackageFiles(analysisDir, packageInfoMap);

        return new ArrayList<>(packageInfoMap.values());
    }

    private void parseDebianPackageFiles(File analysisDir, Map<String, PackageInfo> idToPackageInfoMap) throws IOException {
        final File packageMetaDir = new File(analysisDir, "package-meta");
        if (!packageMetaDir.exists()) return;

        final String[] packageFiles = FileUtils.scanForFiles(packageMetaDir, "**/*_apt.txt", null);

        for (String packageFile : packageFiles) {
            List<String> fileContentLines = FileUtils.readLines(new File(packageMetaDir, packageFile), FileUtils.ENCODING_UTF_8);

            String parsedPackageName = ParsingUtils.getValue(fileContentLines, "Package:");
            String parsedPackageVersion = ParsingUtils.getValue(fileContentLines, "Version:");

            // use the source information (if available) as group
            String parsedPackageGroup = ParsingUtils.getValue(fileContentLines, "Source:");

            String concludedName = parsedPackageName + "-" + parsedPackageVersion;

            PackageInfo packageInfo = idToPackageInfoMap.get(concludedName);
            if (packageInfo == null) {
                packageInfo = new PackageInfo();
                packageInfo.status = "configured";
            }

            packageInfo.id = concludedName;
            packageInfo.name = parsedPackageName;
            packageInfo.version = parsedPackageVersion;

            parseAttributes(packageInfo, fileContentLines);

            registerPackageInfo(packageInfo, idToPackageInfoMap);
        }
    }

    private void parseAttributes(PackageInfo packageInfo, List<String> fileContentLines) {
        packageInfo.url = ParsingUtils.getValue(fileContentLines, "Homepage:");
        packageInfo.description = ParsingUtils.getValue(fileContentLines, "Description:");
        final String source = ParsingUtils.getValue(fileContentLines, "Source:");

        // strip of version information included in the source reference to get a clean package group
        if (source != null) {
            packageInfo.group = source.replaceAll("(.*) \\(.*\\)", "$1");
        }

        String architecture = ParsingUtils.getValue(fileContentLines, "APT-Sources:");
        if (architecture != null) {
            architecture = architecture.replaceAll("(.* )([a-zA-Z0-9]*)( Packages)", "$2");

            // it was observed that sometimes APT-Sources contains a path to a status file. We omit this case.
            if (!architecture.contains("/status")) {
                packageInfo.arch = architecture;
            }
        }

    }

    private static void parseDebianPackageList(File shareDir, Map<String, PackageInfo> idToPackageReferenceMap) throws IOException {
        File dpkgPackagesFile = new File(shareDir, "packages_dpkg.txt");
        String packageFile = FileUtils.readFileToString(dpkgPackagesFile, FileUtils.ENCODING_UTF_8);
        String[] lines = packageFile.split("\\n");

        int i = 1;
        int[] elementIndex = null;
        for (String line : lines) {
            if (line.startsWith(SEPARATOR_3PLUS)) {
                String[] elements = line.split(SEPARATOR_DASH);
                elementIndex = new int[elements.length];
                for (int j = 0; j < elements.length - 1; j++) {
                    elementIndex[j] = elements[j].length() + 1;
                    if (j > 0) elementIndex[j] += elementIndex[j - 1];
                }
            }

            if (line.startsWith(STATUS_INSTALLED) || line.startsWith(STATUS_PREPARED)) {
                String name = line.substring(elementIndex[0], elementIndex[1]).trim();

                if (name.indexOf(SEPARATOR_COLON) > 0) {
                    name = name.substring(0, name.indexOf(SEPARATOR_COLON));
                }

                PackageInfo packageInfo = idToPackageReferenceMap.get(name);
                if (packageInfo == null) {
                    packageInfo = new PackageInfo();
                }

                packageInfo.name = name;
                packageInfo.version = line.substring(elementIndex[1], Math.min(line.length(), elementIndex[2])).trim();
                packageInfo.id = name + "-" + packageInfo.version;

                if (elementIndex.length == 3) {
                    packageInfo.arch = line.substring(elementIndex[2], elementIndex[3]).trim();
                    packageInfo.description = line.substring(elementIndex[3]).trim();
                }
                if (elementIndex.length == 2) {
                    packageInfo.description = line.substring(elementIndex[2]).trim();
                }

                if (line.startsWith(STATUS_INSTALLED)) packageInfo.status = "installed";
                if (line.startsWith(STATUS_PREPARED)) packageInfo.status = "prepared";

                registerPackageInfo(packageInfo, idToPackageReferenceMap);
            }
        }
    }

}
