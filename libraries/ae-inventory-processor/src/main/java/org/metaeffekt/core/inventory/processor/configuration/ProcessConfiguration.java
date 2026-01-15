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
import org.metaeffekt.core.inventory.processor.configuration.converter.FieldConverter;
import org.metaeffekt.core.inventory.processor.report.configuration.CentralSecurityPolicyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.*;
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

    @ExcludeProcessConfigurationProperty
    private final LinkedHashMap<String, Pair<?, ?>> cachedProperties = new LinkedHashMap<>();

    public ProcessConfiguration setActive(boolean active) {
        this.active = active;
        return this;
    }

    public boolean isActive() {
        return this.active;
    }

    public ProcessConfiguration setId(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return this.id;
    }

    /* LOGGING */

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

    /* CACHING */

    protected <S, T> T accessCachedProperty(String propertyId, S external, Function<S, T> supplier) {
        if (cachedProperties.containsKey(propertyId) && Objects.equals(cachedProperties.get(propertyId).getLeft(), external)) {
            return (T) cachedProperties.get(propertyId).getRight();
        }

        try {
            final T internal = supplier.apply(external);
            cachedProperties.put(propertyId, Pair.of(external, internal));
            return internal;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to construct configuration property [" + propertyId + "]. Cause: " + e.getMessage() + "\nConfiguration Value: " + external, e);
        }
    }

    /* PROPERTY ACCESS (GET / SET) */

    public Map<String, Object> getProperties() {
        final LinkedHashMap<String, Object> properties = new LinkedHashMap<>();

        properties.put("configurationType", this.getClass().getSimpleName());

        for (Field field : this.getClass().getDeclaredFields()) {
            if (this.isExcluded(field)) {
                continue;
            }

            field.setAccessible(true);
            final String propertyName = this.extractFieldName(field);
            final ProcessConfigurationProperty configAnnotation = field.getAnnotation(ProcessConfigurationProperty.class);

            try {
                final Object rawValue = field.get(this);
                final Object serializedValue;

                if (hasCustomConverter(configAnnotation)) {
                    final FieldConverter<Object, Object> converter = constructDefaultInstance(
                            (Class<? extends FieldConverter<Object, Object>>) configAnnotation.converter());
                    serializedValue = converter.serialize(rawValue);
                } else {
                    serializedValue = this.serialize(rawValue);
                }

                if (serializedValue != null) {
                    properties.put(propertyName, serializedValue);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Cannot access field [" + field.getName() + "] during serialization", e);
            }
        }

        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();

            try {
                final Field field = findMatchingField(key);

                if (field != null) {
                    field.setAccessible(true);
                    final ProcessConfigurationProperty configAnnotation = field.getAnnotation(ProcessConfigurationProperty.class);

                    if (hasCustomConverter(configAnnotation)) {
                        final FieldConverter<Object, Object> converter = constructDefaultInstance(
                                (Class<? extends FieldConverter<Object, Object>>) configAnnotation.converter());
                        field.set(this, converter.deserialize(value));
                    } else {
                        setStandardProperty(field, key, value);
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Failed to configure property [%s] with value [%s]. Cause: %s", key, value, e.getMessage()), e);
            }
        }
    }

    private void setStandardProperty(Field field, String key, Object value) throws IllegalAccessException {
        final Class<?> targetType = field.getType();

        if (List.class.isAssignableFrom(targetType)) {
            field.set(this, this.deserializeCollection(key, field, value, ArrayList::new));
        } else if (Set.class.isAssignableFrom(targetType)) {
            field.set(this, this.deserializeCollection(key, field, value, HashSet::new));
        } else if (Map.class.isAssignableFrom(targetType)) {
            field.set(this, this.deserializeMap(key, field, value));
        } else {
            field.set(this, this.deserialize(key, targetType, value));
        }
    }

    private Collection<Object> deserializeCollection(String key, Field field, Object value, Supplier<Collection<Object>> constructor) {
        if (value == null) return constructor.get();

        if (!(value instanceof Collection)) {
            throw this.createPropertyException(key, value, "Collection");
        }

        final Class<?> itemType = this.extractGenericType(field, 0);
        final Collection<?> inputCollection = (Collection<?>) value;
        final Collection<Object> targetCollection = constructor.get();

        for (Object item : inputCollection) {
            targetCollection.add(this.deserialize(key, itemType, item));
        }
        return targetCollection;
    }

    private Map<Object, Object> deserializeMap(String key, Field field, Object value) {
        if (value == null) return new HashMap<>();

        if (!(value instanceof Map)) {
            throw this.createPropertyException(key, value, "Map");
        }

        final Class<?> keyType = this.extractGenericType(field, 0);
        final Class<?> valueType = this.extractGenericType(field, 1);
        final Map<?, ?> inputMap = (Map<?, ?>) value;
        final Map<Object, Object> targetMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : inputMap.entrySet()) {
            final Object k = this.deserialize(key + "-key", keyType, entry.getKey());
            final Object v = this.deserialize(key + "-value", valueType, entry.getValue());
            targetMap.put(k, v);
        }
        return targetMap;
    }

    private Class<?> extractGenericType(Field field, int index) {
        final Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType) {
            final Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
            if (index < typeArguments.length) {
                final Type argument = typeArguments[index];
                if (argument instanceof Class) {
                    return (Class<?>) argument;
                }
                throw new IllegalArgumentException("Field [" + field.getName() + "] uses wildcard or variable types, which are not supported for automatic configuration. Use a concrete type (e.g. List<String>).");
            }
        }

        throw new IllegalArgumentException("Field [" + field.getName() + "] is a raw type (missing generic arguments). Please define strict types (e.g. List<String> instead of List).");
    }

    /* VALIDATION */

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
        final String simpleName = getClass().getSimpleName()
                .replace("Configuration", "")
                .replace("Inventory", "")
                .replace("Enrichment", "");
        final String[] split = simpleName.split("(?=[A-Z])");
        return String.join("-", split).toLowerCase();
    }

    /* PROPERTY LOADING UTILITY METHODS */

    protected <T> void loadProperty(Map<String, Object> properties, String key, Function<Object, T> converter, Consumer<T> consumer) {
        if (properties != null && properties.containsKey(key)) {
            try {
                consumer.accept(converter.apply(properties.get(key)));
            } catch (Exception e) {
                throw createPropertyException(key, properties.get(key), "valid-type-conversion");
            }
        }
    }

    protected <T> void loadListProperty(Map<String, Object> properties, String key, Function<Object, T> converter, Consumer<List<T>> consumer) {
        if (properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Collection) {
                consumer.accept(((Collection<?>) value).stream().map(converter).collect(Collectors.toList()));
            } else {
                throw createPropertyException(key, value, "collection");
            }
        }
    }

    protected <K, V> void loadMapProperty(Map<String, Object> properties, String key, Function<Object, K> keyConverter, Function<Object, V> valueConverter, Consumer<Map<K, V>> consumer) {
        if (properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Map) {
                final Map<K, V> convertedMap = new LinkedHashMap<>();
                ((Map<?, ?>) value).forEach((k, v) -> convertedMap.put(keyConverter.apply(k), valueConverter.apply(v)));
                consumer.accept(convertedMap);
            } else {
                throw createPropertyException(key, value, "map");
            }
        }
    }

    protected <T> void loadSetProperty(Map<String, Object> properties, String key, Function<Object, T> converter, Consumer<Set<T>> consumer) {
        if (properties.containsKey(key)) {
            final Object value = properties.get(key);
            if (value instanceof Collection) {
                consumer.accept(((Collection<?>) value).stream().map(converter).collect(Collectors.toSet()));
            } else {
                throw createPropertyException(key, value, "collection");
            }
        }
    }

    /* SERIALIZATION / DESERIALIZATION CORE */

    private Object deserialize(String key, Class<?> targetType, Object value) {
        if (value == null) {
            return null;
        }

        final Class<?> sourceType = value.getClass();

        if (targetType.isAssignableFrom(sourceType)) {
            return value;
        }

        if (targetType == String.class) {
            return String.valueOf(value);
        }

        // primitives and wrappers
        if (targetType == Boolean.class || targetType == boolean.class) {
            return parseBoolean(value);
        } else if (Number.class.isAssignableFrom(targetType) || targetType.isPrimitive()) {
            return parseNumber(value, targetType, key);
        }

        if (targetType == File.class) {
            return new File(String.valueOf(value));
        }

        if (targetType.isEnum()) {
            try {
                final Method valueOf = targetType.getMethod("valueOf", String.class);
                return valueOf.invoke(null, String.valueOf(value));
            } catch (Exception e) {
            }
        }

        // sub-configuration
        if (ProcessConfiguration.class.isAssignableFrom(targetType)) {
            final ProcessConfiguration subConfig = (ProcessConfiguration) constructDefaultInstance(targetType);
            if (value instanceof Map) {
                subConfig.setProperties((Map<String, Object>) value);
            }
            return subConfig;
        }

        throw new IllegalArgumentException("Unsupported deserialization conversion from [" + sourceType.getSimpleName() + "] to [" + targetType.getSimpleName() + "]: " + value);
    }

    private Object serialize(Object value) {
        if (value == null) {
            return null;
        }

        final Class<?> sourceType = value.getClass();

        // sub-configurations
        if (value instanceof ProcessConfiguration) {
            return ((ProcessConfiguration) value).getProperties();
        }

        if (value instanceof Collection) {
            return ((Collection<?>) value).stream()
                    .map(this::serialize)
                    .collect(Collectors.toList());
        } else if (value instanceof Map) {
            final Map<Object, Object> targetMap = new LinkedHashMap<>();
            ((Map<?, ?>) value).forEach((k, v) -> targetMap.put(serialize(k), serialize(v)));
            return targetMap;
        }

        if (value instanceof File) {
            return ((File) value).getPath();
        }

        if (sourceType.isEnum()) {
            return ((Enum<?>) value).name();
        }

        if (sourceType == String.class || Number.class.isAssignableFrom(sourceType) || sourceType == Boolean.class) {
            return value;
        }

        throw new IllegalArgumentException("Unsupported serialization conversion from [" + sourceType.getSimpleName() + "] to [external type]: " + value);
    }

    private Boolean parseBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Object parseNumber(Object value, Class<?> targetType, String key) {
        final boolean isNumber = value instanceof Number;
        final Number numVal = isNumber ? (Number) value : null;
        final String strVal = String.valueOf(value);

        if (targetType == Integer.class || targetType == int.class) {
            return isNumber ? numVal.intValue() : Integer.parseInt(strVal);
        } else if (targetType == Long.class || targetType == long.class) {
            return isNumber ? numVal.longValue() : Long.parseLong(strVal);
        } else if (targetType == Double.class || targetType == double.class) {
            return isNumber ? numVal.doubleValue() : Double.parseDouble(strVal);
        } else if (targetType == Float.class || targetType == float.class) {
            return isNumber ? numVal.floatValue() : Float.parseFloat(strVal);
        }

        throw createPropertyException(key, value, targetType.getSimpleName());
    }

    /* REFLECTION UTILS */

    private String extractFieldName(Field f) {
        final ProcessConfigurationProperty annotation = f.getAnnotation(ProcessConfigurationProperty.class);
        return annotation == null || StringUtils.isEmpty(annotation.customName()) ? f.getName() : annotation.customName();
    }

    private boolean hasCustomConverter(ProcessConfigurationProperty annotation) {
        return annotation != null && annotation.converter() != ProcessConfigurationProperty.ExcludeConverter.class;
    }

    /**
     * Attempts to find the matching Field of the instance, by:
     * <p>
     * 1. checking the actual field names
     * 2. checking if a field has a matching custom name configured
     * 3. checking if any matching alternative names exist for a field
     *
     * @param key the property key
     * @return the appropriate field, or null if it is excluded
     * @throws IllegalArgumentException if no field can be found
     */
    private Field findMatchingField(String key) {
        for (Field f : this.getClass().getDeclaredFields()) {
            if (isExcluded(f)) continue;

            // 1. exact match
            if (f.getName().equals(key)) return f;

            final ProcessConfigurationProperty p = f.getAnnotation(ProcessConfigurationProperty.class);
            if (p != null) {
                // 2. custom name match
                if (StringUtils.equals(p.customName(), key)) {
                    return f;
                }
                // 3. alternative names match
                for (String alt : p.alternativeNames()) {
                    if (alt.equals(key)) return f;
                }
            }
        }

        return null;
    }

    private boolean isExcluded(Field f) {
        return Modifier.isStatic(f.getModifiers()) || f.isAnnotationPresent(ExcludeProcessConfigurationProperty.class);
    }

    public static <T> T constructDefaultInstance(Class<T> clazz) {
        try {
            final Constructor<T> constructor = clazz.getDeclaredConstructor();
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate target type " + clazz, e);
        }
    }

    protected IllegalArgumentException createPropertyException(String key, Object value, String expectedType) {
        return new IllegalArgumentException("Property [" + key + "] must be a [" + expectedType + "] on " + getClass().getSimpleName() + " from " + value);
    }
}
