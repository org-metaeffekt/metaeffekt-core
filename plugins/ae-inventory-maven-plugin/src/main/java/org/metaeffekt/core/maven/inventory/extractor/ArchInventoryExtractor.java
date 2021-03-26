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
    public void extendInventory(File analysisDir, Inventory inventory) throws IOException {
        // find packages from the provided inputs
        List<PackageInfo> packageReferences = scan(analysisDir);
        packageReferences.forEach(p -> addOrMerge(analysisDir, inventory, p));
    }

    @Override
    protected String extractIssue(File analysisDir) throws IOException {
        return null;
    }

    public List<PackageInfo> scan(File analysisDir) throws IOException {
        Map<String, PackageInfo> idToPackageReferenceMap = new HashMap<>();

        // generate (complementary) package list from scripts output
        parseArchPackageList(analysisDir, idToPackageReferenceMap);

        // generate package list from files in dir
        packagesFromDocumentationDir(analysisDir, new File(analysisDir, FOLDER_USR_SHARE_DOC), idToPackageReferenceMap, true);

        // generate package list from directories in dedicated licenses dir
        packagesFromDocumentationDir(analysisDir, new File(analysisDir, FOLDER_USR_SHARE_LICENSE), idToPackageReferenceMap, false);

        parseArchPackages(analysisDir, idToPackageReferenceMap);

        return new ArrayList<>(idToPackageReferenceMap.values());
    }

    private void parseArchPackages(File analysisDir, Map<String,PackageInfo> idToPackageReferenceMap) throws  IOException {
        for (Map.Entry<String, PackageInfo> entry : idToPackageReferenceMap.entrySet()) {
            String name = entry.getKey();
            PackageInfo p = entry.getValue();
            File file = new File(analysisDir, "package-meta/" + name + "_arch.txt");
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

    private void parseArchPackageList(File shareDir, Map<String, PackageInfo> idToPackageReferenceMap) throws IOException {
        File archPackagesFile = new File(shareDir, FILE_PACKAGES_ARCH_TXT);
        String packageFile = FileUtils.readFileToString(archPackagesFile, FileUtils.ENCODING_UTF_8);
        String[] lines = packageFile.split("\\n");

        for (String line : lines) {
            int indexOfSpace = line.indexOf(" ");
            if (indexOfSpace != -1) {
                String name = line.substring(0, indexOfSpace);
                String version = line.substring(indexOfSpace + 1);

                PackageInfo p = idToPackageReferenceMap.get(name);
                if (p == null) {
                    p = new PackageInfo();
                }
                p.id = name + "-" + version;
                p.component = name;
                p.version = version;

                registerPackageInfo(p, idToPackageReferenceMap);
            }
        }
    }

}
