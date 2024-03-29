/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.maven.kernel.resource;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing resources that are intended for input into a compiler.
 *
 * @author Karsten Klein
 */
public class CompilerResources {

    protected FileSet source;

    /**
     * Defines an additional path section when calculating the destination for the processed file.
     */
    protected String relativeOutputDirectory;

    /**
     * Where to put the compiled results
     */
    protected File destination;

    public static CompilerResources createCompilerResources(File sourceDirectory, String[] includes,
                                                            String[] excludes, File targetDirectory, String relativeOutputDirectory) {
        final CompilerResources resource = new CompilerResources();
        resource.source = new FileSet();
        resource.source.setDirectory(sourceDirectory.toString());
        if (includes != null) {
            resource.source.setIncludes(Arrays.asList(includes));
        }
        if (excludes != null) {
            resource.source.setExcludes(Arrays.asList(excludes));
        }
        resource.relativeOutputDirectory = relativeOutputDirectory;
        resource.destination = targetDirectory;
        return resource;
    }

    /**
     * Produces a map that contains the path to the input file and the path to the output file.
     *
     * @return The map as described above.
     */
    public Map<String, String> getFilesAndDestinations() {
        final Map<String, String> result = new LinkedHashMap<String, String>();

        final File sourceDirectory = new File(source.getDirectory());
        final DirectoryScanner scanner = scan(sourceDirectory);

        for (String included : scanner.getIncludedFiles()) {
            if (!included.isEmpty()) {
                final String sourceDirPath = FilenameUtils.separatorsToUnix(sourceDirectory.toString());
                final String subdir = StringUtils.difference(sourceDirPath, "/" + included);
                final File sourceDir = new File(sourceDirectory, included);
                File destDir = new File(this.destination, subdir);
                if (this.relativeOutputDirectory != null && !this.relativeOutputDirectory.isEmpty()) {
                    destDir = new File(destDir, this.relativeOutputDirectory);
                }
                result.put(FilenameUtils.separatorsToUnix(sourceDir.toString()),
                        FilenameUtils.separatorsToUnix(destDir.toString()));
            }
        }
        return result;
    }

    private DirectoryScanner scan(final File sourceDirectory) {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(sourceDirectory);
        scanner.setIncludes(toArray(source.getIncludes()));
        scanner.setExcludes(toArray(source.getExcludes()));
        scanner.addDefaultExcludes();
        scanner.scan();
        return scanner;
    }

    private String[] toArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }
}
