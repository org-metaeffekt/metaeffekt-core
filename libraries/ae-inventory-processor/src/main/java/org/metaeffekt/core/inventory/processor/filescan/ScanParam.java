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
package org.metaeffekt.core.inventory.processor.filescan;

import org.metaeffekt.core.util.FileUtils;

public class ScanParam {

    private String[] collectIncludes = new String[] { "**/*" };

    private String[] collectExcludes = new String[0];

    private String[] unwrapIncludes = new String[] { "**/*" };

    private String[] unwrapExcludes = new String[0];

    private boolean implicitUnwrap = true;

    public ScanParam() {
    }

    public ScanParam collectAllMatching(String[] includes, String[] excludes) {
        this.collectIncludes = includes;
        this.collectExcludes = excludes;
        return this;
    }

    public ScanParam unwrapAllMatching(String[] includes, String[] excludes) {
        this.unwrapIncludes = includes;
        this.unwrapExcludes = excludes;
        return this;
    }

    public boolean collects(String path) {
        return matches(path, collectIncludes, collectExcludes);
    }

    public boolean unwraps(String path) {
        return matches(path, unwrapIncludes, unwrapExcludes);
    }

    private static boolean matches(String path, String[] includePatterns, String[] excludePatterns) {
        for (String exclude : excludePatterns) {
            if (FileUtils.matches("/" + exclude, path)) {
                return false;
            }
        }
        for (String include : includePatterns) {
            if (FileUtils.matches("/" + include, path)) {
                return true;
            }
        }
        return false;
    }

    public boolean isImplicitUnwrap() {
        return implicitUnwrap;
    }

    public ScanParam implicitUnwrap(boolean implicitUnwrap) {
        this.implicitUnwrap = implicitUnwrap;
        return this;
    }
}
