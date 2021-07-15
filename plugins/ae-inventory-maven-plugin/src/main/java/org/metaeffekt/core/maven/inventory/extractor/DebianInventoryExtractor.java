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
        List<PackageInfo> packageReferences = scan(analysisDir);

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

        // add additional details from package metadata files
        parseDebianPackageFiles(analysisDir, packageInfoMap);

        return new ArrayList<>(packageInfoMap.values());
    }

    private void parseDebianPackageFiles(File analysisDir, Map<String, PackageInfo> idToPackageReferenceMap) throws IOException {
        for (Map.Entry<String, PackageInfo> entry : idToPackageReferenceMap.entrySet()) {
            PackageInfo packageInfo = entry.getValue();
            String packageId = packageInfo.name;
            File packageFile = new File(analysisDir, "package-meta/" + packageId + "_apt.txt");
            if (packageFile.exists()) {
                List<String> fileContentLines = FileUtils.readLines(packageFile, FileUtils.ENCODING_UTF_8);

                packageInfo.url = ParsingUtils.getValue(fileContentLines, "Homepage:");
                packageInfo.description = ParsingUtils.getValue(fileContentLines, "Description:");
                packageInfo.group = ParsingUtils.getValue(fileContentLines, "Source:");
            } else {
                LOG.info("File {} does not exist.", packageFile);
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
                packageInfo.component = name;
                packageInfo.version = line.substring(elementIndex[1], Math.min(line.length(), elementIndex[2])).trim();
                packageInfo.id = name + "-" + packageInfo.version;

                if (elementIndex.length == 3) {
                    packageInfo.arch = line.substring(elementIndex[2], elementIndex[3]).trim();
                    packageInfo.description = line.substring(elementIndex[3], line.length()).trim();
                }
                if (elementIndex.length == 2) {
                    packageInfo.description = line.substring(elementIndex[2], line.length()).trim();
                }

                registerPackageInfo(packageInfo, idToPackageReferenceMap);
            }
        }
    }

}
