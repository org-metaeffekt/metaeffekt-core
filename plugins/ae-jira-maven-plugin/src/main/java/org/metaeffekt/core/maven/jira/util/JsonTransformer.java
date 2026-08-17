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
package org.metaeffekt.core.maven.jira.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.POJONode;

import java.util.*;

/**
 * Transformer to convert between object trees and the string representation of JSON data.
 */
public class JsonTransformer {

    /**
     * Convert data tree recursively to a JSON string.
     *
     * @param data   the data to be converted
     * @param indent if <code>true</code>, indent the output (typically with two spaces)
     * @return JSON string representation of the given data tree
     */
    public static String transform(Object data, boolean indent) {
        JsonMapper.Builder builder = JsonMapper.builder();
        if (indent) {
            builder.configure(SerializationFeature.INDENT_OUTPUT, indent);
        }
        final JsonMapper mapper = builder.build();
        return mapper.writeValueAsString(data);

    }

    /**
     * Convert a JSON string to a data tree structure.
     * <p>
     * Most of the effort goes into replacing the proprietary classes in the package
     * <code>org.codehaus.jackson</code> with plain standard Java classes like
     * <code>java.util.Map</code> and <code>java.util.List</code>.
     *
     * @param dataString the data string to be converted
     * @return The JSON data as a hierarchical structure of maps, lists, string and number values
     */
    public static Object transform(String dataString) {
        ObjectMapper mapper = new JsonMapper();
        JsonNode result = null;

        result = mapper.readTree(dataString);

        return transformNode(result);
    }

    private static Object transformNode(JsonNode node) {
        if (node.isArray()) {
            return transformArray((ArrayNode) node);
        } else if (node.isObject()) {
            return transformObject((ObjectNode) node);
        } else if (node.isPojo()) {
            return transformPojo((POJONode) node);
        } else if (node.isInt()) {
            return node.intValue();
        } else if (node.isBigInteger()) {
            return node.bigIntegerValue();
        } else if (node.isLong()) {
            return node.longValue();
        } else if (node.isDouble() || node.isFloatingPointNumber()) {
            return node.doubleValue();
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isString()) {
            return node.stringValue();
        } else if (node.isNull()) {
            return null;
        } else {
            return node.asString();
        }
    }

    private static Map<String, Object> transformPojo(POJONode pojoNode) {
        Map<String, Object> result = new HashMap<String, Object>();
        Iterator<String> fieldNames = pojoNode.propertyNames().iterator();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode node = pojoNode.get(key);
            result.put(key, transformNode(node));
        }
        return result;
    }

    private static Map<String, Object> transformObject(ObjectNode objectNode) {
        Map<String, Object> result = new HashMap<String, Object>();
        Iterator<String> fieldNames = objectNode.propertyNames().iterator();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode node = objectNode.get(key);
            result.put(key, transformNode(node));
        }
        return result;
    }

    private static List<Object> transformArray(ArrayNode arrayNode) {
        List<Object> result = new ArrayList<Object>();
        Iterator<JsonNode> nodes = arrayNode.values().iterator();
        while (nodes.hasNext()) {
            JsonNode node = nodes.next();
            result.add(transformNode(node));
        }
        return result;
    }

}
