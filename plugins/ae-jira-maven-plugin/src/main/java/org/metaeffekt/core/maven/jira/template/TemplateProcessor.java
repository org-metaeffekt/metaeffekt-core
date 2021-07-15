/*
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.maven.jira.template;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.EscapeTool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

public class TemplateProcessor {

    private static final String ENCODING_UTF_8 = "UTF-8";

    private static final String RESOURCE_LOADING = "class, file";

    private static final String LOG_TAG = "velocity";

    private VelocityEngine ve;

    /**
     * Constructor.
     *
     * @param templateResourcePath the location where the template source files can be found
     */
    public TemplateProcessor(String templateResourcePath) {
        Properties properties = new Properties();
        properties.put(Velocity.RESOURCE_LOADER, RESOURCE_LOADING);
        properties.put(Velocity.FILE_RESOURCE_LOADER_PATH, templateResourcePath);
        properties.put(Velocity.FILE_RESOURCE_LOADER_CACHE, false);
        properties.put(Velocity.INPUT_ENCODING, ENCODING_UTF_8);
        properties.put(Velocity.OUTPUT_ENCODING, ENCODING_UTF_8);
        properties.put(Velocity.SET_NULL_ALLOWED, true);

        ve = new VelocityEngine(properties);
    }

    /**
     * Process a velocity template source and store it in the target path.
     *
     * @param source location of the template relative to the configured template resource path
     * @param target location where the result files are stored
     * @param data the data for template replacements
     *
     * @throws IOException when the source can not be read or the target can not be written
     */
    public void processFile(String source, File target, Map<String, Object> data)
            throws IOException {
        Template template = ve.getTemplate(source);
        VelocityContext context = arrangeContext(data);
        FileWriter writer = new FileWriter(target);
        template.merge(context, writer);
        writer.flush();
        writer.close();
    }

    public String processString(String input, Map<String, Object> data) {
        VelocityContext context = arrangeContext(data);
        StringWriter evaluated = new StringWriter();
        Velocity.evaluate(context, evaluated, LOG_TAG, input);
        return evaluated.toString();
    }

    private VelocityContext arrangeContext(Map<String, Object> data) {
        VelocityContext context = new VelocityContext();
        context.put("esc", new EscapeTool());
        for (Object key : data.keySet()) {
            context.put(key.toString(), data.get(key));
        }
        return context;
    }

}
