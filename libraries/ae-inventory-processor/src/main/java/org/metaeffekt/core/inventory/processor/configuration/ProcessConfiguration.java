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
package org.metaeffekt.core.inventory.processor.configuration;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Base class for configurations classes. Used by the
 * {@link CentralSecurityPolicyConfiguration} and other
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
 *         {@link ProcessConfiguration#setProperties(Map)} that allow for loading and storing the
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

    private final LinkedHashMap<String, Pair<?, ?>> cachedProperties = new LinkedHashMap<>();

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

    public void debugLogConfiguration() {
        final Map<String, Object> configuration = getProperties();

        if (!configuration.isEmpty()) {
            LOG.debug("Configuration [{}]:", getId());
            logConfiguration(configuration, 1, LOG::debug);
        }
    }

    public void logConfiguration() {
        final Map<String, Object> configuration = getProperties();

        if (!configuration.isEmpty()) {
            LOG.info("Configuration [{}]:", getId());
            logConfiguration(configuration, 1, LOG::info);
        }
    }

    private void logConfiguration(List<Object> configuration, int indent, Consumer<String> logLevel) {
        if (!configuration.isEmpty()) {
            configuration.forEach(value -> {
                final StringBuilder sb = new StringBuilder();
                sb.append(IntStream.range(0, indent).mapToObj(i -> "  ").reduce("", String::concat));
                if (value instanceof LinkedHashMap) {
                    logLevel.accept(sb.toString());
                    logConfiguration((LinkedHashMap<String, Object>) value, indent + 1, logLevel);
                } else if (value instanceof List) {
                    logLevel.accept(sb.toString());
                    logConfiguration((List) value, indent + 1, logLevel);
                } else if (value instanceof ProcessConfiguration) {
                    logConfiguration(((ProcessConfiguration) value).getProperties(), indent + 1, logLevel);
                } else {
                    sb.append(value);
                    logLevel.accept(sb.toString());
                }
            });
        }
    }

    private void logConfiguration(Map<String, Object> configuration, int indent, Consumer<String> logLevel) {
        if (!configuration.isEmpty()) {
            configuration.forEach((key, value) -> {
                final StringBuilder sb = new StringBuilder();
                sb.append(IntStream.range(0, indent).mapToObj(i -> "  ").reduce("", String::concat))
                        .append(key).append(": ");
                if (value instanceof LinkedHashMap) {
                    logLevel.accept(sb.toString());
                    logConfiguration((LinkedHashMap<String, Object>) value, indent + 1, logLevel);
                } else if (value instanceof List) {
                    logLevel.accept(sb.toString());
                    logConfiguration((List) value, indent + 1, logLevel);
                } else if (value instanceof ProcessConfiguration) {
                    logLevel.accept(sb.toString());
                    logConfiguration(((ProcessConfiguration) value).getProperties(), indent + 1, logLevel);
                } else {
                    sb.append(value);
                    logLevel.accept(sb.toString());
                }
            });
        }
    }

    protected <S, T> T accessCachedProperty(String propertyId, S external, Function<S, T> supplier) {
        if (cachedProperties.containsKey(propertyId) && cachedProperties.get(propertyId).getLeft().equals(external)) {
            return (T) cachedProperties.get(propertyId).getRight();
        }

        T internal = supplier.apply(external);
        cachedProperties.put(propertyId, Pair.of(external, internal));
        return internal;

    }

    public Map<String, Object> getProperties() {
        LinkedHashMap<String, Object> lhm = new LinkedHashMap<>();
        for (Field f : this.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            Class<?> type = f.getType();

            ExcludedProcessProperty excludedAnnotation = f.getAnnotation(ExcludedProcessProperty.class);
            if (excludedAnnotation != null) continue;

            ConfigurableProcessProperty additionalConfigurationAnnotation = f.getAnnotation(ConfigurableProcessProperty.class);

            String propertyName = extractFieldName(f);
            Object serialized;
            try {
                // use default serializer
                if (additionalConfigurationAnnotation == null || additionalConfigurationAnnotation.converter() == ConfigurableProcessProperty.NoConfigurationSerializer.class) {
                    serialized = serialize(f.getType(), f.get(this));
                }
                // use custom serializer
                else {
                    Class<? extends ConfigurationSerializer<Object, Object>> converterClass = (Class<? extends ConfigurationSerializer<Object, Object>>) additionalConfigurationAnnotation.converter();
                    ConfigurationSerializer<Object, Object> converter = constructDefaultInstance(converterClass);

                    serialized = converter.serialize(f.get(this));
                }

                if(serialized != null) {
                    lhm.put(propertyName, serialized);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Cannot get field [" + f.getName() + "] of type [" + type + "] while serializing to JSON", e);
            }
        }

        return lhm;
    }

    public void setProperties(Map<String, Object> properties) {
        for (String key : properties.keySet()) {
            Object o = properties.get(key);

            Field f = findMatchingField(key);

            if (f != null) {
                f.setAccessible(true);

                ConfigurableProcessProperty additionalConfigurationAnnotation = f.getAnnotation(ConfigurableProcessProperty.class);
                try {
                    if (additionalConfigurationAnnotation == null || additionalConfigurationAnnotation.converter() == ConfigurableProcessProperty.NoConfigurationSerializer.class) {

                        if (ProcessConfiguration.class.isAssignableFrom(f.getType())) {
                            ProcessConfiguration subProcessConfiguration = (ProcessConfiguration) constructDefaultInstance(f.getType());
                            subProcessConfiguration.setProperties((LinkedHashMap<String, Object>) o);
                            f.set(this, subProcessConfiguration);
                        } else {
                            f.set(this, serialize(f.getType(), o));
                        }

                    } else {
                        Class<? extends ConfigurationSerializer<Object, Object>> converterClass = (Class<? extends ConfigurationSerializer<Object, Object>>) additionalConfigurationAnnotation.converter();
                        ConfigurationSerializer<Object, Object> converter = constructDefaultInstance(converterClass);
                        try {
                            f.set(this, converter.deserialize(o));
                        } catch (ClassCastException e) {
                            LOG.info("?");
                            throw e;
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected void collectMisconfigurations(List<ProcessMisconfiguration> misconfigurations) {
    }

    public final List<ProcessMisconfiguration> collectMisconfigurations() {
        final ArrayList<ProcessMisconfiguration> reasons = new ArrayList<>();
        collectMisconfigurations(reasons);
        return reasons;
    }

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

    protected void loadStringProperty(Map<String, Object> properties, String
            key, Consumer<String> consumer) {
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

    protected void loadBooleanProperty(Map<String, Object> properties, String
            key, Consumer<Boolean> consumer) {
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

    protected void loadIntegerProperty(Map<String, Object> properties, String
            key, Consumer<Integer> consumer) {
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

    protected void loadDoubleProperty(Map<String, Object> properties, String
            key, Consumer<Double> consumer) {
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

    protected void loadJsonObjectProperty(Map<String, Object> properties, String
            key, Consumer<JSONObject> consumer) {
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

    protected void loadJsonArrayProperty(Map<String, Object> properties, String
            key, Consumer<JSONArray> consumer) {
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

    protected <T> void loadProperty(Map<String, Object> properties, String
            key, Function<Object, T> converter, Consumer<T> consumer) {
        if (properties != null && properties.containsKey(key)) {
            final Object value = properties.get(key);
            consumer.accept(converter.apply(value));
        }
    }

    protected <T> void loadListProperty(Map<String, Object> properties, String
            key, Function<Object, T> converter, Consumer<List<T>> consumer) {
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

    protected <K, V> void loadMapProperty(Map<String, Object> properties, String
            key, Function<Object, K> keyConverter, Function<Object, V> valueConverter, Consumer<Map<K, V>> consumer) {
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

    protected <T> void loadSetProperty(Map<String, Object> properties, String
            key, Function<Object, T> converter, Consumer<Set<T>> consumer) {
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

    protected <T extends ProcessConfiguration> void loadSubConfiguration
            (Map<String, Object> properties, String
                    key, Supplier<T> configurationSupplier, Consumer<T> consumer) {
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

    protected <T extends ProcessConfiguration> void loadSubConfigurations
            (Map<String, Object> properties, String
                    key, Supplier<T> configurationSupplier, Consumer<List<T>> consumer) {
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

    private Object serialize(Class<?> targetType, Object value) {
        if (value == null) {
            return null;
        }

        // sub-configuration
        if (targetType.getSuperclass() == ProcessConfiguration.class) {
            return ((ProcessConfiguration) value).getProperties();
        }

        // sub-configurations
        if (targetType == List.class) {
            List<?> list = (List<?>) value;
            if(!list.isEmpty() && list.get(0).getClass().isAssignableFrom(ProcessConfiguration.class)) {
                return list.stream().map(e -> ((ProcessConfiguration) e).getProperties()).collect(Collectors.toList());
            }
        }

        // enum
        if(targetType.isEnum()) {
            try {
                Method valueOf = targetType.getMethod("valueOf", String.class);
                return valueOf.invoke(null, value);
            } catch (Exception e) {
            }
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        // string
        if (targetType == String.class) {
            return String.valueOf(value);

            // boxed types
        } else if (targetType == Boolean.class) {
            if (value instanceof Boolean) return value;
            else return Boolean.parseBoolean(String.valueOf(value));
        } else if (targetType == Integer.class) {
            if (value instanceof Number) return ((Number) value).intValue();
            else return Integer.parseInt(String.valueOf(value));
        } else if (targetType == Long.class) {
            if (value instanceof Number) return ((Number) value).longValue();
            else return Long.parseLong(String.valueOf(value));
        } else if (targetType == Float.class) {
            if (value instanceof Number) return ((Number) value).floatValue();
            else return Float.parseFloat(String.valueOf(value));
        } else if (targetType == Double.class) {
            if (value instanceof Number) return ((Number) value).doubleValue();
            else return Double.parseDouble(String.valueOf(value));

            // primitive types
        } else if (targetType == boolean.class) {
            if (value instanceof Boolean) return value;
            else return Boolean.parseBoolean(String.valueOf(value));
        } else if (targetType == int.class) {
            if (value instanceof Number) return ((Number) value).intValue();
            else return Integer.parseInt(String.valueOf(value));
        } else if (targetType == long.class) {
            if (value instanceof Number) return ((Number) value).longValue();
            else return Long.parseLong(String.valueOf(value));
        } else if (targetType == float.class) {
            if (value instanceof Number) return ((Number) value).floatValue();
            else return Float.parseFloat(String.valueOf(value));
        } else if (targetType == double.class) {
            if (value instanceof Number) return ((Number) value).doubleValue();
            else return Double.parseDouble(String.valueOf(value));

        } else {
            throw new IllegalArgumentException("Unsupported conversion from [" + value + " : " + value.getClass() + "] to [" + targetType + "]");
        }
    }

    public static <T> T constructDefaultInstance(Class<T> clazz) {
        try {
            final Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
            if (!declaredConstructor.isAccessible()) {
                declaredConstructor.setAccessible(true);
            }
            return declaredConstructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate target type " + clazz, e);
        }
    }

    private String extractFieldName(Field f) {
        ConfigurableProcessProperty annotation = f.getAnnotation(ConfigurableProcessProperty.class);
        return annotation == null || StringUtils.isEmpty(annotation.customName()) ? f.getName() : annotation.customName();
    }

    /**
     * Attempts to find the matching Field of the instance, by:
     * <p>
     *  first  -> checking the actual field names
     *  second -> checking if a field has a matching custom name configured
     *  third  -> checking if any matching alternative names exist for a field
     * @param key the property key
     * @return the appropriate field, or null if it is excluded
     * @throws IllegalArgumentException if no field can be found
     */
    private Field findMatchingField(String key) {
        Class<? extends ProcessConfiguration> clazz = this.getClass();
        List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));

        if(fields.stream().anyMatch(f -> f.getAnnotation(ExcludedProcessProperty.class) != null && f.getName().equals(key))) {
            return null;
        }

        Optional<Field> f;

        f = fields.stream().filter(field -> Objects.equals(field.getName(), key)).findAny();
        if(f.isPresent()) {
            return f.get();
        }

        f = fields.stream().filter(field -> field.getAnnotation(ConfigurableProcessProperty.class) != null &&
                Objects.equals(field.getAnnotation(ConfigurableProcessProperty.class).customName(), key)).findAny();
        if(f.isPresent()) {
            return f.get();
        }

        f = fields.stream().filter(field -> field.getAnnotation(ConfigurableProcessProperty.class) != null &&
                Arrays.asList(field.getAnnotation(ConfigurableProcessProperty.class).alternativeNames()).contains(key)).findAny();

        return f.orElseThrow(() ->  new IllegalArgumentException("No field found named: " + key + " in class: " + this.getClass().getSimpleName()));
    }

    public static class JsonObjectDefaultConverter implements ConfigurationSerializer<String, Map<String, ?>> {

        @Override
        public Map<String, ?> serialize(String internal) {
            return new JSONObject(internal).toMap();

        }

        @Override
        public String deserialize(Map<String, ?> external) {
            return new JSONArray(external).toString();
        }
    }

    public static class FileDefaultConverter implements ConfigurationSerializer<File, Object> {

        @Override
        public Object serialize(File external) {
            if(external == null) return null;
            return external.getAbsolutePath();
        }

        @Override
        public File deserialize(Object internal) {
            if(internal instanceof File) return (File) internal;

            return new File(String.valueOf(internal));
        }
    }

    public static class FileListDefaultConverter implements ConfigurationSerializer<List<File>, List<Object>> {

        @Override
        public List<Object> serialize(List<File> external) {
            return external.stream().map(File::getAbsolutePath).collect(Collectors.toList());
        }

        @Override
        public List<File> deserialize(List<Object> internal) {
            if(internal == null || internal.isEmpty()) {
                return Collections.emptyList();
            }
            if(internal.get(0) instanceof File) {
                return internal.stream().map(f -> ((File) f)).collect(Collectors.toList());
            }
            return internal.stream().map(f -> new File(String.valueOf(f))).collect(Collectors.toList());
        }
    }
}
