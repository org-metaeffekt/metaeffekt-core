/**
 * Copyright 2009-2020 the original author or authors.
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

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.POJONode;

import java.io.IOException;
import java.util.*;

/** Transformer to convert between object trees and the string representation of JSON data. */
public class JsonTransformer {

    /**
     * Convert data tree recursively to a JSON string.
     * 
     * @param data the data to be converted
     * @param indent if <code>true</code>, indent the output (typically with two spaces)
     * @return JSON string representation of the given data tree
     * @throws JsonGenerationException
     * @throws IOException
     */
    public static String transform(Object data, boolean indent) {
        ObjectMapper mapper = new ObjectMapper();
        if (indent) {
            mapper.configure(Feature.INDENT_OUTPUT, true);
        }
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonGenerationException e) {
            throw new RuntimeException(e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert a JSON string to a data tree structure.
     * <p>
     * Most of the effort goes into replacing the proprietary classes in the package
     * <code>org.codehaus.jackson</code> with plain standard Java classes like
     * <code>java.util.Map</code> and <code>java.util.List</code>.
     * 
     * @param dataString the data string to be converted
     * @return the JSON data as a hierachical structure of maps, lists, string and number values
     */
    public static Object transform(String dataString) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = null;

        try {
            result = mapper.readTree(dataString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
            return node.getIntValue();
        } else if (node.isBigInteger()) {
            return node.getBigIntegerValue();
        } else if (node.isLong()) {
            return node.getLongValue();
        } else if (node.isDouble() || node.isFloatingPointNumber()) {
            return node.getDoubleValue();
        } else if (node.isBoolean()) {
            return node.getBooleanValue();
        } else if (node.isTextual()) {
            return node.getTextValue();
        } else if (node.isNull()) {
            return null;
        } else {
            return node.getValueAsText();
        }
    }

    private static Map<String, Object> transformPojo(POJONode pojoNode) {
        Map<String, Object> result = new HashMap<String, Object>();
        Iterator<String> fieldNames = pojoNode.getFieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode node = pojoNode.get(key);
            result.put(key, transformNode(node));
        }
        return result;
    }

    private static Map<String, Object> transformObject(ObjectNode objectNode) {
        Map<String, Object> result = new HashMap<String, Object>();
        Iterator<String> fieldNames = objectNode.getFieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode node = objectNode.get(key);
            result.put(key, transformNode(node));
        }
        return result;
    }

    private static List<Object> transformArray(ArrayNode arrayNode) {
        List<Object> result = new ArrayList<Object>();
        Iterator<JsonNode> nodes = arrayNode.getElements();
        while (nodes.hasNext()) {
            JsonNode node = nodes.next();
            result.add(transformNode(node));
        }
        return result;
    }

}
