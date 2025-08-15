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
package org.metaeffekt.core.inventory.processor.adapter.npm;

import org.json.JSONObject;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class NpmLockParserFactory {

    private static final String YARN_LOCK = "yarn.lock";

    public static PackageLockParser createPackageLockParser(File lockFile) throws IOException {
        final String json = FileUtils.readFileToString(lockFile, FileUtils.ENCODING_UTF_8);

        if (YARN_LOCK.equals(lockFile.getName().toLowerCase(Locale.US))) {
            return new YarnLockParser(lockFile);
        } else {
            // expecting a package-lock.json
            final JSONObject object = new JSONObject(json);
            int packageLockVersion = object.getInt("lockfileVersion");
            switch (packageLockVersion) {
                case 3: return new PackageLockParser3(lockFile, object);
                case 1:
                default:
                    return new PackageLockParser1(lockFile, object);
            }
        }
    }

}
