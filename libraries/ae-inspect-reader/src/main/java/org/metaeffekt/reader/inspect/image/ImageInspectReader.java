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
package org.metaeffekt.reader.inspect.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.metaeffekt.reader.inspect.image.model.ImageInspectData;

import java.io.File;
import java.io.IOException;

/**
 * Collection of static functions for reading docker (image) inspect output to java objects..
 */
public class ImageInspectReader {

    protected static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reads docker inspect json files and converts them to a list of inspection outputs.
     * This can contain multiple elements since docker inspect can output multiple inspections
     * in the master array.
     *
     * @param file The json file to read from.
     *
     * @return Returns ImageInspectData containing the individual inspect sections.
     */
    public static ImageInspectData dataFromJson(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("Passed file was not a file: " + file);
        }

        ImageInspectData inspects;
        try {
            inspects = objectMapper.readValue(file, ImageInspectData.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Encountered exception while reading [" + file.getName() + "]: " +
                    e.getMessage(), e);
        }

        return inspects;
    }
}
