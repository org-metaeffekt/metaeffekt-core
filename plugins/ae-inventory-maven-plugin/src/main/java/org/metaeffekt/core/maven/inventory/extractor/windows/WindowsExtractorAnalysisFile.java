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
package org.metaeffekt.core.maven.inventory.extractor.windows;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public enum WindowsExtractorAnalysisFile {
    Class_CIM_SoftwareElement("CIM_SoftwareElement", "json", true),
    Class_Win32_BaseBoard("Win32_BaseBoard", "json", true),
    Class_Win32_MotherboardDevice("Win32_MotherboardDevice", "json", true),
    Class_Win32_Bios("Win32_Bios", "json", true),
    Class_Win32_CDROMDrive("Win32_CDROMDrive", "json", true),
    Class_Win32_ComputerSystem("Win32_ComputerSystem", "json", true),
    Class_Win32_ComputerSystemProduct("Win32_ComputerSystemProduct", "json", true),
    Class_Win32_DiskDrive("Win32_DiskDrive", "json", true),
    Class_Win32_DiskPartition("Win32_DiskPartition", "json", true),
    Class_Win32_DisplayConfiguration("Win32_DisplayConfiguration", "json", true),
    Class_Win32_InstalledProgramFramework("Win32_InstalledProgramFramework", "json", true),
    Class_Win32_InstalledStoreProgram("Win32_InstalledStoreProgram", "json", true),
    Class_Win32_InstalledWin32Program("Win32_InstalledWin32Program", "json", true),
    Class_Win32_LogicalDisk("Win32_LogicalDisk", "json", true),
    Class_Win32_NetworkAdapter("Win32_NetworkAdapter", "json", true),
    Class_Win32_NetworkAdapterConfiguration("Win32_NetworkAdapterConfiguration", "json", true),
    Class_Win32_NetworkLoginProfile("Win32_NetworkLoginProfile", "json", true),
    Class_Win32_NetworkProtocol("Win32_NetworkProtocol", "json", true),
    Class_Win32_OperatingSystem("Win32_OperatingSystem", "json", true),
    Class_Win32_OptionalFeature("Win32_OptionalFeature", "json", true),
    Class_Win32_PhysicalMemory("Win32_PhysicalMemory", "json", true),
    Class_Win32_PnPEntity("Win32_PnPEntity", "json", true),
    Class_Win32_PnpSignedDriver("Win32_PnpSignedDriver", "json", true),
    Class_Win32_Printer("Win32_Printer", "json", true),
    Class_Win32_PrinterDriver("Win32_PrinterDriver", "json", true),
    Class_Win32_Processor("Win32_Processor", "json", true),
    Class_Win32_Product("Win32_Product", "json", true),
    Class_Win32_Service("Win32_Service", "json", true),
    Class_Win32_SoftwareElement("Win32_SoftwareElement", "json", true),
    Class_Win32_SoftwareElementCondition("Win32_SoftwareElementCondition", "json", true),
    Class_Win32_SoftwareFeature("Win32_SoftwareFeature", "json", true),
    Class_Win32_SoftwareFeatureCheck("Win32_SoftwareFeatureCheck", "json", true),
    Class_Win32_SoftwareFeatureParent("Win32_SoftwareFeatureParent", "json", true),
    Class_Win32_SoftwareFeatureSoftwareElements("Win32_SoftwareFeatureSoftwareElements", "json", true),
    Class_Win32_SoundDevice("Win32_SoundDevice", "json", true),
    Class_Win32_SystemDriver("Win32_SystemDriver", "json", true),
    Class_Win32_USBController("Win32_USBController", "json", true),
    Class_Win32_VideoController("Win32_VideoController", "json", true),

    Get_PnpDevice("Get-PnpDevice", "json", false),

    FileSymlinksList("FileSymlinksList", "txt", false),
    FileSystemDirsList("FileSystemDirsList", "txt", false),
    FileSystemFilesList("FileSystemFilesList", "txt", false),

    /**
     * @deprecated was split up into multiple files, use the dedicated files instead
     */
    @Deprecated
    RegistrySubtree("RegistrySubtree", "json", false),
    RegistrySubtree_WindowsUninstall("RegistrySubtree-windows-uninstall", "json", false),

    OSversion("OSversion", "txt", false),
    systeminfo("systeminfo", "json", false),
    Get_ComputerInfo("Get-ComputerInfo", "json", false),
    ;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsExtractorAnalysisFile.class);

    private final String typeName;
    private final String fileType;
    private final boolean isWmiClass;

    WindowsExtractorAnalysisFile(String typeName, String fileType, boolean isWmiClass) {
        this.typeName = typeName;
        this.fileType = fileType;
        this.isWmiClass = isWmiClass;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getFileType() {
        return fileType;
    }

    public boolean isWmiClass() {
        return isWmiClass;
    }

    public File getFile(File baseDir) {
        return new File(baseDir, getTypeName() + "." + getFileType());
    }

    public File getExistingFile(File baseDir) {
        final File file = getFile(baseDir);
        if (!file.exists()) return null;
        return file;
    }

    public JSONObject readJsonObject(File baseDir) throws IOException {
        final File jsonFile = getFile(baseDir);
        if (!jsonFile.exists()) return null;
        return performParsingStrategyString(jsonFile, source -> {
            final String processed = source
                    .replace("�", "") // U+00EF U+00BF U+00BD
                    .replace("\uFEFF", "") // U+FEFF
                    .replace("\u0000", "") // U+0000
                    .trim();
            if (processed.length() <= 1) {
                return null;
            }
            return new JSONObject(processed);
        });
    }

    public JSONObject readJsonObjectOrEmpty(File baseDir) throws IOException {
        return Optional.ofNullable(readJsonObject(baseDir)).orElse(new JSONObject());
    }

    public JSONArray readJsonArray(File baseDir) throws IOException {
        final File classFile = getFile(baseDir);
        if (!classFile.exists()) return null;
        return performParsingStrategyString(classFile, source -> {
            final String processed = source
                    .replace("�", "") // U+00EF U+00BF U+00BD
                    .replace("\uFEFF", "") // U+FEFF
                    .replace("\u0000", "") // U+0000
                    .trim();
            if (processed.length() <= 1) {
                return null;
            }
            return new JSONArray(processed);
        });
    }

    public JSONArray readJsonArrayOrEmpty(File baseDir) throws IOException {
        return Optional.ofNullable(readJsonArray(baseDir)).orElse(new JSONArray());
    }

    public List<String> readLines(File baseDir) throws IOException {
        final File classFile = getFile(baseDir);
        if (!classFile.exists()) return null;
        return performParsingStrategyStringList(classFile, Function.identity());
    }

    public List<String> readLinesOrEmpty(File baseDir) throws IOException {
        return Optional.ofNullable(readLines(baseDir)).orElse(new ArrayList<>());
    }

    private <T> T performParsingStrategyString(File file, Function<String, T> conversionFunction) throws IOException {
        Exception lastException = null;

        for (Charset charset : charsets) {
            try {
                final String content = FileUtils.readFileToString(file, charset);
                return conversionFunction.apply(content);
            } catch (Exception e) {
                LOG.debug("Failed to parse file {} using charset {}", file, charset, e);
                lastException = e;
            }
        }

        if (lastException != null) {
            throw new IOException("Failed to parse file using any of the supported charsets " + Arrays.toString(charsets) + ": " + file + "\n" + lastException.getMessage(), lastException);
        } else {
            throw new IOException("Failed to parse file using any of the supported charsets " + Arrays.toString(charsets) + ": " + file);
        }
    }

    private <T> T performParsingStrategyStringList(File file, Function<List<String>, T> conversionFunction) throws IOException {
        Exception lastException = null;

        for (Charset charset : charsets) {
            try {
                final List<String> content = FileUtils.readLines(file, charset);
                return conversionFunction.apply(content);
            } catch (Exception e) {
                LOG.debug("Failed to parse file {} using charset {}", file, charset, e);
                lastException = e;
            }
        }

        if (lastException != null) {
            throw new IOException("Failed to parse file using any of the supported charsets " + Arrays.toString(charsets) + ": " + file + "\n" + lastException.getMessage(), lastException);
        } else {
            throw new IOException("Failed to parse file using any of the supported charsets " + Arrays.toString(charsets) + ": " + file);
        }
    }

    private final Charset[] charsets = new Charset[]{
            StandardCharsets.UTF_8,
            StandardCharsets.UTF_16LE,
            StandardCharsets.ISO_8859_1
    };
}
