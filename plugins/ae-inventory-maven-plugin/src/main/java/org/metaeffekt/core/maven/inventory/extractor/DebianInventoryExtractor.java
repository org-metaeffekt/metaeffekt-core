package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebianInventoryExtractor extends AbstractInventoryExtractor {

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
    public Inventory extractInventory(File analysisDir, String inventoryId) throws IOException {
        Inventory inventory = new Inventory();

        // find packages from the provided inputs
        List<PackageReference> packageReferences = scan(analysisDir);

        packageReferences.forEach(p -> addOrMerge(analysisDir, inventory, inventoryId, p));

        return inventory;
    }

    public static List<PackageReference> scan(File analysisDir) throws IOException {
        Map<String, PackageReference> nameToPackageReferenceMap = new HashMap<>();

        // generate package list from files in dir
        packagesFromDocumentationDir(analysisDir, new File(analysisDir, FOLDER_USR_SHARE_DOC), nameToPackageReferenceMap);

        // generate package list from directories in dedicated licenses dir
        packagesFromDocumentationDir(analysisDir, new File(analysisDir, FOLDER_USR_SHARE_LICENSE), nameToPackageReferenceMap);

        // generate (complementary) package list from scripts output
        parseDebianPackageList(analysisDir, nameToPackageReferenceMap);

        return new ArrayList<>(nameToPackageReferenceMap.values());
    }

    private static void parseDebianPackageList(File shareDir, Map<String, PackageReference> nameToPackageReferenceMap) throws IOException {
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

                PackageReference packageReference = nameToPackageReferenceMap.get(name);
                if (packageReference == null) {
                    packageReference = new PackageReference();
                    nameToPackageReferenceMap.put(name, packageReference);
                }

                packageReference.component = name;
                packageReference.version = line.substring(elementIndex[1], Math.min(line.length(), elementIndex[2])).trim();
                packageReference.id = name + "-" + packageReference.version;

                if (elementIndex.length == 3) {
                    packageReference.arch = line.substring(elementIndex[2], elementIndex[3]).trim();
                    packageReference.description = line.substring(elementIndex[3], line.length()).trim();
                }
                if (elementIndex.length == 2) {
                    packageReference.description = line.substring(elementIndex[2], line.length()).trim();
                }
            }
        }
    }

}
