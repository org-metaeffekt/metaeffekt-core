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
package org.metaeffekt.core.inventory.processor.patterns;

import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.patterns.contributors.ComponentPatternContributor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ComponentPatternContributorRunner {
    /**
     * Central map used for resolving which runner to use.<br>
     * It must represent the order that the contributors were inserted in:
     * <ul>
     *     <li>insertion must work in series</li>
     *     <li>for each contributor, generate an entry for each registered suffix</li>
     *     <li>(this means that for each suffix, the earliest added contributor takes priority)</li>
     *     <li>the order in which suffixes were added is now important (in contrast to the previous impl)</li>
     * </ul>
     * By doing this, it should yield the same behaviour as the previous implementation.
     * <br>
     * Do not modify it after construction! Ideally, the implementation should be unmodifiable.
     */
    private final Map<String, List<ComponentPatternContributor>> contributorResolver;

    /**
     * This list is carried along to record added contributors in correct order.
     * <br>
     * This may become useful for debugging purposes or be used to reconstruct a runner from an existing list.
     * Since the order is important, this will be much simpler than trying to reconstruct the order from
     * the {@link #contributorResolver}.
     */
    private final List<ComponentPatternContributor> registeredContributors;

    public static class ComponentPatternContributorRunnerBuilder {
        private final Map<String, List<ComponentPatternContributor>> contributorResolver = new LinkedHashMap<>();
        private final List<ComponentPatternContributor> registeredContributors = new ArrayList<>();

        /**
         * Use {@link ComponentPatternContributorRunner#builder()}.
         */
        protected ComponentPatternContributorRunnerBuilder() {}

        /**
         * Adds a contributor to the resolver, used for later running.
         * @param contributor the contributor to be added
         */
        public synchronized ComponentPatternContributorRunnerBuilder add(ComponentPatternContributor contributor) {
            registeredContributors.add(contributor);
            for (String suffix : contributor.getSuffixes()) {
                contributorResolver.computeIfAbsent(suffix, (k) -> new ArrayList<>()).add(contributor);
            }
            return this;
        }

        /**
         * Builds a finalized object.
         * @return the built object
         */
        public synchronized ComponentPatternContributorRunner build() {
            return new ComponentPatternContributorRunner(contributorResolver, registeredContributors);
        }
    }

    private ComponentPatternContributorRunner(
            Map<String, List<ComponentPatternContributor>> contributorResolver,
            List<ComponentPatternContributor> registeredContributors) {
        LinkedHashMap<String, List<ComponentPatternContributor>> intermediate = new LinkedHashMap<>();
        for (Map.Entry<String, List<ComponentPatternContributor>> entry : contributorResolver.entrySet()) {
            intermediate.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }

        // make these unmodifiable to prevent accidents
        this.contributorResolver = Collections.unmodifiableMap(intermediate);
        this.registeredContributors = Collections.unmodifiableList(registeredContributors);
    }

    /**
     * Returns a builder to construct an object of this class.
     * @return returns a builder
     */
    public static ComponentPatternContributorRunnerBuilder builder() {
        return new ComponentPatternContributorRunnerBuilder();
    }

    /**
     * Checks for and runs the first applicable contributor.
     * @param baseDir as in {@link ComponentPatternContributor}
     * @param relativeAnchorFilePath as in {@link ComponentPatternContributor}
     * @param checksum as in {@link ComponentPatternContributor}
     * @return returns a list of generated component patterns
     */
    public List<ComponentPatternData> run(File baseDir, String relativeAnchorFilePath, String checksum) {
        Objects.requireNonNull(relativeAnchorFilePath);

        String lowercasedPathInContext = relativeAnchorFilePath.toLowerCase(
                ComponentPatternProducer.localeConstants.PATH_LOCALE
        );

        for (Map.Entry<String, List<ComponentPatternContributor>> entry : contributorResolver.entrySet()) {
            if (lowercasedPathInContext.endsWith(entry.getKey())) {
                // this is a hit. value contributors declare interest in this file (by suffix check)
                for (ComponentPatternContributor contributor : entry.getValue()) {
                    // this needs the second layer of applies checks
                    if (contributor.applies(relativeAnchorFilePath)) {
                        List<ComponentPatternData> componentPatterns =
                                contributor.contribute(baseDir, relativeAnchorFilePath, checksum);

                        for (ComponentPatternData componentPattern : componentPatterns) {
                            componentPattern.setContext(contributor.getClass().getName());
                        }

                        // first applicable contributor wins.
                        return componentPatterns;
                    }
                }
                // no applicable contributors for this suffix
            }
        }
        // no applicable suffix, so no contributors present that declare support for this file. no derived patterns.
        return Collections.emptyList();
    }

    public List<ComponentPatternContributor> getRegisteredContributors() {
        return registeredContributors;
    }
}
