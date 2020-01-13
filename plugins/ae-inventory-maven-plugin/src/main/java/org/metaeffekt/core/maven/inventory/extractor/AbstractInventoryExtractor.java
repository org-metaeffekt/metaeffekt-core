package org.metaeffekt.core.maven.inventory.extractor;

import org.apache.tools.ant.DirectoryScanner;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * The {@link org.metaeffekt.core.maven.inventory.mojo.ContainerInventoryExtractionMojo} expects
 * information of an appliance or container being available in an analysis folder. The
 * content depends on the distribution an the extractor scripts.<br/>
 * Different implementations of {@link AbstractInventoryExtractor} support the different outputs.
 */
public abstract class AbstractInventoryExtractor implements InventoryExtractor {

    public static final String FOLDER_USR_SHARE_DOC = "usr-share-doc";
    public static final String FOLDER_USR_SHARE_LICENSE = "usr-share-license";

    /**
     * Anticipates a directory for each package in packagesDocDir. The directory contains the
     * package name only (no other attribute is derived).
     *
     * @param analysisDir The analysisDir.
     * @param packagesDocDir The specific (one out of potentially many) packageDocDir.
     * @param nameToPackageReferenceMap The resulting {@link PackageReference} instances are added to the map.
     */
    protected static void packagesFromDocumentationDir(File analysisDir, File packagesDocDir, Map<String, PackageReference> nameToPackageReferenceMap) {
        if (packagesDocDir.exists()) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(packagesDocDir);
            scanner.setIncludes(new String[]{"*"});
            scanner.scan();
            for (String path : scanner.getIncludedDirectories()) {
                PackageReference packageReference = new PackageReference();

                // here no version is added, since such information is not available
                // currently the whole process is only for validating that there is no
                // extra content specified that is not available from the package manager.
                packageReference.id = path;
                packageReference.component = path;
                nameToPackageReferenceMap.put(path, packageReference);
            }
        }
    }

    protected void addOrMerge(File analysisDir, Inventory inventory, String inventoryId, PackageReference p) {
        Artifact derivedFromPackage = p.createArtifact(analysisDir);
        Artifact referenceArtifact = inventory.findArtifact(derivedFromPackage.getId());
        if (referenceArtifact != null && derivedFromPackage.getVersion().equalsIgnoreCase(referenceArtifact.getVersion())) {
            // already added --> merge
            referenceArtifact.merge(derivedFromPackage);
        } else {
            derivedFromPackage.set(KEY_ATTRIBUTE_SOURCE_PROJECT, inventoryId);
            derivedFromPackage.set(KEY_ATTRIBUTE_TYPE, TYPE_PACKAGE);
            inventory.getArtifacts().add(derivedFromPackage);
        }
    }

    @Override
    public void validate(File analysisDir) throws IllegalStateException {
        // here the common aspects are validated
        validateFileHasContent(new File(analysisDir, "files.txt"));
        validateFileHasContent(new File(analysisDir, "issue.txt"));
        validateFileExists(new File(analysisDir, "release.txt"));
        validateFileHasContent(new File(analysisDir, "uname.txt"));
    }

    private void validateFileExists(File file) {
        if (!file.exists()) {
            throw new IllegalStateException(
                String.format("File %s expected, but does not exists.", file.getPath()));
        }
    }

    private void validateFileHasContent(File file) {
        validateFileExists(file);
        try {
            String content = FileUtils.readFileToString(file, FileUtils.ENCODING_UTF_8);
            if (content == null || content.trim().length() == 0) {
                throw new IllegalStateException(
                    String.format("File %s does not contain any content.", file.getPath()));
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Unable to reed file %s.", file.getPath()));
        }
    }
}
