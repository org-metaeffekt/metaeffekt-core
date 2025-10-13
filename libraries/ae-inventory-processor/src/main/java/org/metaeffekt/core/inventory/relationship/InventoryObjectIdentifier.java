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
package org.metaeffekt.core.inventory.relationship;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains all known strategies for identifer creation or extraction as well as provides method to interact
 * with the identifiers.
 */
@SuppressWarnings("unchecked")
public class InventoryObjectIdentifier {

    private static final Map<Class<?>, IdentifierStrategy<?>> strategies = new HashMap<>();

    static {
        // artifact identifier creation strategy
        strategies.put(Artifact.class, new IdentifierStrategy<Artifact>(
                listOf(
                        Artifact.Attribute.ID.getKey(),
                        Artifact.Attribute.VERSION.getKey(),
                        Artifact.Attribute.CHECKSUM.getKey()),
                listOf(
                        (Artifact a) -> a.get(Artifact.Attribute.ID),
                        (Artifact a) -> a.get(Artifact.Attribute.VERSION),
                        (Artifact a) -> a.get(Artifact.Attribute.CHECKSUM)
                )
        ));

        // asset identifier creation strategy
        strategies.put(AssetMetaData.class, new IdentifierStrategy<AssetMetaData>(
                listOf(
                        AssetMetaData.Attribute.ASSET_ID.getKey(),
                        AssetMetaData.Attribute.VERSION.getKey(),
                        AssetMetaData.Attribute.CHECKSUM.getKey()),
                listOf(
                        a -> a.get(AssetMetaData.Attribute.ASSET_ID),
                        a -> a.get(AssetMetaData.Attribute.VERSION),
                        a -> a.get(AssetMetaData.Attribute.CHECKSUM)
                )
        ));
    }

    /**
     * Creates an identifier for an object contained in an {@link org.metaeffekt.core.inventory.processor.model.Inventory}
     *
     * @param object the object for which to create an identifier
     * @param <T> class type generic
     * @return the identifier as a string
     */
    public static <T> String createIdentifier(T object) {
        IdentifierStrategy<T> strategy = (IdentifierStrategy<T>) strategies.get(object.getClass());
        if (strategy == null) {
            throw new IllegalArgumentException("No identifier strategy for " + object.getClass());
        }
        return strategy.createIdentifier(object);
    }

    /**
     * Creates a map of inventory object attributes to their respective values based on the associated strategy and which
     * fields are present.
     *
     * @param type object class type for strategy selection
     * @param identifier the identifier
     * @return a map of present attributes to values
     * @param <T> class type generic
     */
    public static <T> Map<String, String> parseIdentifier(Class<T> type, String identifier) {
        final IdentifierStrategy<T> strategy = (IdentifierStrategy<T>) strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No identifier strategy for " + type);
        }
        return strategy.parseIdentifier(identifier);
    }


    private static <T> java.util.List<T> listOf(T... objects) {
        return Arrays.asList(objects);
    }
}
