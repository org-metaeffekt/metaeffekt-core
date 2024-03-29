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
package org.metaeffekt.reader.util;

import com.sun.codemodel.JCodeModel;
import org.apache.commons.io.FileUtils;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

/**
 * Converts json to java objects using jsonschema2pojo.
 * Note that this one eats a json. Not a json schema.
 * Useful for getting classes from docker inspect dumps.
 */
public class InspectToClassUtil {
    public static void main(String [] args) throws IOException {
        JCodeModel codeModel = new JCodeModel();

        URL source = new File("src/test/resources/container_config_1_19.json").toPath().toUri().toURL();
        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isIncludeGetters() {
                return false;
            }

            @Override
            public boolean isUseLongIntegers() {
                return true;
            }

            @Override
            public AnnotationStyle getAnnotationStyle() {
                return AnnotationStyle.JACKSON;
            }

            @Override
            public SourceType getSourceType() {
                return SourceType.JSON;
            }
        };

        SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());
        mapper.generate(codeModel, "ContainerConfigFromRepo", "org.metaeffekt.reader.inspect.image.model", source);

        File tempDir = Files.createTempDirectory("required").toFile();
        codeModel.build(tempDir);

        File dest = new File("./target/GENERATE-POJO-OUT");
        if (!dest.isDirectory()) {
            if (!dest.mkdirs()) {
                throw new RuntimeException("Failed to create output directory '" + dest.getAbsolutePath() + "'.");
            }
        }
        FileUtils.copyDirectory(tempDir, dest);
    }
}
