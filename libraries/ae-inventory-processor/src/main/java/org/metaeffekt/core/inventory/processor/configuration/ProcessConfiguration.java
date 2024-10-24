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
package org.metaeffekt.core.inventory.processor.configuration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Base class for configurations classes. Used by the
 * {@link org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration} and other
 * classes containing configuration parameters for the inventory enrichment process.<br>
 * A configuration class usually has several qualities:
 * <ul>
 *     <li>
 *         Contains fields of types that are settable via a maven configuration, meaning only collections, primitive
 *         data types, enums, and other classes that then need to be filled in the configuration.
 *     </li>
 *     <li>
 *         Setters allow for the builder pattern, always returning <code>this</code>.
 *     </li>
 *     <li>
 *         Has a method {@link ProcessConfiguration#getProperties()} and a counterpart
 *         {@link ProcessConfiguration#setProperties(LinkedHashMap)} that allow for loading and storing the
 *         configuration from and to a map. Use the <code>loadProperties</code> methods to load the properties from the
 *         map.
 *     </li>
 *     <li>
 *         Can provide customized information on what fields are misconfigured via the
 *         {@link ProcessConfiguration#collectMisconfigurations()} method.
 *     </li>
 * </ul>
 */
public abstract class ProcessConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessConfiguration.class);

    private boolean active = true;
    private String id = buildInitialId();

    public ProcessConfiguration setActive(boolean active) {
        this.active = active;
        return this;
    }

    public boolean isActive() {
        return active;
    }

    public ProcessConfiguration setId(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return id;
    }

    public void logConfiguration() {
        final Map<String, Object> configuration = getProperties();

        if (!configuration.isEmpty()) {
            LOG.info("Configuration [{}]:", getId());
            logConfiguration(configuration, 1);
        }
    }

    private void logConfiguration(List<Object> configuration, int indent) {
        if (!configuration.isEmpty()) {
            configuration.forEach(value -> {
                final StringBuilder sb = new StringBuilder();
                sb.append(IntStream.range(0, indent).mapToObj(i -> "  ").reduce("", String::concat));
                if (value instanceof LinkedHashMap) {
                    LOG.info(sb.toString());
                    logConfiguration((LinkedHashMap<String, Object>) value, indent + 1);
                } else if (value instanceof List) {
                    LOG.info(sb.toString());
                    logConfiguration((List) value, indent + 1);
                } else if (value instanceof ProcessConfiguration) {
                    logConfiguration(((ProcessConfiguration) value).getProperties(), indent + 1);
                } else {
                    sb.append(value);
                    LOG.info(sb.toString());
                }
            });
        }
    }

    private void logConfiguration(Map<String, Object> configuration, int indent) {
        if (!configuration.isEmpty()) {
            configuration.forEach((key, value) -> {
                final StringBuilder sb = new StringBuilder();
                sb.append(IntStream.range(0, indent).mapToObj(i -> "  ").reduce("", String::concat))
                        .append(key).append(": ");
                if (value instanceof LinkedHashMap) {
                    LOG.info(sb.toString());
                    logConfiguration((LinkedHashMap<String, Object>) value, indent + 1);
                } else if (value instanceof List) {
                    LOG.info(sb.toString());
                    logConfiguration((List) value, indent + 1);
                } else if (value instanceof ProcessConfiguration) {
                    LOG.info(sb.toString());
                    logConfiguration(((ProcessConfiguration) value).getProperties(), indent + 1);
                } else {
                    sb.append(value);
                    LOG.info(sb.toString());
                }
            });
        }
    }

    public abstract LinkedHashMap<String, Object> getProperties();

    public abstract void setProperties(LinkedHashMap<String, Object> properties);

    public final List<ProcessMisconfiguration> collectMisconfigurations() {
        final ArrayList<ProcessMisconfiguration> reasons = new ArrayList<>();
        collectMisconfigurations(reasons);
        return reasons;
    }

    protected abstract void collectMisconfigurations(List<ProcessMisconfiguration> misconfigurations);

    public void assertNoMisconfigurations() {
        final List<ProcessMisconfiguration> misconfigurations = collectMisconfigurations();
        if (!misconfigurations.isEmpty()) {
            final String message = misconfigurations.stream()
                    .map(ProcessMisconfiguration::toString)
                    .collect(Collectors.joining("\n ", " ", ""));
            throw new IllegalArgumentException("Configuration [" + getId() + "] contains misconfigurations:\n" + message);
        }
    }

    public String buildInitialId() {
        final String id = getClass().getSimpleName()
                .replace("Configuration", "")
                .replace("Inventory", "")
                .replace("Enrichment", "");
        final String[] split = id.split("(?=[A-Z])");
        return String.join("-", split).toLowerCase();
    }

    /* PROPERTY LOADING UTILITY METHODS */

    protected <F, T> T optionalConversion(F value, Function<F, T> converter) {
        return value != null ? converter.apply(value) : null;
    }

    protected void loadStringProperty(Map<String, Object> properties, String key, Consumer<String> consumer) {
        try {
            if (properties != null && properties.containsKey(key)) {
                final Object value = properties.get(key);
                if (value instanceof String) {
                    consumer.accept((String) value);
                } else {
                    throw createPropertyException(key, value, "string");
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set property '" + key + "' on " + getClass().getSimpleName() + " from " + properties, e);
        }
    }

    protected void loadBooleanProperty(Map<String, Object> properties, String key, Consumer<Boolean> consumer) {
        if (properties != null && properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Boolean) {
                consumer.accept((Boolean) value);
            } else if (value instanceof String) {
                consumer.accept(Boolean.valueOf((String) value));
            } else {
                throw createPropertyException(key, value, "boolean");
            }
        }
    }

    protected void loadIntegerProperty(Map<String, Object> properties, String key, Consumer<Integer> consumer) {
        if (properties != null && properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Integer) {
                consumer.accept((Integer) value);
            } else if (value instanceof Number) {
                consumer.accept(((Number) value).intValue());
            } else if (value instanceof String) {
                consumer.accept(Integer.valueOf((String) value));
            } else {
                throw createPropertyException(key, value, "integer");
            }
        }
    }

    protected void loadLongProperty(Map<String, Object> properties, String key, Consumer<Long> consumer) {
        if (properties != null && properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Long) {
                consumer.accept((Long) value);
            } else if (value instanceof Number) {
                consumer.accept(((Number) value).longValue());
            } else if (value instanceof String) {
                consumer.accept(Long.valueOf((String) value));
            } else {
                throw createPropertyException(key, value, "long");
            }
        }
    }

    protected void loadDoubleProperty(Map<String, Object> properties, String key, Consumer<Double> consumer) {
        if (properties != null && properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Double) {
                consumer.accept((Double) value);
            } else if (value instanceof Number) {
                consumer.accept(((Number) value).doubleValue());
            } else if (value instanceof String) {
                consumer.accept(Double.valueOf((String) value));
            } else {
                throw createPropertyException(key, value, "double");
            }
        }
    }

    protected void loadJsonObjectProperty(Map<String, Object> properties, String key, Consumer<JSONObject> consumer) {
        if (properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Map<?, ?>) {
                final Map<String, Object> valueMap = (Map<String, Object>) value;
                final JSONObject jsonObject = new JSONObject(valueMap);
                consumer.accept(jsonObject);
            } else {
                throw createPropertyException(key, value, "json-object");
            }
        }
    }

    protected void loadJsonArrayProperty(Map<String, Object> properties, String key, Consumer<JSONArray> consumer) {
        if (properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof List<?>) {
                final List<Object> valueList = (List<Object>) value;
                final JSONArray jsonArray = new JSONArray(valueList);
                consumer.accept(jsonArray);
            } else {
                throw createPropertyException(key, value, "json-array");
            }
        }
    }

    protected <T> void loadProperty(Map<String, Object> properties, String key, Function<Object, T> converter, Consumer<T> consumer) {
        if (properties != null && properties.containsKey(key)) {
            final Object value = properties.get(key);
            consumer.accept(converter.apply(value));
        }
    }

    protected <T> void loadListProperty(Map<String, Object> properties, String key, Function<Object, T> converter, Consumer<List<T>> consumer) {
        if (properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Collection<?>) {
                final Collection<?> valueList = (Collection<?>) value;
                final List<T> convertedList = valueList.stream()
                        .map(converter)
                        .collect(Collectors.toList());
                consumer.accept(convertedList);
            } else {
                throw createPropertyException(key, value, "collection");
            }
        }
    }

    protected <K, V> void loadMapProperty(Map<String, Object> properties, String key, Function<Object, K> keyConverter, Function<Object, V> valueConverter, Consumer<Map<K, V>> consumer) {
        if (properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Map<?, ?>) {
                final Map<K, V> convertedMap = new LinkedHashMap<>();
                final Map<?, ?> valueMap = (Map<?, ?>) value;
                valueMap.forEach((k, v) -> {
                    convertedMap.put(keyConverter.apply(k), valueConverter.apply(v));
                });
                consumer.accept(convertedMap);
            } else {
                throw createPropertyException(key, value, "map");
            }
        }
    }

    protected <T> void loadSetProperty(Map<String, Object> properties, String key, Function<Object, T> converter, Consumer<Set<T>> consumer) {
        if (properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Collection<?>) {
                final Collection<?> valueList = (Collection<?>) value;
                final Set<T> convertedList = valueList.stream()
                        .map(converter)
                        .collect(Collectors.toSet());
                consumer.accept(convertedList);
            } else {
                throw createPropertyException(key, value, "collection");
            }
        }
    }

    protected <T extends ProcessConfiguration> void loadSubConfiguration(Map<String, Object> properties, String key, Supplier<T> configurationSupplier, Consumer<T> consumer) {
        if (properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Map<?, ?>) {
                final Map<String, Object> valueMap = (Map<String, Object>) value;
                final T subConfiguration = configurationSupplier.get();
                subConfiguration.setProperties(new LinkedHashMap<>(valueMap));
                consumer.accept(subConfiguration);
            } else {
                throw createPropertyException(key, value, "map");
            }
        }
    }

    protected <T extends ProcessConfiguration> void loadSubConfigurations(Map<String, Object> properties, String key, Supplier<T> configurationSupplier, Consumer<List<T>> consumer) {
        if (properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Collection<?>) {
                final Collection<?> valueList = (Collection<?>) value;
                final List<T> convertedList = valueList.stream()
                        .map(v -> {
                            if (v instanceof Map<?, ?>) {
                                final Map<String, Object> valueMap = (Map<String, Object>) v;
                                final T subConfiguration = configurationSupplier.get();
                                subConfiguration.setProperties(new LinkedHashMap<>(valueMap));
                                return subConfiguration;
                            } else {
                                throw new IllegalArgumentException("Property '" + key + "' must be a list of maps.");
                            }
                        })
                        .collect(Collectors.toList());
                consumer.accept(convertedList);
            } else {
                throw createPropertyException(key, value, "list");
            }
        }
    }

    protected IllegalArgumentException createPropertyException(String key, Object value, String expectedType) {
        return new IllegalArgumentException("Property '" + key + "' must be a '" + expectedType + "' on " + getClass().getSimpleName() + " from " + value);
    }
}
