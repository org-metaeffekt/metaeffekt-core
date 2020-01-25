package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.util.FileUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class InventoryExtractorUtil {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    private InventoryExtractorUtil() {};

    /**
     * Filters the file list and outputs a list of files that is not covered by the packages.
     *
     * @param analysisDir The analysis base dir.
     * @param excludePatterns Additional exclude patterns.
     *
     * @return List of files not covered by package file lists or exclude patterns.
     * @throws IOException
     */
    public static final List<String> filterFileList(File analysisDir, List<String> excludePatterns) throws IOException {
        File files = new File(analysisDir, "files.txt");
        File filesDir = new File(analysisDir, "files");

        String[] packageFiles = FileUtils.scanForFiles(filesDir, "**/*_files.txt", "--nothing--");
        List<String> fileList = FileUtils.readLines(files, FileUtils.ENCODING_UTF_8);
        for (String file : new ArrayList<>(fileList)) {
            for (String pattern : excludePatterns) {
                if (ANT_PATH_MATCHER.match(pattern, file)) {
                    fileList.remove(file);
                }
            }
        }

        for (String singlePackageFile : packageFiles) {
            List<String> packageFileList = FileUtils.readLines(new File(filesDir, singlePackageFile), FileUtils.ENCODING_UTF_8);
            for (String file : packageFileList) {
                // skip artifacts
                if (StringUtils.isEmpty(file)) continue;
                if (file.endsWith(" contains:")) continue;

                // convert to match requirements for matching in file list
                String convertedFile = file;
                if (!convertedFile.startsWith("/")) {
                    convertedFile = "/" + convertedFile;
                }
                boolean removed = fileList.remove(convertedFile);
                if (!removed) {
                    // TODO: log
                    // getLog().debug("File specified in package file list not matched: " + convertedFile);
                }
            }
        }
        return fileList;
    }

}
