package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchInventoryExtractor extends AbstractInventoryExtractor {

    public static final String FILE_PACKAGES_ARCH_TXT = "packages_arch.txt";

    @Override
    public boolean applies(File analysisDir) {
        return new File(analysisDir, FILE_PACKAGES_ARCH_TXT).exists();
    }

    @Override
    public Inventory extractInventory(File analysisDir, String inventoryId) throws IOException {
        Inventory inventory = new Inventory();

        // find packages from the provided inputs
        List<PackageReference> packageReferences = scan(analysisDir);

        packageReferences.forEach(p -> addOrMerge(analysisDir, inventory, inventoryId, p));

        return inventory;
    }

    public List<PackageReference> scan(File analysisDir) throws IOException {
        Map<String, PackageReference> nameToPackageReferenceMap = new HashMap<>();

        // generate package list from files in dir
        packagesFromDocumentationDir(analysisDir, new File(analysisDir, FOLDER_USR_SHARE_DOC), nameToPackageReferenceMap);

        // generate package list from directories in dedicated licenses dir
        packagesFromDocumentationDir(analysisDir, new File(analysisDir, FOLDER_USR_SHARE_LICENSE), nameToPackageReferenceMap);

        // generate (complementary) package list from scripts output
        parseArchPackageList(analysisDir, nameToPackageReferenceMap);

        parseArchPackages(analysisDir, nameToPackageReferenceMap);

        return new ArrayList<>(nameToPackageReferenceMap.values());
    }

    private void parseArchPackages(File analysisDir, Map<String,PackageReference> nameToPackageReferenceMap) throws  IOException {
        for (Map.Entry<String, PackageReference> entry : nameToPackageReferenceMap.entrySet()) {
            String name = entry.getKey();
            PackageReference p = entry.getValue();
            File file = new File(analysisDir, "packages/" + name + "_arch.txt");
            if (file.exists()) {
                List<String> content = FileUtils.readLines(file, FileUtils.ENCODING_UTF_8);
                for (String line : content) {
                    int colonIndex = line.indexOf(":");
                    if (colonIndex > 0) {
                        String trim = line.substring(colonIndex + 1).trim();
                        if (line.startsWith("Licenses        :")) {
                            p.license = trim;
                        }
                        if (line.startsWith("Description     :")) {
                            p.description = trim;
                        }
                        if (line.startsWith("Architecture    :")) {
                            p.arch = trim;
                        }
                        if (line.startsWith("URL             :")) {
                            p.url = trim;
                        }
                    }
                }
            }
        }
    }

    private void parseArchPackageList(File shareDir, Map<String, PackageReference> nameToPackageReferenceMap) throws IOException {
        File archPackagesFile = new File(shareDir, FILE_PACKAGES_ARCH_TXT);
        String packageFile = FileUtils.readFileToString(archPackagesFile, FileUtils.ENCODING_UTF_8);
        String[] lines = packageFile.split("\\n");

        for (String line : lines) {
            int indexOfSpace = line.indexOf(" ");
            if (indexOfSpace != -1) {
                String name = line.substring(0, indexOfSpace);
                String version = line.substring(indexOfSpace + 1);

                PackageReference p = nameToPackageReferenceMap.get(name);
                if (p == null) {
                    p = new PackageReference();
                    nameToPackageReferenceMap.put(name, p);
                }
                p.id = name + "-" + version;
                p.component = name;
                p.version = version;
            }
        }
    }

}
