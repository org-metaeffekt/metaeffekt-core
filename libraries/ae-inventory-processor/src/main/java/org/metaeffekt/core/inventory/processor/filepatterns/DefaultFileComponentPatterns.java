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
package org.metaeffekt.core.inventory.processor.filepatterns;

import java.util.ArrayList;
import java.util.List;

public class DefaultFileComponentPatterns {

    public static final List<FileComponentPattern> PATTERNS = createFilePatterns();

    private static List<FileComponentPattern> createFilePatterns() {
        final List<FileComponentPattern> fileComponentPatternsList = new ArrayList<>();

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/site-packages\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+)\\.dist-info\\/RECORD$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "/site-packages/$1";
            fcp.type = "module";
            fcp.specificType = "python-module";
            fileComponentPatternsList.add(fcp);
        }

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/site-packages\\/(.*)-([0-9]+\\.[0-9]+)\\.dist-info\\/RECORD$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "/site-packages/$1";
            fcp.type = "module";
            fcp.specificType = "python-module";
            fileComponentPatternsList.add(fcp);
        }

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/cis/(amazon-corretto)-([0-9]+\\.[0-9]+\\.[0-9]+.*).tar.gz$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "/cis/java-port/";
            fcp.type = "archive";
            fileComponentPatternsList.add(fcp);
        }

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+-[0-9]+)[\\.|.module\\+].*\\.([x86_64|noarch]+)\\.rpm$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "$0";
            fcp.type = "package";
            fcp.specificType = "rpm-package";
            fileComponentPatternsList.add(fcp);
        }

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+-[0-9]+)[\\.|.module\\+].*\\.([x86_64|noarch]+)\\.rpm$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "$0";
            fcp.type = "package";
            fcp.specificType = "rpm-package";
            fileComponentPatternsList.add(fcp);
        }

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]{8}-[0-9]+)[\\.|.module\\+].*\\.([x86_64|noarch]+)\\.rpm$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "$0";
            fcp.type = "package";
            fcp.specificType = "rpm-package";
            fileComponentPatternsList.add(fcp);
        }

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+\\.b[0-9]+-[0-9]+)\\.el8_4\\.([x86_64|noarch]+)\\.rpm$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "$0";
            fcp.type = "package";
            fcp.specificType = "rpm-package";
            fileComponentPatternsList.add(fcp);
        }

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]{4}e-[0-9]+)\\.el8\\.([x86_64|noarch]+)\\.rpm$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "$0";
            fcp.type = "package";
            fcp.specificType = "rpm-package";
            fileComponentPatternsList.add(fcp);
        }

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]{4}e-[0-9]+)\\.el8\\.([x86_64|noarch]+)\\.rpm$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "$0";
            fcp.type = "package";
            fcp.specificType = "rpm-package";
            fileComponentPatternsList.add(fcp);
        }

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/([^\\/]+)-([\\.[0-9]+]*-[0-9]+)\\.([x86_64|i686|noarch|src]+)\\.rpm$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "$0";
            fcp.type = "package";
            fcp.specificType = "rpm-package";
            fileComponentPatternsList.add(fcp);
        }

        {
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/([^\\/]+)-([\\.[0-9]+]*-[0-9]+)\\.rpm$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "$0";
            fcp.type = "package";
            fcp.specificType = "rpm-package";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit version jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit version jar + SNAPSHOT
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+-SNAPSHOT)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit version jar + qualifier + SNAPSHOT
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+-[a-zA-Z0-9]+-SNAPSHOT)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit version jar . qualifier + SNAPSHOT
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+\\.[a-zA-Z0-9]+-SNAPSHOT)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 2 digit version jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit version / build jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)_([0-9]+\\.[0-9]+\\.[0-9]+[\\.rv0-9-]+)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1_$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit version / build jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+[\\.v0-9-]+)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1_$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit version _ jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)_([0-9]+\\.[0-9]+\\.[0-9]+)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1_$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit version .Final jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+.Final)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit version .RELEASE jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+.RELEASE)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 2 digit version FINAL jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+-FINAL-[0-9]+)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 2 digit version FINAL jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+-FINAL-[0-9]+)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit pre -
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+\\.pre[0-9]+)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit pre _
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)_([0-9]+\\.[0-9]+\\.[0-9]+\\.pre[0-9]+)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1_$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // 3 digit - digits; e.g. nexus-ssl-plugin-3.30.1-01.jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/(.*)-([0-9]+\\.[0-9]+\\.[0-9]+-[0-9]+)\\.jar$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1_$2.jar";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }


        {   // usr/lib/jvm/java-1.8.0-openjdk-1.8.0.292.b10-0.el8_3.x86_64/lib/tools.jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/java-([0-9]+\\.[0-9]+\\.[0-9]+)-openjdk-(.*)\\/(lib|jre)\\/.*\\.jar$";
            fcp.replacementForName = "openjdk";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "openjdk-$2";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // usr/lib/jvm/java-1.8-openjdk-1.8.0.292.b10-0.el8_3.x86_64/lib/tools.jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/java-([0-9]+\\.[0-9]+)-openjdk-(.*)\\/(lib|jre)\\/.*\\.jar$";
            fcp.replacementForName = "openjdk";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "openjdk-$2";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // /usr/lib/jvm/jdk-8u322-bellsoft-x86_64/jre/lib/charsets.jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/jdk-(8u322)-bellsoft-(.*)\\/(lib|jre)\\/.*\\.jar$";
            fcp.replacementForName = "bellsoft-openjdk";
            fcp.replacementForVersion = "$1";
            fcp.replacementForQualifier = "bellsoft-openjdk-$1";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // /paketo-buildpacks_bellsoft-liberica/jre
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/paketo-buildpacks_bellsoft-liberica\\/jre\\/(lib|jre)\\/.*\\.jar$";
            fcp.replacementForName = "bellsoft-liberica";
            fcp.replacementForVersion = "null";
            fcp.replacementForQualifier = "bellsoft-liberica-jre";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // /usr/lib64/jvm/java-1.8-openjdk/lib/jrt-fs.jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/java-([0-9]+\\.[0-9]+)-openjdk\\/(lib|jre)\\/.*\\.jar$";
            fcp.replacementForName = "openjdk";
            fcp.replacementForVersion = "$1";
            fcp.replacementForQualifier = "openjdk-$1";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // /usr/lib64/jvm/java-11-openjdk-11/lib/jrt-fs.jar
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/java-([0-9]+)-openjdk-(.*)\\/(lib|jre)\\/.*\\.jar$";
            fcp.replacementForName = "openjdk";
            fcp.replacementForVersion = "$1";
            fcp.replacementForQualifier = "openjdk-$1";
            fcp.replacementForSubpath = "$0";
            fcp.type = "module";
            fcp.specificType = "jar-module";
            fileComponentPatternsList.add(fcp);
        }

        {   // /prometheus-client-mmap-1.2.10-x86_64-linux-gnu
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/([a-zA-Z-_]+)-([0-9]+\\.[0-9]+\\.[0-9]+)-(x86_64-.*)\\/[Cc]argo.toml$";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2-$3";
            fcp.replacementForSubpath = "$0";
            fileComponentPatternsList.add(fcp);
        }

        {   // ruby/gems/3.4.0/cache/[drb-2.2.1.gem]/[data.tar.gz]/[data.tar]/drb.gemspec
            FileComponentPattern fcp = new FileComponentPattern();
            fcp.patternString = "^.*\\/cache\\/\\[([a-zA-Z0-9-_]+)-([0-9]+\\.[0-9]+\\.[0-9]+)\\.gem\\]\\/\\[data.tar.gz\\]\\/\\[data\\.tar\\]\\/.+\\.gemspec";
            fcp.replacementForName = "$1";
            fcp.replacementForVersion = "$2";
            fcp.replacementForQualifier = "$1-$2";
            fcp.replacementForSubpath = "$0";

            fcp.type = "module";
            fcp.specificType = "ruby-gem";

            fileComponentPatternsList.add(fcp);
        }

        return fileComponentPatternsList;
    }

}
