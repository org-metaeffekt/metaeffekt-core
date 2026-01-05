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
package org.metaeffekt.core.inventory.processor.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
public class SheetSerializationContext {

    @Getter
    private final Inventory inventory;

    @Getter
    private final String contextKey;

    private final Supplier<Collection<? extends AbstractModelBase>> supplier;

    private final List<String> attributeList;

    public SheetSerializationContext(Inventory inventory, String contextKey, Supplier<Collection<? extends AbstractModelBase>> supplier) {
        this.inventory = inventory;
        this.contextKey = contextKey;
        this.supplier = supplier;

        final InventorySerializationContext inventorySerializationContext = inventory.getSerializationContext();
        this.attributeList = inventorySerializationContext.get(contextKey + ".columnlist");
    }

    public void renameAttribute(String oldAttribute, String newAttribute) {
        renameAttribute(oldAttribute, newAttribute, false);
    }

    public void renameAttributes(String oldAttribute, String newAttribute, boolean allowOverwrites) {
        renameAttribute(oldAttribute, newAttribute, allowOverwrites);
    }

    public void renameAttribute(String oldAttribute, String newAttribute, boolean allowOverwrites) {
        Collection<? extends AbstractModelBase> abstractModelBases = supplier.get();
        if (abstractModelBases == null || abstractModelBases.isEmpty()) return;

        for (AbstractModelBase object : abstractModelBases) {
            String oldValue = object.get(oldAttribute);
            String newValue = object.get(newAttribute);

            // normalize to null
            if (StringUtils.isEmpty(oldValue)) oldValue = null;
            if (StringUtils.isEmpty(newValue)) newValue = null;

            if (Objects.equals(oldValue, newValue)) {
                // already the same value; remove old attribute
                object.set(oldAttribute, null);
            } else if (newValue != null && oldValue != null) {
                // new value is already set and different from the value in the old attribute
                if (!allowOverwrites) {
                    final String message = String.format("Cannot rename [%s] attribute %s to %s. " +
                                    "New attribute already exists and contains different values: old [%s] vs new [%s]",
                            object.getClass(), oldAttribute, newAttribute, oldValue, newValue);
                    log.error(message);
                } else {
                    object.set(newAttribute, oldValue);
                    object.set(oldAttribute, null);
                }
            } else if (newValue == null) {
                // newValue is null; replace
                object.set(newAttribute, oldValue);
                object.set(oldAttribute, null);
            } else {
                // oldValue is null; do nothing
            }
        }

        // manage serialization context only if available
        if (attributeList != null) {
            int index = attributeList.indexOf(oldAttribute);
            int indexNew = attributeList.indexOf(newAttribute);

            // in case the new attribute already exists, it remains its position.
            if (indexNew == -1) {
                if (index != -1) {
                    // replace current position with new attribute
                    attributeList.set(index, newAttribute);
                } else {
                    // rename does not add any new items
                }
            }

            while (attributeList.contains(oldAttribute)) {
                attributeList.remove(oldAttribute);
            }
        }
    }

    public void removeAttributeStartingWith(String prefix) {
        removeMatchingAttributes(a -> a.startsWith(prefix));
    }

    public void removeAttribute(String attribute) {
        final Collection<? extends AbstractModelBase> objects = supplier.get();
        if (objects != null && !objects.isEmpty()) {
            for (AbstractModelBase object : objects) {
                object.set(attribute, null);
            }
        }
        if (attributeList != null) {
            attributeList.remove(attribute);
        }
    }

    private void removeMatchingAttributes(Predicate<String> attributePredicate) {
        final Collection<? extends AbstractModelBase> objects = supplier.get();
        if (objects != null && !objects.isEmpty()) {
            for (AbstractModelBase object : objects) {
                for (String attribute : new HashSet<>(object.getAttributes())) {
                    if (attributePredicate.test(attribute)) {
                        object.set(attribute, null);
                    }
                }
            }
        }
        if (attributeList != null) {
            attributeList.removeIf(attributePredicate);
        }
    }

    public void moveToEnd(String attribute) {
        if (attributeList != null) {
            if (attributeList.remove(attribute)) {
                // reinsert only in case the attribute existed; do not introduce new columns here
                attributeList.add(attribute);
            }
        }
    }

    public void insertAttribute(String attribute) {
        if (attributeList != null) {
            if (!attributeList.contains(attribute)) {
                attributeList.add(attribute);
            }
        }
    }


    public void insertAttribute(String referenceAttribute, String insertAttribute, int offset) {
        if (attributeList == null) return;
        int indexOfReferenceItem = attributeList.indexOf(referenceAttribute);
        int indexOfItem = attributeList.indexOf(insertAttribute);

        // do not insert new (empty attributes)
        if (indexOfItem == -1) return;

        if (indexOfReferenceItem >= 0) {
            attributeList.remove(insertAttribute);
            attributeList.add(Math.min(Math.max(0, indexOfReferenceItem + offset), attributeList.size()), insertAttribute);
        }
    }

    public void combineAttributes(String targetAttribute, String attribute1, String attribute2, String deliminator) {
        Collection<? extends AbstractModelBase> objects = supplier.get();
        if (objects != null && !objects.isEmpty()) {
            for (AbstractModelBase object : objects) {
                StringBuffer sb = new StringBuffer();

                String attributeValue1 = object.get(attribute1);
                String attributeValue2 = object.get(attribute2);

                if (StringUtils.isNotEmpty(attributeValue1)) {
                    sb.append(attributeValue1);
                }
                if (StringUtils.isNotEmpty(sb.toString())) {
                    sb.append(deliminator);
                }
                if (StringUtils.isNotEmpty(attributeValue2)) {
                    sb.append(attributeValue2);
                }

                object.set(attribute1, null);
                object.set(attribute2, null);
                if (StringUtils.isNotEmpty(sb.toString())) {
                    object.set(targetAttribute, sb.toString());
                }
            }
        }
        if (attributeList != null) {
            List<String> removableAttributes = new ArrayList<>();
            removableAttributes.add(attribute1);
            removableAttributes.add(attribute2);
            removableAttributes.remove(targetAttribute);
            attributeList.removeAll(removableAttributes);
        }
    }

}
