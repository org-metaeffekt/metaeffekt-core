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
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.metaeffekt.core.security.cvss.condition.ConditionTree.*;

public class ConditionTreeTest {

    @Test
    public void simpleComparisonRuleEvaluationTest() {
        ConditionTree condition = new ConditionTree()
                .rule(new NodeRule(NodeRule.Mode.AND, Arrays.asList(new ComparisonRule(5, "==", "key"))));

        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key", 5))));
    }

    @Test
    public void compositeRuleEvaluationAndTest() {
        ConditionTree condition = new ConditionTree()
                .and(new ComparisonRule(5, "==", "key1"), new ComparisonRule(10, "==", "key2"));

        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key2", 10))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key2", 15))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key2", 15))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 10, "key2", 10))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 10, "key2", 15))));
    }

    @Test
    public void compositeRuleEvaluationOrTest() {
        ConditionTree condition = new ConditionTree()
                .or(new ComparisonRule(5, "==", "key1"), new ComparisonRule(10, "==", "key2"));

        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key2", 10))));
        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key2", 15))));
        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key1", 10, "key2", 10))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 10, "key2", 15))));
    }

    @Test
    public void compositeRuleEvaluationNoneTest() {
        ConditionTree condition = new ConditionTree()
                .none(new ComparisonRule(20, "==", "key1"), new ComparisonRule(30, "==", "key2"));

        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key2", 15))));
        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key1", 30, "key2", 5))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key2", 30))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 20, "key2", 5))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 20, "key2", 30))));
    }

    @Test
    public void cvssConditionWithMultipleRulesTest() {
        ConditionTree condition = new ConditionTree()
                .or(new ComparisonRule(5, "==", "key1"))
                .or(new ComparisonRule(10, "==", "key2"));

        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key2", 20))));
        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key1", 20, "key2", 10))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 20, "key2", 20))));
    }

    @Test
    public void jsonSerializationAndDeserializationTest() {
        ConditionTree originalCondition = new ConditionTree()
                .and(new ComparisonRule(10, "==", "key"));

        JSONArray json = originalCondition.toJson();
        ConditionTree deserializedCondition = fromJson(json);

        assertTrue(deserializedCondition.isApplicable(new ConditionTreeContext(map("key", 10))));
        assertFalse(deserializedCondition.isApplicable(new ConditionTreeContext(map("key", 20))));
    }

    @Test
    public void contextDataExtractionTest() {
        ConditionTree condition = new ConditionTree()
                .or(new ComparisonRule(25, "==", "key", 1, "key2"));

        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key", Arrays.asList(
                map("key2", 30),
                map("key2", 25)
        )))));
        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key", Arrays.asList(
                map("key2", 30),
                map("key2", 25),
                map("key2", 35)
        )))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key", Arrays.asList(
                map("key2", 25),
                map("key2", 30)
        )))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key", Arrays.asList(
                map("key2", 25)
        )))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key3", Arrays.asList(
                map("key2", 30),
                map("key2", 25)
        )))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map())));
    }

    @Test
    public void predefinedConditionsAnyTest() {
        assertTrue(ANY.isApplicable(new ConditionTreeContext(map())));
        assertTrue(ANY.isApplicable(new ConditionTreeContext(map("key", 5))));
    }

    @Test
    public void predefinedConditionsNeverTest() {
        assertFalse(NEVER.isApplicable(new ConditionTreeContext(map())));
        assertFalse(NEVER.isApplicable(new ConditionTreeContext(map("key", 5))));
    }

    @Test
    public void emptyAndRuleListInNodeRuleTest() {
        ConditionTree condition = new ConditionTree().and();
        assertTrue(condition.isApplicable(new ConditionTreeContext(map())));
    }

    @Test
    public void emptyRuleListInCvssConditionTest() {
        ConditionTree condition = new ConditionTree();
        assertFalse(condition.isApplicable(new ConditionTreeContext(map())));
    }

    @Test
    public void nestedCompositeRulesTest() {
        ConditionTree condition = new ConditionTree()
                .or(
                        NodeRule.and(
                                new ComparisonRule(5, "==", "key1"),
                                NodeRule.or(
                                        new ComparisonRule(10, "==", "key2"),
                                        new ComparisonRule(15, "==", "key3")
                                )
                        )
                );

        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key2", 10))));
        assertTrue(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key3", 15))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key2", 15))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 5, "key3", 10))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 10, "key2", 10))));
        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 10, "key3", 15))));
    }

    @Test
    public void invalidObjectSelectorsInContextTest() {
        ConditionTree condition = new ConditionTree()
                .or(new ComparisonRule(5, "==", "invalidKey"));

        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key1", 5))));
    }

    @Test
    public void comparisonWithIncompatibleTypesTest() {
        ConditionTree condition = new ConditionTree()
                .or(new ComparisonRule(5, "==", "key"));

        assertFalse(condition.isApplicable(new ConditionTreeContext(map("key", "stringValue"))));
    }

    @Test
    public void equalConditionTreesTest() {
        ConditionTree tree1 = new ConditionTree()
                .or(new ComparisonRule(5, "==", "key1"))
                .or(new ComparisonRule(10, "==", "key2"));

        ConditionTree tree2 = new ConditionTree()
                .or(new ComparisonRule(5, "==", "key1"))
                .or(new ComparisonRule(10, "==", "key2"));

        assertEquals(tree1, tree2);
        assertEquals(tree1.hashCode(), tree2.hashCode());
    }

    @Test
    public void unequalConditionTreesTest() {
        ConditionTree tree1 = new ConditionTree()
                .or(new ComparisonRule(5, "==", "key1"));

        ConditionTree tree2 = new ConditionTree()
                .or(new ComparisonRule(5, "==", "key2"));

        assertNotEquals(tree1, tree2);
        assertNotEquals(tree1.hashCode(), tree2.hashCode());
    }

    @Test
    public void equalNodeRulesTest() {
        NodeRule rule1 = NodeRule.and(
                new ComparisonRule(5, "==", "key1"),
                new ComparisonRule(10, "==", "key2"));

        NodeRule rule2 = NodeRule.and(
                new ComparisonRule(5, "==", "key1"),
                new ComparisonRule(10, "==", "key2"));

        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
    }

    @Test
    public void unequalNodeRulesTest() {
        NodeRule rule1 = NodeRule.and(new ComparisonRule(5, "==", "key1", 3));
        NodeRule rule2 = NodeRule.or(new ComparisonRule(5, "==", "key1", 3));

        assertNotEquals(rule1, rule2);
    }

    @Test
    public void unequalNodeRulesAccessSelectorTest() {
        NodeRule rule1 = NodeRule.and(new ComparisonRule(5, "==", "key1", 2));
        NodeRule rule2 = NodeRule.and(new ComparisonRule(5, "==", "key1", 3));

        assertNotEquals(rule1, rule2);
    }

    private static Map<String, Object> map() {
        return new HashMap<>();
    }

    private static Map<String, Object> map(String k1, Object v1) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    private static Map<String, Object> map(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    private static Map<String, Object> map(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
}
