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

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class FileComponentPatternProcessor {

    private final Map<String, FileMetaData> pathDataMap;

    private final List<CompiledFileComponentPattern> patterns;

    private static class CompiledFileComponentPattern {
        public FileComponentPattern fileComponentPattern;
        public Pattern pattern;
    }

    public FileComponentPatternProcessor(Map<String, FileMetaData> fileDataMap, List<FileComponentPattern> patterns) {
        this.pathDataMap = fileDataMap;
        this.patterns = compile(patterns);
    }

    public FileComponentPatternProcessor() {
        this.pathDataMap = new HashMap<>();
        this.patterns = compile(DefaultFileComponentPatterns.PATTERNS);
    }

    private List<CompiledFileComponentPattern> compile(List<FileComponentPattern> fileComponentPatternList) {
        // compile patterns
        final List<CompiledFileComponentPattern> compiledPatterns = new ArrayList<>();
        for (FileComponentPattern fcp : fileComponentPatternList) {
            CompiledFileComponentPattern cfcp = new CompiledFileComponentPattern();
            cfcp.fileComponentPattern = fcp;
            cfcp.pattern = Pattern.compile(fcp.patternString);
            compiledPatterns.add(cfcp);
        }
        return compiledPatterns;
    }

    public FileMetaData deriveFileMetaData(String path) {
        // check map; early exit
        final FileMetaData mappedData = pathDataMap.get(path);
        if (mappedData != null) {
            return mappedData;
        }

        // skip files that we have anyway no pattern for
        if (path.endsWith(".dat")) return null;
        if (path.endsWith(".py")) return null;
        if (path.endsWith(".pyc")) return null;
        if (path.endsWith(".c")) return null;
        if (path.endsWith(".cpp")) return null;
        if (path.endsWith(".h")) return null;
        if (path.endsWith(".notzip")) return null;
        if (path.endsWith(".ko")) return null;
        if (path.endsWith(".go")) return null;
        if (path.endsWith(".vmanifest")) return null;
        if (path.endsWith("/Makefile")) return null;
        if (path.endsWith("/makefile")) return null;

        // apply file component patterns
        for (CompiledFileComponentPattern cfcp : patterns) {
            final Matcher matcher = cfcp.pattern.matcher(path);
            if (matcher.matches()) {
                final FileComponentPattern fcp = cfcp.fileComponentPattern;
                final String name = matcher.replaceAll(fcp.replacementForName);
                final String version = matcher.replaceAll(fcp.replacementForVersion);
                final String qualifier = matcher.replaceAll(fcp.replacementForQualifier);
                final String removableSubPath = matcher.replaceAll(fcp.replacementForSubpath);

                FileMetaData fmd = new FileMetaData();
                fmd.setPath(path);
                fmd.setName(name);
                fmd.setVersion(version);
                fmd.setQualifier(qualifier);
                fmd.setType(fcp.getType());
                fmd.setSpecificType(fcp.getSpecificType());

                return fmd;
            }
        }

        // complain
        if (path.endsWith(".jar")) {
            log.warn("Could not identify matching pattern to artifact in path: " + path);
        }

        return null;
    }

}
