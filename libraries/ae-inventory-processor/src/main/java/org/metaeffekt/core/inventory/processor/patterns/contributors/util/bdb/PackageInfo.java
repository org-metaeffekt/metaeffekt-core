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

package org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb;

import java.util.ArrayList;
import java.util.List;

public class PackageInfo {
    private Integer epoch;
    private String name;
    private String version;
    private String release;
    private String arch;
    private String sourceRpm;
    private int size;
    private String license;
    private String vendor;
    private String modularityLabel;
    private String summary;
    private String pgp;
    private String sigMD5;
    private DigestAlgorithm digestAlgorithm;
    private int installTime;
    private List<String> baseNames;
    private List<Integer> dirIndexes;
    private List<String> dirNames;
    private List<Integer> fileSizes;
    private List<String> fileDigests;
    private List<Short> fileModes;
    private List<Integer> fileFlags;
    private List<String> userNames;
    private List<String> groupNames;
    private List<String> provides;
    private List<String> requires;
    private String group;
    private String url;
    private String os;
    private String distribution;
    private String distTag;
    private String distUrl;
    private String platform;
    private String nevra;
    private String rpmVersion;

    // Getters and Setters
    public Integer getEpoch() {
        return epoch;
    }

    public void setEpoch(Integer epoch) {
        this.epoch = epoch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public String getSourceRpm() {
        return sourceRpm;
    }

    public void setSourceRpm(String sourceRpm) {
        this.sourceRpm = sourceRpm;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModularityLabel() {
        return modularityLabel;
    }

    public void setModularityLabel(String modularityLabel) {
        this.modularityLabel = modularityLabel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPgp() {
        return pgp;
    }

    public void setPgp(String pgp) {
        this.pgp = pgp;
    }

    public String getSigMD5() {
        return sigMD5;
    }

    public void setSigMD5(String sigMD5) {
        this.sigMD5 = sigMD5;
    }

    public DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(DigestAlgorithm digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public int getInstallTime() {
        return installTime;
    }

    public void setInstallTime(int installTime) {
        this.installTime = installTime;
    }

    public List<String> getBaseNames() {
        return baseNames;
    }

    public void setBaseNames(List<String> baseNames) {
        this.baseNames = baseNames;
    }

    public List<Integer> getDirIndexes() {
        return dirIndexes;
    }

    public void setDirIndexes(List<Integer> dirIndexes) {
        this.dirIndexes = dirIndexes;
    }

    public List<String> getDirNames() {
        return dirNames;
    }

    public void setDirNames(List<String> dirNames) {
        this.dirNames = dirNames;
    }

    public List<Integer> getFileSizes() {
        return fileSizes;
    }

    public void setFileSizes(List<Integer> fileSizes) {
        this.fileSizes = fileSizes;
    }

    public List<String> getFileDigests() {
        return fileDigests;
    }

    public void setFileDigests(List<String> fileDigests) {
        this.fileDigests = fileDigests;
    }

    public List<Short> getFileModes() {
        return fileModes;
    }

    public void setFileModes(List<Short> fileModes) {
        this.fileModes = fileModes;
    }

    public List<Integer> getFileFlags() {
        return fileFlags;
    }

    public void setFileFlags(List<Integer> fileFlags) {
        this.fileFlags = fileFlags;
    }

    public List<String> getUserNames() {
        return userNames;
    }

    public void setUserNames(List<String> userNames) {
        this.userNames = userNames;
    }

    public List<String> getGroupNames() {
        return groupNames;
    }

    public void setGroupNames(List<String> groupNames) {
        this.groupNames = groupNames;
    }

    public List<String> getProvides() {
        return provides;
    }

    public void setProvides(List<String> provides) {
        this.provides = provides;
    }

    public List<String> getRequires() {
        return requires;
    }

    public void setRequires(List<String> requires) {
        this.requires = requires;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOs() {
        return os;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistTag(String distTag) {
        this.distTag = distTag;
    }

    public String getDistTag() {
        return distTag;
    }

    public void setDistUrl(String distUrl) {
        this.distUrl = distUrl;
    }

    public String getDistUrl() {
        return distUrl;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPlatform() {
        return platform;
    }

    public void setNevra(String nevra) {
        this.nevra = nevra;
    }

    public String getNevra() {
        return nevra;
    }

    public void setRpmVersion(String rpmVersion) {
        this.rpmVersion = rpmVersion;
    }

    public String getRpmVersion() {
        return rpmVersion;
    }

    public List<String> installedFileNames() throws Exception {
        if (dirNames == null || dirIndexes == null || baseNames == null) {
            if (dirNames == null && dirIndexes == null && baseNames == null) {
                return null;
            }
            throw new IllegalStateException("Invalid metadata detected for rpm: " + name);
        }

        if (dirNames.isEmpty() || dirIndexes.isEmpty() || baseNames.isEmpty()) {
            if (dirNames.isEmpty() && dirIndexes.isEmpty() && baseNames.isEmpty()) {
                return null;
            }
            throw new IllegalStateException("Invalid metadata detected for rpm: " + name);
        }

        if (dirIndexes.size() != baseNames.size() || dirNames.size() > baseNames.size()) {
            throw new IllegalStateException("Invalid metadata detected for rpm: " + name);
        }

        final List<String> filePaths = new ArrayList<>();
        for (int i = 0; i < baseNames.size(); i++) {
            final int idx = dirIndexes.get(i);
            if (dirNames.size() <= idx) {
                throw new Exception("invalid rpm " + name);
            }
            final String dir = dirNames.get(idx);
            filePaths.add(pathJoin(dir, baseNames.get(i))); // should be slash-separated
        }
        return filePaths;
    }

    public List<FileInfo> installedFiles() throws Exception {
        List<String> fileNames = installedFileNames();
        if (fileNames == null) {
            return null;
        }

        List<FileInfo> files = new ArrayList<>();
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            String digest = i < fileDigests.size() ? fileDigests.get(i) : null;
            Short mode = i < fileModes.size() ? fileModes.get(i) : null;
            Integer size = i < fileSizes.size() ? fileSizes.get(i) : null;
            String username = i < userNames.size() ? userNames.get(i) : null;
            String groupname = i < groupNames.size() ? groupNames.get(i) : null;
            Integer flags = i < fileFlags.size() ? fileFlags.get(i) : null;

            FileInfo record = new FileInfo(fileName, mode, digest, size, username, groupname, new FileFlags(flags));
            files.add(record);
        }

        return files;
    }

    public int epochNum() {
        return epoch == null ? 0 : epoch;
    }

    private String pathJoin(String dir, String baseName) {
        if (dir.endsWith("/")) {
            return dir + baseName;
        } else {
            return dir + "/" + baseName;
        }
    }
}

