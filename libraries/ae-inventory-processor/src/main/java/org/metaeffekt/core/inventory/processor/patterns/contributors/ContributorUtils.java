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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

public class ContributorUtils {

    public static String extendArchivePattern(String path) {
        int lastSlash = path.lastIndexOf("/");

        if (lastSlash == -1) {
            // do not extend if a wildcard is in the path
            if (path.contains("*")) {
                return null;
            }
            return "[" + path + "]";
        }

        String archiveName = path.substring(lastSlash + 1);

        if (archiveName.contains("*")) {
            // do not extend if a wildcard is in the archive name
            return null;
        }

        String archivePath = path.substring(0, lastSlash);

        // apply square brackets only to archive name and extend with wildcard
        return archivePath + "/[" + archiveName + "]/**/*";
    }

    public static String extendLibPattern(String path) {
        int lastSlash = path.lastIndexOf("/");

        if (lastSlash == -1) {
            // do not extend if a wildcard is in the path
            if (path.contains("*")) {
                return null;
            }
            return "[" + path + "]";
        }

        String archiveName = path.substring(lastSlash + 1);

        if (archiveName.contains("*")) {
            // do not extend if a wildcard is in the archive name
            return null;
        }

        String archivePath = path.substring(0, lastSlash);

        final int lastDotIndex = archiveName.lastIndexOf(".");

        if (lastDotIndex > -1) {
            // apply square brackets only to archive name and extend with wildcard
            return archivePath + "/" + archiveName.substring(0, lastDotIndex) + ".so*";
        } else {
            return archivePath + "/" + archiveName + ".so*";
        }
    }

}
