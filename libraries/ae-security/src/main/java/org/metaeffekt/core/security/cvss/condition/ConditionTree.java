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
package org.metaeffekt.core.security.cvss.condition;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class ConditionTree implements Cloneable {

    private final static Logger LOG = LoggerFactory.getLogger(ConditionTree.class);

    private final List<NodeRule> orRules = new ArrayList<>();

    public ConditionTree() {
    }

    public ConditionTree(NodeRule rule) {
        this.orRules.add(rule);
    }

    public List<NodeRule> getOrRules() {
        return orRules;
    }

    public boolean isApplicable(ConditionTreeContext context) {
        return orRules.stream().anyMatch(rule -> rule.isApplicable(context));
    }

    public ConditionTree rule(List<NodeRule> rules) {
        this.orRules.addAll(rules);
        return this;
    }

    public ConditionTree rule(NodeRule... rules) {
        this.orRules.addAll(Arrays.asList(rules));
        return this;
    }

    public ConditionTree or(Rule... rules) {
        this.orRules.add(NodeRule.or(rules));
        return this;
    }

    public ConditionTree and(Rule... rules) {
        this.orRules.add(NodeRule.and(rules));
        return this;
    }

    public ConditionTree none(Rule... rules) {
        this.orRules.add(NodeRule.none(rules));
        return this;
    }

    public JSONArray toJson() {
        return new JSONArray(orRules.stream().map(NodeRule::toJson).collect(Collectors.toList()));
    }

    public static ConditionTree fromJson(JSONArray json) {
        try {
            final ConditionTree condition = new ConditionTree();
            for (int i = 0; i < json.length(); i++) {
                final JSONObject rule = json.getJSONObject(i);
                final NodeRule parsedRule = NodeRule.parseRule(rule);
                condition.orRules.add(parsedRule);
            }
            return condition;
        } catch (Exception e) {
            LOG.error("Failed to parse {} instance from JSON: {}", ConditionTree.class.getSimpleName(), json, e);
            throw e;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionTree that = (ConditionTree) o;
        return Objects.equals(orRules, that.orRules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orRules);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public ConditionTree clone() {
        final ConditionTree clone = new ConditionTree();
        for (NodeRule orRule : orRules) {
            clone.orRules.add(orRule.clone());
        }
        return clone;
    }

    public final static ConditionTree ANY = new ConditionTree().or(new ComparisonRule(null, ComparisonRule.TRUE));
    public final static ConditionTree NEVER = new ConditionTree().and(new ComparisonRule(null, ComparisonRule.FALSE));

    public static class NodeRule implements Rule, Cloneable {
        private final Mode mode;
        private final List<Rule> rules;

        public NodeRule(Mode mode, List<Rule> rules) {
            this.mode = mode;
            this.rules = rules;
        }

        public List<Rule> getRules() {
            return rules;
        }

        public Mode getMode() {
            return mode;
        }

        public NodeRule addRule(Rule rule) {
            rules.add(rule);
            return this;
        }

        public static NodeRule and(Rule... rules) {
            return new NodeRule(Mode.AND, Arrays.asList(rules));
        }

        public static NodeRule or(Rule... rules) {
            return new NodeRule(Mode.OR, Arrays.asList(rules));
        }

        public static NodeRule none(Rule... rules) {
            return new NodeRule(Mode.NONE, Arrays.asList(rules));
        }

        @Override
        public boolean isApplicable(ConditionTreeContext context) {
            switch (mode) {
                case AND:
                    return rules.stream().allMatch(rule -> rule.isApplicable(context));
                case OR:
                    return rules.stream().anyMatch(rule -> rule.isApplicable(context));
                case NONE:
                    return rules.stream().noneMatch(rule -> rule.isApplicable(context));
                default:
                    throw new IllegalStateException("Unknown comparison mode [" + mode + "] on " + this);
            }
        }

        @Override
        public JSONObject toJson() {
            return new JSONObject()
                    .put("mo", mode.toString())
                    .put("ru", rules.stream().map(Rule::toJson).collect(Collectors.toList()));
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NodeRule nodeRule = (NodeRule) o;
            return mode == nodeRule.mode && Objects.equals(rules, nodeRule.rules);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mode, rules);
        }

        @Override
        public NodeRule clone() {
            final NodeRule clone = new NodeRule(mode, new ArrayList<>());
            for (Rule rule : rules) {
                clone.rules.add(rule.clone());
            }
            return clone;
        }

        private static NodeRule parseRule(JSONObject json) {
            final Mode mode = Mode.valueOf(json.getString("mo"));
            final List<Rule> rules = new ArrayList<>();
            final JSONArray ruleJson = json.getJSONArray("ru");
            for (int i = 0; i < ruleJson.length(); i++) {
                final JSONObject rule = ruleJson.getJSONObject(i);
                if (rule.has("mo")) {
                    rules.add(parseRule(rule));
                } else {
                    rules.add(ComparisonRule.fromJson(rule));
                }
            }
            return new NodeRule(mode, rules);
        }

        public enum Mode {
            AND,
            OR,
            NONE
        }
    }

    public static class ComparisonRule implements Rule, Cloneable {
        private final Object[] objectSelector;
        private final BiPredicate<Object, Object> operation;
        private final Object value;

        public ComparisonRule(Object value, BiPredicate<Object, Object> operation, Object... objectSelector) {
            this.objectSelector = objectSelector;
            this.operation = operation;
            this.value = value;
        }

        public ComparisonRule(Object value, String operation, Object... objectSelector) {
            this.objectSelector = objectSelector;
            this.operation = findOperationByName(operation);
            this.value = value;
        }

        public boolean isApplicable(ConditionTreeContext context) {
            final Object object = context.getObject(objectSelector);
            return evaluateCondition(this.operation, value, object);
        }

        @Override
        public JSONObject toJson() {
            JSONArray objectSelectorJson = new JSONArray();
            for (Object o : objectSelector) {
                objectSelectorJson.put(o);
            }
            return new JSONObject()
                    .put("op", findNameByOperation(operation))
                    .put("va", value)
                    .put("se", objectSelectorJson);
        }

        @Override
        public String toString() {
            return toJson().toString();
        }

        public static ComparisonRule fromJson(JSONObject json) {
            return new ComparisonRule(
                    json.get("va"), json.getString("op"),
                    json.getJSONArray("se").toList().toArray(new Object[0])
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComparisonRule that = (ComparisonRule) o;
            return Arrays.equals(objectSelector, that.objectSelector) && Objects.equals(operation, that.operation) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(operation, value);
            result = 31 * result + Arrays.hashCode(objectSelector);
            return result;
        }

        @Override
        public ComparisonRule clone() {
            return new ComparisonRule(value, operation, objectSelector);
        }

        public final static Map<String, BiPredicate<Object, Object>> REGISTERED_OPERATIONS_BY_NAME = new HashMap<>();
        public final static Map<BiPredicate<Object, Object>, String> REGISTERED_OPERATIONS_BY_OPERATION = new HashMap<>();

        public static BiPredicate<Object, Object> findOperationByName(String name) {
            if (name == null) {
                LOG.warn("Attempting to find condition by name which is null. Returning null.");
                return null;
            }
            final BiPredicate<Object, Object> condition = REGISTERED_OPERATIONS_BY_NAME.get(name);
            if (condition == null) {
                LOG.warn("Attempting to find condition by name [{}], which is not registered. Returning null.", name);
            }
            return condition;
        }

        public static String findNameByOperation(BiPredicate<Object, Object> operation) {
            if (operation == null) {
                LOG.warn("Attempting to find name by operation which is null. Returning null.");
                return null;
            }
            final String name = REGISTERED_OPERATIONS_BY_OPERATION.get(operation);
            if (name == null) {
                LOG.warn("Attempting to find name by operation [{}], which is not registered. Returning null.", operation);
            }
            return name;
        }

        public static boolean evaluateCondition(BiPredicate<Object, Object> operation, Object value, Object comparisonObject) {
            return operation.test(value, comparisonObject);
        }

        public static BiPredicate<Object, Object> registerOperation(String name, BiPredicate<Object, Object> condition) {
            REGISTERED_OPERATIONS_BY_NAME.put(name, condition);
            REGISTERED_OPERATIONS_BY_OPERATION.put(condition, name);
            return condition;
        }

        public final static BiPredicate<Object, Object> TRUE = registerOperation("true", (v, o) -> true);
        public final static BiPredicate<Object, Object> FALSE = registerOperation("false", (v, o) -> false);

        public final static BiPredicate<Object, Object> EQUALS = registerOperation("==", Object::equals);
        public final static BiPredicate<Object, Object> NOT_EQUALS = registerOperation("!=", (v, o) -> !EQUALS.test(v, o));
        public final static BiPredicate<Object, Object> CONTAINS = registerOperation("contains", (v, o) -> {
            if (v instanceof String && o instanceof String) {
                return ((String) o).contains((String) v);
            } else if (v instanceof Collection && o instanceof Collection) {
                return ((Collection) o).containsAll((Collection) v);
            } else if (v instanceof Collection && o instanceof String) {
                return ((Collection) v).stream().anyMatch(e -> ((String) o).contains(e.toString()));
            } else if (v instanceof String && o instanceof Collection) {
                return ((Collection) o).stream().anyMatch(e -> ((String) v).contains(e.toString()));
            } else {
                LOG.warn("Cannot apply contains operation on [{}] and [{}]", v, o);
                return false;
            }
        });
        public final static BiPredicate<Object, Object> NOT_CONTAINS = registerOperation("!contains", (v, o) -> !CONTAINS.test(v, o));

        public final static BiPredicate<Object, Object> GREATER_THAN = registerOperation(">", (v, o) -> {
            if (v instanceof Number && o instanceof Number) {
                return ((Number) o).doubleValue() > ((Number) v).doubleValue();
            } else {
                LOG.warn("Cannot apply greaterThan operation on [{}] and [{}]", v, o);
                return false;
            }
        });
        public final static BiPredicate<Object, Object> GREATER_THAN_OR_EQUALS = registerOperation(">=", (v, o) -> {
            if (v instanceof Number && o instanceof Number) {
                return ((Number) o).doubleValue() >= ((Number) v).doubleValue();
            } else {
                LOG.warn("Cannot apply greaterThanOrEquals operation on [{}] and [{}]", v, o);
                return false;
            }
        });
        public final static BiPredicate<Object, Object> LESS_THAN = registerOperation("<", (v, o) -> !GREATER_THAN_OR_EQUALS.test(v, o));
        public final static BiPredicate<Object, Object> LESS_THAN_OR_EQUALS = registerOperation("<=", (v, o) -> !GREATER_THAN.test(v, o));

        public final static BiPredicate<Object, Object> IS_NULL = registerOperation("isNull", (v, o) -> o == null);
        public final static BiPredicate<Object, Object> IS_EMPTY = registerOperation("isEmpty", (v, o) -> {
            if (o == null) return true;
            if (o instanceof String) return ((String) o).isEmpty();
            if (o instanceof Collection) return ((Collection) o).isEmpty();
            if (o instanceof Map) return ((Map) o).isEmpty();
            return false;
        });
        public final static BiPredicate<Object, Object> IS_NOT_NULL = registerOperation("!isNull", (v, o) -> o != null);
        public final static BiPredicate<Object, Object> IS_NOT_EMPTY = registerOperation("!isEmpty", (v, o) -> !IS_EMPTY.test(v, o));

        public final static BiPredicate<Object, Object> CONTAINS_CSV = registerOperation("containsCsv", (v, o) -> {
            if (v instanceof String && o instanceof String) {
                return Arrays.asList(((String) o).split(",")).containsAll(Arrays.asList(((String) v).split(", ")));
            } else if (v instanceof Collection && o instanceof String) {
                return ((Collection) v).stream().allMatch(e -> Arrays.asList(((String) o).split(", ")).contains(e.toString()));
            } else if (v instanceof String && o instanceof Collection) {
                return ((Collection) o).stream().anyMatch(e -> Arrays.asList(((String) v).split(", ")).contains(e.toString()));
            } else {
                LOG.warn("Cannot apply containsCsv operation on [{}] and [{}]", v, o);
                return false;
            }
        });
        public final static BiPredicate<Object, Object> NOT_CONTAINS_CSV = registerOperation("!containsCsv", (v, o) -> !CONTAINS_CSV.test(v, o));


    }

    public interface Rule extends Cloneable {
        boolean isApplicable(ConditionTreeContext context);

        JSONObject toJson();

        Rule clone();
    }
}
