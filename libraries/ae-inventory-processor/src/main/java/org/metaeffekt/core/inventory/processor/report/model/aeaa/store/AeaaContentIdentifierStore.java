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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.store;

import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ContentIdentifierStore is an abstract class that provides a storage mechanism for content identifiers, each represented by
 * a subclass of ContentIdentifier. This class supports registering, retrieving, and managing content identifiers based on their
 * name and implementation. The identifiers are validated and stored in a list, ensuring no duplicates based on name and implementation.
 * <p>
 * Subclasses must implement methods to create specific identifiers and define default identifiers.
 *
 * @param <T> the type of ContentIdentifier stored in this ContentIdentifierStore
 */
public abstract class AeaaContentIdentifierStore<T extends AeaaContentIdentifierStore.AeaaContentIdentifier> {

    private static final Logger LOG = LoggerFactory.getLogger(Inventory.class);

    private final List<T> contentIdentifiers = new ArrayList<>();

    protected AeaaContentIdentifierStore() {
        this.contentIdentifiers.addAll(this.createDefaultIdentifiers());
    }

    protected abstract T createIdentifier(String name, String implementation);

    protected abstract Collection<T> createDefaultIdentifiers();


    public List<T> values() {
        return this.contentIdentifiers;
    }

    public T registerIdentifier(T contentIdentifier) {
        if (contentIdentifier == null) {
            LOG.warn("Skipping registration of null content identifier in {}", this.getClass().getSimpleName());
            return null;

        } else if (contentIdentifier.getName() == null) {
            LOG.warn("Skipping registration of content identifier with null name in {}", this.getClass().getSimpleName());
            return null;

        } else if (contentIdentifier.getWellFormedName() == null) {
            contentIdentifier.setWellFormedName(AeaaContentIdentifier.deriveWellFormedName(contentIdentifier.getName()));
            LOG.warn("Deriving missing well-formed name for content identifier with name {} in {}: {}",
                    contentIdentifier.getName(), this.getClass().getSimpleName(), contentIdentifier.getWellFormedName());
        }

        final T existing = contentIdentifiers.stream()
                .filter(ci -> ci.getName().equals(contentIdentifier.getName()) && ci.getImplementation().equals(contentIdentifier.getImplementation()))
                .findFirst().orElse(null);
        if (existing != null) {
            LOG.warn("Skipping registration of content identifier with name {}, already present in {}->{}: {}",
                    contentIdentifier.getName(),
                    this.getClass().getSimpleName(), contentIdentifier.getClass().getSimpleName(),
                    contentIdentifiers.stream().map(AeaaContentIdentifier::getName).collect(Collectors.toList()));
            return existing;
        }

        contentIdentifiers.add(contentIdentifier);
        return contentIdentifier;
    }

