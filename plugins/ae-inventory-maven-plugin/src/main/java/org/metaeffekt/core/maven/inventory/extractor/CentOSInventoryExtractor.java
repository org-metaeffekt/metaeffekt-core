package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CentOSInventoryExtractor extends AbstractInventoryExtractor {

    public static final String FILE_PACKAGES_RPM_TXT = "packages_rpm.txt";

    @Override
    public boolean applies(File analysisDir) {
        return new File(analysisDir, FILE_PACKAGES_RPM_TXT).exists();
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
        parseRpmPackageList(analysisDir, nameToPackageReferenceMap);

        return new ArrayList<>(nameToPackageReferenceMap.values());
    }


    public static void parseRpmPackageList(File shareDir, Map<String, PackageReference> nameToPackageReferenceMap) throws IOException {
        File rpmPackagesFile = new File(shareDir, FILE_PACKAGES_RPM_TXT);
        String packageFileContent = FileUtils.readFileToString(rpmPackagesFile, FileUtils.ENCODING_UTF_8);
        String[] lines = packageFileContent.split("\\n");

        for (String line : lines) {

            String[] elements = line.split("\\|");

            String name = elements[1].trim();
            String version = elements[2].trim();

            PackageReference packageReference = nameToPackageReferenceMap.get(name);

            if (packageReference == null) {
                packageReference = new PackageReference();
                nameToPackageReferenceMap.put(name + "-" + version, packageReference);
            }

            packageReference.component = name;
            packageReference.version = version;
            packageReference.id = name + "-" + version;
            packageReference.license = elements[3].trim();
            if (elements.length > 4)
                packageReference.url = elements[4].trim();
            if (elements.length > 5)
                packageReference.arch = elements[5].trim();
            if (elements.length > 6)
                packageReference.description = elements[6].trim();
        }
    }

}
