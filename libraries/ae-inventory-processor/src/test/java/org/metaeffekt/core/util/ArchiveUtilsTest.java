/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.util;

import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ArchiveUtilsTest {

    @Ignore
    @Test
    public void testUnpackIfPossible() {
        final File inputBaseDir = new File("<path-to-scan-folder>");

        final String[] files = FileUtils.scanDirectoryForFiles(inputBaseDir, "**/*");

        for (String file : files) {
            final File inputFile = new File(inputBaseDir, file);

            if (inputFile.isDirectory()) continue;

            final File targetFolder = new File(inputFile.getParentFile(), "[" + inputFile.getName() + "]");

            ArchiveUtils.unpackIfPossible(inputFile, targetFolder, new ArrayList<>());
        }
    }

    @Ignore
    @Test
    public void testMsiUnpack() throws IOException, InterruptedException {
        final File inputBaseDir = new File("<path-to-scan-folder>");

        final String[] msiFiles = FileUtils.scanDirectoryForFiles(inputBaseDir, "**/*");

        String sevenZZ = "<path-to-7zz>/7zz";

        for (String msiFile : msiFiles) {

            final File inputFile = new File(inputBaseDir, msiFile);

            if (inputFile.isDirectory()) continue;

            final File targetFolder = new File(inputFile.getParentFile(), "[" + inputFile.getName() + "]");

            FileUtils.forceMkdir(targetFolder);


            List<String> commandParts = new ArrayList<>();
            commandParts.add(sevenZZ);
            commandParts.add("x");
            commandParts.add(inputFile.getAbsolutePath());
            commandParts.add("-o" + targetFolder.getAbsolutePath());
            commandParts.add("-y");
            final String command = commandParts.stream().collect(Collectors.joining(" "));

            ExecUtils.ExecParam execParam = new ExecUtils.ExecParam(commandParts);
            execParam.retainErrorOutputs();
            execParam.setWorkingDir(targetFolder);

            ExecUtils.executeAndThrowIOExceptionOnFailure(execParam);

            if (targetFolder.listFiles().length == 0) {
                FileUtils.deleteDirectoryQuietly(targetFolder);
            }

        }
    }

    @Test
    public void buildInventoryFromVersionFiles_WindowsCab() throws IOException {
        final File inputBaseDir = new File("<path-to-scan-folder>");
        final File targetFile = new File("target/scan-versions.xls");

        final Inventory inventory = deriveInventory(inputBaseDir);

        new InventoryWriter().writeInventory(inventory, targetFile);
    }

    private Inventory deriveInventory(File inputBaseDir) throws IOException {
        final String[] versionFiles = FileUtils.scanForFiles(inputBaseDir, "**/version.txt,**/Version.txt", null);

        final Inventory inventory = new Inventory();

        for (String versionFile : versionFiles) {

            final String content = FileUtils.readFileToString(new File(inputBaseDir, versionFile), "UTF-16LE");

            final String[] lines = content.split("[\n|\r]+");

            Map<String, String> keyValueMap = new HashMap<>();

            Pattern pattern = Pattern.compile("(?m)^VALUE \"(.*?)\",\\s*\"(.*?)\"$");

            for (String line : lines) {
                line = line.trim();

                final Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    final String key = matcher.replaceAll("$1");
                    final String value = matcher.replaceAll("$2");
                    keyValueMap.put(key, value);
                }
            }

            Artifact artifact = new Artifact();
            artifact.setId(keyValueMap.get("OriginalFilename"));
            artifact.setComponent(keyValueMap.get("ProductName"));
            artifact.set("Package Specified Copyright", keyValueMap.get("LegalCopyright"));
            artifact.set("Package Specified Trademarks", keyValueMap.get("LegalTrademarks"));
            artifact.setVersion(keyValueMap.get("FileVersion"));
            artifact.set("Product Version", keyValueMap.get("ProductVersion"));
            artifact.set("Assembly Version", keyValueMap.get("Assembly Version"));
            artifact.set("Package Specified Build Number", keyValueMap.get("Internal Build Number"));
            artifact.set("Package Specified Name", keyValueMap.get("InternalName"));
            artifact.set(Constants.KEY_DESCRIPTION, keyValueMap.get("FileDescription"));
            artifact.set(Constants.KEY_ORGANIZATION, keyValueMap.get("CompanyName"));

            keyValueMap.remove("OriginalFilename");
            keyValueMap.remove("ProductName");
            keyValueMap.remove("LegalCopyright");
            keyValueMap.remove("FileVersion");
            keyValueMap.remove("Assembly Version");
            keyValueMap.remove("ProductVersion");
            keyValueMap.remove("FileDescription");
            keyValueMap.remove("CompanyName");

            keyValueMap.remove("Comments");
            keyValueMap.remove("LegalTrademarks");
            keyValueMap.remove("InternalName");
            keyValueMap.remove("Internal Build Number");

            keyValueMap.entrySet().stream().forEach(System.out::println);

            inventory.getArtifacts().add(artifact);

            System.out.println(artifact.createCompareStringRepresentation());
        }

        inventory.mergeDuplicates();
        return inventory;
    }

}