    public List<T> fromMapNamesAndImplementations(Map<String, String> entries) {
        return entries.entrySet().stream()
                .map(entry -> this.fromNameAndImplementation(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public T fromNameAndImplementation(String name, String implementation) {
        final boolean hasImplementation = !StringUtils.isEmpty(implementation);
        final boolean hasName = !StringUtils.isEmpty(name);

        if (!hasName) {
            throw new IllegalArgumentException("Name must not be blank or null for content identifier in " + this.getClass().getSimpleName());
        }

        final String effectiveImplementation = hasImplementation ? implementation : name;

        // as a first attempt, use the name AND implementation to find matching content identifiers
        final Optional<T> byNameAndImplementation = contentIdentifiers.stream()
                .filter(ci -> name.equals(ci.getName()) && effectiveImplementation.equals(ci.getImplementation()))
                .findFirst();
        if (byNameAndImplementation.isPresent()) {
            return byNameAndImplementation.get();
        }

        final Optional<T> byNameAndImplementationWellFormed = contentIdentifiers.stream()
                .filter(ci -> (name.equals(ci.getName()) || name.equals(ci.getWellFormedName())) && effectiveImplementation.equals(ci.getImplementation()))
                .findFirst();
        if (byNameAndImplementationWellFormed.isPresent()) {
            return byNameAndImplementationWellFormed.get();
        }

        final Optional<T> byName = contentIdentifiers.stream()
                .filter(ci -> name.equals(ci.getName()) || name.equals(ci.getWellFormedName()))
                .findFirst();
        if (byName.isPresent()) {
            return byName.get();
        }

        // otherwise the identifier does not exist yet, register a new content identifier
        return this.registerIdentifier(this.createIdentifier(name, effectiveImplementation));
    }

    public T fromNameWithoutCreation(String name) {
        final boolean hasName = !StringUtils.isEmpty(name);

        if (!hasName) {
            throw new IllegalArgumentException("Name must not be blank or null for content identifier in " + this.getClass().getSimpleName());
        }

        final Optional<T> byName = contentIdentifiers.stream()
                .filter(ci -> name.equals(ci.getName()) || name.equals(ci.getWellFormedName()))
                .findFirst();

        return byName.orElse(null);
    }

    public T fromNameAndImplementationWithoutCreation(String name, String implementation) {
        final boolean hasName = !StringUtils.isEmpty(name);
        final boolean hasImplementation = !StringUtils.isEmpty(implementation);

        if (!hasName) {
            throw new IllegalArgumentException("Name must not be blank or null for content identifier in " + this.getClass().getSimpleName());
        }

        final Optional<T> byName = contentIdentifiers.stream()
                // returns true if:
                // - the name matches the name of the content identifier AND the implementation matches the implementation of the content identifier
                // - the name matches the name of the content identifier AND the implementation is not provided
                // - the name matches the well-formed name of the content identifier AND the implementation matches the implementation of the content identifier
                // - the name matches the well-formed name of the content identifier AND the implementation is not provided
                .filter(ci -> (name.equals(ci.getName()) || name.equals(ci.getWellFormedName())) && (!hasImplementation || implementation.equals(ci.getImplementation())))
                .findFirst();

        return byName.orElse(null);
    }

    public Optional<T> fromId(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Id must not be blank or null for content identifier in " + this.getClass().getSimpleName());
        }

        return contentIdentifiers.stream()
                .filter(ci -> ci.patternMatchesId(id))
                .findFirst();
    }

    public AeaaSingleContentIdentifierParseResult<T> fromJsonNameAndImplementation(JSONObject json) {
        final String name = ObjectUtils.firstNonNull(json.optString("source", null), json.optString("name", null), "unknown");
        final String implementation = ObjectUtils.firstNonNull(json.optString("implementation", null), name, "unknown");
        final T identifier = fromNameAndImplementation(name, implementation);

        final String id = json.optString("id", null);

        return new AeaaSingleContentIdentifierParseResult<>(identifier, id);
    }

    public List<T> fromJsonNamesAndImplementations(JSONArray json) {
        final List<T> identifiers = new ArrayList<>();

        for (int i = 0; i < json.length(); i++) {
            final JSONObject jsonObject = json.getJSONObject(i);
            final AeaaSingleContentIdentifierParseResult<T> pair = fromJsonNameAndImplementation(jsonObject);
            identifiers.add(pair.getIdentifier());
        }

        return identifiers;
    }

    public Map<T, Set<String>> fromJsonMultipleReferencedIds(JSONArray json) {
        final Map<T, Set<String>> referencedIds = new HashMap<>();

        for (int i = 0; i < json.length(); i++) {
            final JSONObject jsonObject = json.getJSONObject(i);
            final AeaaSingleContentIdentifierParseResult<T> pair = fromJsonNameAndImplementation(jsonObject);
            final T identifier = pair.getIdentifier();
            final String id = pair.getId();

            referencedIds.computeIfAbsent(identifier, k -> new HashSet<>()).add(id);
        }

        return referencedIds;
    }

    public AeaaSingleContentIdentifierParseResult<T> fromMap(Map<String, Object> map) {
        final String name = ObjectUtils.firstNonNull(map.get("source"), map.get("name"), "unknown").toString();
        final String implementation = ObjectUtils.firstNonNull(map.get("implementation"), name, "unknown").toString();
        final T identifier = fromNameAndImplementation(name, implementation);

        final String id = map.get("id") == null ? null : map.get("id").toString();

        return new AeaaSingleContentIdentifierParseResult<>(identifier, id);
    }

    public Map<T, Set<String>> fromListMultipleReferencedIds(List<Map<String, Object>> map) {
        final Map<T, Set<String>> referencedIds = new HashMap<>();

        for (Map<String, Object> entry : map) {
            final AeaaSingleContentIdentifierParseResult<T> pair = fromMap(entry);
            final T identifier = pair.getIdentifier();
            final String id = pair.getId();

            referencedIds.computeIfAbsent(identifier, k -> new HashSet<>()).add(id);
        }

        return referencedIds;
    }

    public static <T extends AeaaContentIdentifier> JSONArray toJson(Map<T, Set<String>> referencedIds) {
        final JSONArray jsonArray = new JSONArray();

        for (Map.Entry<T, Set<String>> entry : referencedIds.entrySet()) {
            final String name = entry.getKey().getName();
            final String implementation = entry.getKey().getImplementation();
            final Set<String> ids = entry.getValue();

            for (String id : ids) {
                jsonArray.put(new JSONObject()
                        .put("name", name)
                        .put("implementation", implementation)
                        .put("id", id));
            }
        }

        return jsonArray;
    }

    public static <T extends AeaaContentIdentifier> JSONArray toJson(Collection<T> contentIdentifiers) {
        final JSONArray jsonArray = new JSONArray();
        for (T contentIdentifier : contentIdentifiers) {
            jsonArray.put(new JSONObject()
                    .put("name", contentIdentifier.getName())
                    .put("implementation", contentIdentifier.getImplementation()));
        }
        return jsonArray;
    }

    public static class AeaaSingleContentIdentifierParseResult<T extends AeaaContentIdentifier> {
        private final T identifier;
        private final String id;

        public AeaaSingleContentIdentifierParseResult(T identifier, String id) {
            this.identifier = identifier;
            this.id = id;
        }

        public T getIdentifier() {
            return identifier;
        }

        public String getId() {
            return id;
        }
    }

    public static Map<AeaaContentIdentifier, Set<String>> parseLegacyJsonReferencedIds(JSONObject referencedIds) {
        final Map<String, Set<String>> mapFormatReferencedIds = new HashMap<>();

        referencedIds.toMap().forEach((key, value) -> {
            if (value instanceof Collection) {
                mapFormatReferencedIds.put(key, new HashSet<>((Collection<String>) value));
            } else {
                LOG.warn("Could not parse referenced ids of type [{}]: {}", key, value);
            }
        });

        return parseLegacyJsonReferencedIds(mapFormatReferencedIds);
    }

    public static <T extends Collection<String>> Map<AeaaContentIdentifier, Set<String>> parseLegacyJsonReferencedIds(Map<String, T> referencedIds) {
        final AeaaAdvisoryTypeStore advisoryTypeStore = AeaaAdvisoryTypeStore.get();
        final AeaaVulnerabilityTypeStore vulnerabilityTypeStore = AeaaVulnerabilityTypeStore.get();
        final AeaaOtherTypeStore otherTypeStore = AeaaOtherTypeStore.get();

        final Map<AeaaContentIdentifier, Set<String>> parsedReferencedIds = new HashMap<>();

        for (Map.Entry<String, T> entry : referencedIds.entrySet()) {
            final AeaaAdvisoryTypeIdentifier<?> advisoryIdentifier = advisoryTypeStore.fromNameWithoutCreation(entry.getKey());
            final AeaaVulnerabilityTypeIdentifier<?> vulnerabilityIdentifier = vulnerabilityTypeStore.fromNameWithoutCreation(entry.getKey());
            final AeaaOtherTypeIdentifier otherIdentifier = otherTypeStore.fromNameWithoutCreation(entry.getKey());

            if (advisoryIdentifier != null) {
                parsedReferencedIds.computeIfAbsent(advisoryIdentifier, k -> new HashSet<>()).addAll(entry.getValue());
            } else if (vulnerabilityIdentifier != null) {
                parsedReferencedIds.computeIfAbsent(vulnerabilityIdentifier, k -> new HashSet<>()).addAll(entry.getValue());
            } else if (otherIdentifier != null) {
                parsedReferencedIds.computeIfAbsent(otherIdentifier, k -> new HashSet<>()).addAll(entry.getValue());
            } else {
                LOG.warn("Could not find content identifier for referenced ids of type [{}]: {}", entry.getKey(), entry.getValue());
            }
        }

        return parsedReferencedIds;
    }

    public static <T extends AeaaContentIdentifier> JSONObject mergeIntoLegacyJson(List<Map<? extends T, Set<String>>> contentIdentifiersTypes) {
        final JSONObject json = new JSONObject();
        for (Map<? extends T, Set<String>> contentIdentifiers : contentIdentifiersTypes) {
            for (Map.Entry<? extends T, Set<String>> entry : contentIdentifiers.entrySet()) {
                json.put(entry.getKey().getName(), new JSONArray(entry.getValue()));
            }
        }
        return json;
    }

    public abstract static class AeaaContentIdentifier {
        /**
         * Unique identifiable name of the content source.<br>
         * SHOULD only contain <code>[a-zA-Z0-9_]</code>, but this will not be validated for.
         * Use {@link #prepareName(String)} to prepare the name to ensure it follows this pattern.
         */
        private String name;
        /**
         * Well-formed name of the content source.<br>
         * Will be used as a display name and SHOULD therefore be human-readable.
         */
        private String wellFormedName;
        /**
         * Specific implementation of the content source.<br>
         * May be left away if is identical to the {@link #name}.
         * Describes the implementation class (type) of a content source and will overwrite the {@link #name} when searching for an implementation class.
         */
        private String implementation;
        /**
         * Pattern to identify the content source by a representation of an identifier provided by the source of this content identifier.
         */
        private Pattern idPattern;

        public AeaaContentIdentifier(String name, String wellFormedName, String implementation, Pattern idPattern) {
            this.name = name;
            this.wellFormedName = wellFormedName;
            this.setImplementation(implementation);
            this.idPattern = idPattern;
        }

        public void setImplementation(String implementation) {
            this.implementation = StringUtils.isEmpty(implementation) ? this.name : implementation;
        }

        public String name() {
            return this.name;
        }

        public String getName() {
            return name;
        }

        public String getWellFormedName() {
            return wellFormedName;
        }

        public void setWellFormedName(String wellFormedName) {
            this.wellFormedName = wellFormedName;
        }

        public String getImplementation() {
            return implementation;
        }

        public Pattern getIdPattern() {
            return idPattern;
        }

        /**
         * Will use the {@link #idPattern} to check if the given id matches the pattern.<br>
         * If no pattern is provided, this will always return <code>false</code>.
         *
         * @param id Id to check
         * @return <code>true</code> if the id matches the pattern, <code>false</code> otherwise
         */
        public boolean patternMatchesId(String id) {
            if (idPattern == null) {
                return false;
            }

            return idPattern.matcher(id).matches();
        }

        public Set<String> fromFreeText(String text) {
            if (StringUtils.isEmpty(text) || this.idPattern == null) {
                return Collections.emptySet();
            }

            final Set<String> ids = new HashSet<>();

            final Matcher matcher = this.idPattern.matcher(text);
            while (matcher.find()) {
                ids.add(matcher.group());
            }

            return ids;
        }

        @Override
        public String toString() {
            return Objects.toString(this.getName());
        }

        public String toExtendedString() {
            if (this.implementation.equals(this.name)) {
                return this.getName() + " (" + this.getWellFormedName() + ")";
            } else {
                return this.getName() + ":" + this.getImplementation() + " (" + this.getWellFormedName() + ")";
            }
        }

        /**
         * Prepare the name to only contain <code>[a-zA-Z0-9_]</code>.
         * Will replace all characters not matching this pattern with an underscore and remove multiple underscores.
         *
         * @param name Name to prepare
         * @return Name with only <code>[a-zA-Z0-9_]</code>
         */
        public static String prepareName(String name) {
            return name.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("_+", "_");
        }

        /**
         * Derive a well-formed name from the given name.<br>
         * Will convert the name to lowercase, replace all <code>[-_ ]+</code> with a single space and capitalize the first letter of each word.<br>
         * If the name starts with <code>cert </code>, it will be replaced with <code>CERT-</code> and converted to uppercase.
         * <p>
         * Example: <code>cert cve</code> -&gt; <code>CERT-CVE</code> or <code>gitlab</code> -&gt; <code>Gitlab</code>
         *
         * @param name Name to derive a well-formed name from
         * @return Well-formed name
         */
        public static String deriveWellFormedName(String name) {
            name = name.toLowerCase().replaceAll("[-_ ]+", " ");
            if (name.startsWith("cert ")) {
                return name.replaceFirst("cert ", "CERT-").toUpperCase();
            } else {
                return WordUtils.capitalize(name);
            }
        }
    }
}
