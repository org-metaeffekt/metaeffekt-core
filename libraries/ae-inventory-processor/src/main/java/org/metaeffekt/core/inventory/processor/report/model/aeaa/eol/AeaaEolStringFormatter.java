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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.eol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The strings provided by the EOL Date data source may contain variables and expressions in their link and releaseLabel
 * attributes. This class provides a formatter to replace these variables and expressions with values.
 * <p>
 * Examples:
 * <ul>
 *     <li>https://github.com/rabbitmq/rabbitmq-server/releases/tag/rabbitmq_v{{'__LATEST__'|replace:'.','_'}}</li>
 *     <li>https://archive.apache.org/dist/kafka/__LATEST__/RELEASE_NOTES.html</li>
 *     <li>OS X __RELEASE_CYCLE__ (__CODENAME__)</li>
 * </ul>
 * This is a two-step process:
 * <ol>
 *     <li>Replace variables with values</li>
 *     <li>Replace expressions with values</li>
 * </ol>
 * As of now, there is only one function:
 * <ul>
 *     <li>replace: replace all occurrences of the first argument with the second argument <code>{{'1.2.3'|replace:'.',''}}</code></li>
 * </ul>
 */
public abstract class AeaaEolStringFormatter {

    private final static Map<String, Function<AeaaEolCycle, String>> VARIABLE_MAPPERS = new LinkedHashMap<String, Function<AeaaEolCycle, String>>() {{
        put("__LATEST__", AeaaEolCycle::getLatest);
        put("__VERSION__", AeaaEolCycle::getLatest);
        put("__RELEASE_CYCLE__", AeaaEolCycle::getCycle);
        put("__CODENAME__", AeaaEolCycle::getCodename);
    }};

    public static String format(String baseString, AeaaEolCycle cycle) {
        if (baseString == null) return null;
        return baseString.contains("__") || baseString.contains("{{")
                ? replaceExpressions(replaceVariables(baseString, cycle), cycle)
                : baseString;
    }

    private static String replaceVariables(String baseString, AeaaEolCycle cycle) {
        if (baseString == null) return null;
        String result = baseString;
        for (Map.Entry<String, Function<AeaaEolCycle, String>> entry : VARIABLE_MAPPERS.entrySet()) {
            final String applied = entry.getValue().apply(cycle);
            final String escaped = applied == null ? "" : applied.replace("\\", "\\\\").replace("'", "\\'");
            result = result.replace(entry.getKey(), escaped);
        }
        return result;
    }

    private static String replaceExpressions(String baseString, AeaaEolCycle cycle) {
        String result = baseString;
        for (int i = 0; i < 100; i++) {
            final String expression = extractNextExpression(result);
            if (expression == null) {
                break;
            }
            final String replacement = evaluateExpression(expression, cycle);
            result = result.replace(expression, replacement);
        }
        return result;
    }

    private static String extractNextExpression(String result) {
        if (result == null) {
            return null;
        }
        final int start = result.lastIndexOf("{{");
        if (start < 0) {
            return null;
        }
        final int end = result.indexOf("}}", start);
        if (end < 0) {
            return null;
        }
        return result.substring(start, end + 2);
    }

    /**
     * Evaluates expressions of the form <code>{{'object'|function:'argument1','argument2'}}</code>.
     *
     * @param expression {{'object'|function:'argument1','argument2'}}
     * @param cycle      EOL cycle to get values from
     * @return evaluated expression
     */
    private static String evaluateExpression(String expression, AeaaEolCycle cycle) {
        final String innerExpression = expression.substring(2, expression.length() - 2);

        // go character per character and tokenize the string to prevent incorrect matching with the "'" or "\" character
        final List<String> tokens = tokenizeString(innerExpression);

        // find object, function and arguments and call evaluateFunction
        if (tokens.size() >= 3) {
            final String object = tokens.get(0);
            final String functionName = tokens.get(1);
            final String[] arguments = tokens.subList(2, tokens.size()).toArray(new String[0]);
            return evaluateFunction(object, functionName, arguments, cycle);
        }

        throw new IllegalArgumentException("Invalid expression: " + innerExpression);
    }

    private static String evaluateFunction(String object, String functionName, String[] arguments, AeaaEolCycle cycle) {
        switch (functionName) {
            case "replace":
                return object.replace(arguments[0], arguments[1]);
            case "trim":
                return object.trim();
            case "toLowerCase":
            case "lowerCase":
            case "lower":
                return object.toLowerCase();
            case "toUpperCase":
            case "upperCase":
            case "upper":
                return object.toUpperCase();
            case "substring":
                if (arguments.length == 1) {
                    return object.substring(Integer.parseInt(arguments[0]));
                } else if (arguments.length == 2) {
                    return object.substring(Integer.parseInt(arguments[0]), Integer.parseInt(arguments[1]));
                } else {
                    throw new IllegalArgumentException("Invalid number of arguments for substring: " + arguments.length);
                }
            default:
                throw new IllegalArgumentException("Unknown function: " + functionName);
        }
    }

    private static List<String> tokenizeString(String innerExpression) {
        final StringBuilder tokenBuilder = new StringBuilder();
        final List<String> tokens = new ArrayList<>();

        boolean inString = false;
        boolean wasInStringBeforeExiting = false;
        for (int i = 0; i < innerExpression.length(); i++) {
            final char c = innerExpression.charAt(i);
            if (c == '\\') {
                tokenBuilder.append(innerExpression.charAt(++i));
            } else if (c == '\'') {
                inString = !inString;
                wasInStringBeforeExiting = true;
            } else if ((c == ':' || c == '|' || c == ',') && !inString) {
                tokens.add(tokenBuilder.toString());
                tokenBuilder.setLength(0);
                wasInStringBeforeExiting = false;
            } else {
                tokenBuilder.append(c);
            }
        }
        if (tokenBuilder.length() > 0 || wasInStringBeforeExiting) {
            tokens.add(tokenBuilder.toString());
        }
        return tokens;
    }
}
