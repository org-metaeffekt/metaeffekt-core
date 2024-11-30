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
package org.metaeffekt.core.inventory.processor.linux;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinuxDistributionUtil {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxDistributionUtil.class);

    private static final Pattern CPE_PATTERN = Pattern.compile("cpe:/o:(.*?):.*?:(.*?):.*?$");

    public static final String ISSUE = "issue";
    public static final String ISSUE_NET = "issue.net";

    public static final String OS_RELEASE = "os-release";
    public static final String LSB_RELEASE = "lsb-release";
    public static final String REDHAT_RELEASE = "redhat-release";
    public static final String CENTOS_RELEASE = "centos-release";
    public static final String SYSTEM_RELEASE = "system-release";
    public static final String FEDORA_RELEASE = "fedora-release";
    public static final String ALMALINUX_RELEASE = "almalinux-release";
    public static final String ALPINE_RELEASE = "alpine-release";

    public static final String PFSENSE_VERSION = "version";
    public static final String DEBIAN_VERSION_DASH = "debian-version";
    public static final String DEBIAN_VERSION = "debian_version";
    public static final String SYSTEM_RELEASE_CPE = "system-release-cpe";

    /**
     * List all potential paths providing details on the linux distribution
     */
    public static final String[] CONTEXT_PATHS = new String[] {
            "/etc/" + ISSUE, // consumed
            "/etc/" + ISSUE_NET, // open

            "/etc/" + OS_RELEASE, // consumed
            "/usr/lib/" + OS_RELEASE, // consumed

            "/etc/" + SYSTEM_RELEASE_CPE, // open
            "/etc/" + REDHAT_RELEASE, // contains name + version + code name; open

            "/etc/" + LSB_RELEASE, // properties; open
            "/etc/" + CENTOS_RELEASE, // contains name + version + code name; open
            "/etc/" + ALMALINUX_RELEASE, // expected plain version
            "/etc/" + ALPINE_RELEASE, // plain version; consumed

            "/etc/" + DEBIAN_VERSION, // plain version; consumed
            "/etc/" + DEBIAN_VERSION_DASH, // regarded potential variant; plain version; comsumed
            "/etc/" + PFSENSE_VERSION, // regarded plain version; comsumed

            // generic catch-all for some other distributions
            "/etc/" + SYSTEM_RELEASE // open
    };

    public static class LinuxDistro {

        public String issue;
        public String version;

        public String id;
        public String versionId;

        public String cpe;

        public String url;

        public String release;

        public String vendor;

        /**
         * The map enables to collect further details available for post-processing.
         */
        public Map<String, Properties> filePropertiesMap = new HashMap<>();
    }

    public static String parseDebianBasedDistro(String baseDir, String virtualRootPath) {
        List<Path> paths = new ArrayList<>(Arrays.asList(
                Paths.get(baseDir + "/" + virtualRootPath + "/etc/" + OS_RELEASE),
                Paths.get(baseDir + "/" + virtualRootPath + "/etc/" + LSB_RELEASE),
                Paths.get(baseDir + "/" + virtualRootPath + "/etc/" + DEBIAN_VERSION),
                Paths.get(baseDir + "/" + virtualRootPath + "/etc/" + DEBIAN_VERSION_DASH),
                Paths.get(baseDir + "/" + virtualRootPath + "/etc/" + REDHAT_RELEASE),
                Paths.get(baseDir + "/" + virtualRootPath + "/etc/" + CENTOS_RELEASE),

                // generic catch-all for some other distributions
                Paths.get(baseDir + "/" + virtualRootPath + "/etc/" + SYSTEM_RELEASE)
        ));

        for (Path path : paths) {
            try {
                if (Files.exists(path) && Files.size(path) > 0) {
                    List<String> lines = Files.readAllLines(path);
                    for (String line : lines) {
                        if (path.endsWith(OS_RELEASE) ||
                                path.endsWith(LSB_RELEASE)) {
                            // parse key-value pair files
                            if (line.contains("=")) {
                                String[] parts = line.split("=", 2);
                                String key = parts[0];
                                String value = parts[1].replace("\"", "").trim();
                                if ("ID".equals(key) || "DISTRIB_ID".equals(key)) {
                                    return value;
                                }
                            }
                        } else if (path.endsWith(DEBIAN_VERSION_DASH)) {
                            // directly return "debian" if the file exists, assuming the file content isn't needed
                            return "debian";
                        } else if (path.endsWith(DEBIAN_VERSION)) {
                            // directly return "debian" if the file exists, assuming the file content isn't needed
                            return "debian";
                        } else if (path.endsWith(REDHAT_RELEASE) ||
                                path.endsWith(CENTOS_RELEASE) ||
                                path.endsWith(SYSTEM_RELEASE) ||
                                path.endsWith(FEDORA_RELEASE)) {
                            return line.trim().split(" ")[0].toLowerCase();
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug("Exception tying to parse distro in file [{}].", path);
            }
        }
        return null;
    }

    public static LinuxDistro parseDistro(File distroBaseDir) {

        final File challengeBaseDir = new File(distroBaseDir, "etc");
        if (!challengeBaseDir.exists()) {
            LOG.warn("Received base directory for detecting linux distro does not have expected structure: " + distroBaseDir.getAbsolutePath());
        }

        LinuxDistro linuxDistro = new LinuxDistro();

        // parsing is done incrementally (information is distributed and potentially not available everywhere)
        // later parsing steps can enhance / overwrite previous consolidating the information

        parseDebianVersion(distroBaseDir, linuxDistro);
        parseAlpineRelease(distroBaseDir, linuxDistro);
        parseAlmaLinuxRelease(distroBaseDir, linuxDistro);
        parsePfSenseVersion(distroBaseDir, linuxDistro);

        parseUsrBinOsRelease(distroBaseDir, linuxDistro);
        parseEtcOsRelease(distroBaseDir, linuxDistro);

        parseIssue(distroBaseDir, linuxDistro);

        parseSystemReleaseCpe(distroBaseDir, linuxDistro);

        // fallback option in case version id not available yet
        if (linuxDistro.version == null) {
            Properties properties = linuxDistro.filePropertiesMap.get("usr/lib/" + OS_RELEASE);
            if (properties == null) {
                properties = linuxDistro.filePropertiesMap.get("etc/" + OS_RELEASE);
            }
            if (properties != null) {
                String prettyVersion = properties.getProperty("PRETTY_NAME");
                prettyVersion = prettyVersion.replace(linuxDistro.issue, "");
                prettyVersion = prettyVersion.replace(" v", "");
                linuxDistro.version = modulateValue(prettyVersion, linuxDistro.version);
            }
        }

        parseRedHatRelease(distroBaseDir, linuxDistro);

        if (linuxDistro.cpe != null) {
            if (linuxDistro.id == null) {
                parseSystemReleaseCpe(linuxDistro);
            }
        }

        if (linuxDistro.id != null) {
            if ("rhel".equals(linuxDistro.id)) {
                linuxDistro.vendor = "redhat";
            } else {
                linuxDistro.vendor = linuxDistro.id;
            }
        }

        if (linuxDistro.issue != null) {
            return linuxDistro;
        }

        return null;
    }

    private static void parseDebianVersion(File distroBaseDir, LinuxDistro linuxDistro) {
        File file = new File(distroBaseDir, "etc/" + DEBIAN_VERSION);
        if (!file.exists()) {
            file = new File(distroBaseDir, "etc/" + DEBIAN_VERSION_DASH);
        }
        parsePlainVersionFile(linuxDistro, file);
    }

    private static void parseAlpineRelease(File distroBaseDir, LinuxDistro linuxDistro) {
        parsePlainVersionFile(linuxDistro, new File(distroBaseDir, "etc/" + ALPINE_RELEASE));
    }

    private static void parseAlmaLinuxRelease(File distroBaseDir, LinuxDistro linuxDistro) {
        parsePlainVersionFile(linuxDistro, new File(distroBaseDir, "etc/" + ALMALINUX_RELEASE));
    }

    private static void parsePfSenseVersion(File distroBaseDir, LinuxDistro linuxDistro) {
        parsePlainVersionFile(linuxDistro, new File(distroBaseDir, "etc/" + PFSENSE_VERSION));
    }

    private static void parseRedHatRelease(File file, LinuxDistro linuxDistro) {
        Pattern pattern = Pattern.compile("(.*?)\\srelease\\s(\\d\\.\\d+)");
        try (Stream<String> lines = Files.lines(new File(file, "etc/" + REDHAT_RELEASE).toPath())) {
            for (String line : lines.collect(Collectors.toList())) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    if (matcher.groupCount() >= 2) {
                        if (linuxDistro.id == null) linuxDistro.id = matcher.group(1);
                        if (linuxDistro.version == null) linuxDistro.version = matcher.group(2);
                        if (linuxDistro.versionId == null) linuxDistro.versionId = matcher.group(2);
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Cannot parse [{}].", file.getAbsolutePath());
        }
    }

    private static void parsePlainVersionFile(LinuxDistro linuxDistro, File file) {
        try {
            if (file.exists()) {
                String version = FileUtils.readFileToString(file, FileUtils.ENCODING_UTF_8).trim();
                if (!version.isEmpty()) {
                    linuxDistro.version = version;
                }
            }
        } catch (Exception e) {
            LOG.debug("Cannot parse [{}].", file.getAbsolutePath());
        }
    }

    private static void parseIssue(File distroBaseDir, LinuxDistro linuxDistro) {
        final File issueFile = new File(distroBaseDir, "etc/issue");
        try {
            if (issueFile.exists()) {
                final String issue = FileUtils.readFileToString(issueFile, "UTF-8");

                String issueExtract = issue.replace("Welcome to ", "");
                issueExtract = issueExtract.replace("Kernel \\r on an \\m (\\l)", "");
                issueExtract = issueExtract.replace("\\S\nKernel \\r on an \\m", "");
                issueExtract = issueExtract.replace("Kernel \\r on an \\m", ""); // this line exists in the issue file in centos 6.9
                issueExtract = issueExtract.replace(" \\n \\l", "");
                issueExtract = issueExtract.replace(" - Kernel %r (%t).", "");
                issueExtract = issueExtract.trim();

                linuxDistro.issue = modulateValue(issueExtract, linuxDistro.issue);
            }
        } catch (Exception e) {
            LOG.debug("Cannot parse [{}].", issueFile.getAbsolutePath());
        }
    }

    public static boolean applies(String pathInContext) {
        pathInContext= "/" + pathInContext;
        for (String contextPath : CONTEXT_PATHS) {
            if (pathInContext.endsWith(contextPath)) {
                return true;
            }
        }
        return false;
    }

    public static String[] getContextPaths() {
        return CONTEXT_PATHS;
    }

    private static void parseUsrBinOsRelease(File distroBaseDir, LinuxDistro linuxDistro) {
        parseOsRelease(distroBaseDir, linuxDistro, "usr/lib/" + OS_RELEASE);
    }

    private static void parseEtcOsRelease(File distroBaseDir, LinuxDistro linuxDistro) {
        parseOsRelease(distroBaseDir, linuxDistro, "etc/" + OS_RELEASE);
    }

    private static void parseOsRelease(File distroBaseDir, LinuxDistro linuxDistro, String path) {
        final File osReleaseFile = new File(distroBaseDir, path);

        if (osReleaseFile.exists()) {
            final Properties properties = PropertiesUtils.loadPropertiesFile(osReleaseFile);
            linuxDistro.issue = modulateValue(properties.getProperty("NAME"), linuxDistro.issue);
            linuxDistro.version = modulateValue(properties.getProperty("VERSION"), linuxDistro.version);

            linuxDistro.id = modulateValue(properties.getProperty("ID"), linuxDistro.id);
            linuxDistro.versionId = modulateValue(properties.getProperty("VERSION_ID"), linuxDistro.versionId);

            linuxDistro.cpe = modulateValue(properties.getProperty("CPE_NAME"), linuxDistro.cpe);
            linuxDistro.url = modulateValue(properties.getProperty("HOME_URL"), linuxDistro.url);

            linuxDistro.filePropertiesMap.put(path, properties);
        }
    }

    private static String modulateValue(String name, String currentValue) {
        if (currentValue == null) {
            if (name != null && !StringUtils.isEmpty(name)) {
                return name.replaceAll("\"", "");
            }
        }
        return currentValue;
    }

    private static void parseSystemReleaseCpe(File distroBaseDir, LinuxDistro linuxDistro) {
        final File file = new File(distroBaseDir, "etc/" + SYSTEM_RELEASE_CPE);
        linuxDistro.cpe = modulateValue(parsePlainFile(file), linuxDistro.cpe);
    }

    private static String parsePlainFile(File file) {
        if (file.exists()) {
            try {
                String content = FileUtils.readFileToString(file, FileUtils.ENCODING_UTF_8).trim();
                if (!content.isEmpty()) {
                    return content;
                }
            } catch (Exception e) {
                LOG.debug("Cannot parse [{}].", file.getAbsolutePath());
            }
        }
        return null;
    }

    private static void parseSystemReleaseCpe(LinuxDistro linuxDistro) {
        final Matcher matcher = CPE_PATTERN.matcher(linuxDistro.cpe);
        if (matcher.find()) {
            if (matcher.find()) {
                linuxDistro.id = modulateValue(matcher.group(1), linuxDistro.id);
            }
        }
    }

}
