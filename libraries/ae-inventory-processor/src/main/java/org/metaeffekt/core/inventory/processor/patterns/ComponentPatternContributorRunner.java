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
package org.metaeffekt.core.inventory.processor.patterns;

import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.patterns.contributors.ComponentPatternContributor;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentPatternContributorRunner {
    /**
     * This map is used to store the contributors in the order of their phase.
     * <br>
     * The key is the phase number, the value is a list of contributors that are registered for that phase.
     * <br>
     * This is used to run the contributors in the correct order.
     */
    private TreeMap<Integer, Map<String, List<ComponentPatternContributor>>> phaseContributors;

    public static class ComponentPatternContributorRunnerBuilder {
        private final TreeMap<Integer, Map<String, List<ComponentPatternContributor>>> phaseContributors = new TreeMap<>();

        /**
         * Use {@link ComponentPatternContributorRunner#builder()}.
         */
        protected ComponentPatternContributorRunnerBuilder() {}

        /**
         * Adds a contributor to the resolver, used for later running.
         * @param contributor the contributor to be added
         * @return the builder
         */
        public synchronized ComponentPatternContributorRunnerBuilder add(ComponentPatternContributor contributor) {
            int phase = contributor.getExecutionPhase();
            phaseContributors.computeIfAbsent(phase, k -> new HashMap<>());

            for (String suffix : contributor.getSuffixes()) {
                phaseContributors.get(phase).computeIfAbsent(suffix, k -> new ArrayList<>()).add(contributor);
            }
            return this;
        }

        /**
         * Builds a finalized object.
         * @return the built object
         */
        public synchronized ComponentPatternContributorRunner build() {
            return new ComponentPatternContributorRunner(phaseContributors);
        }
    }

    private ComponentPatternContributorRunner(TreeMap<Integer, Map<String, List<ComponentPatternContributor>>> phaseContributors) {
        this.phaseContributors = phaseContributors;
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
    public List<ComponentPatternData> run(File baseDir, String virtualRootPath, String relativeAnchorFilePath, String checksum) {
        List<ComponentPatternData> results = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, List<ComponentPatternContributor>>> phaseEntry : phaseContributors.entrySet()) {
            for (Map.Entry<String, List<ComponentPatternContributor>> suffixEntry : phaseEntry.getValue().entrySet()) {
                String lowercasedPathInContext = relativeAnchorFilePath.toLowerCase(ComponentPatternProducer.localeConstants.PATH_LOCALE);
                Pattern pattern = convertWildcardPatternToRegex(suffixEntry.getKey());
                Matcher matcher = pattern.matcher(lowercasedPathInContext);
                if (matcher.find()) {
                    for (ComponentPatternContributor contributor : suffixEntry.getValue()) {
                        if (contributor.applies(relativeAnchorFilePath)) {
                            List<ComponentPatternData> componentPatterns =
                                    contributor.contribute(baseDir, virtualRootPath, relativeAnchorFilePath, checksum);
                            results.addAll(componentPatterns);
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * Converts a wildcard pattern to a regex pattern.
     * @param wildcardPattern the pattern to convert
     * @return the converted pattern
     */
    private Pattern convertWildcardPatternToRegex(String wildcardPattern) {
        // Escape special regex characters except "*" (which we'll handle separately)
        String escapedPattern = wildcardPattern
                .replace(".", "\\.")
                .replace("?", "\\?")
                .replace("**", ".*") // Convert "**" to ".*" in regex, which means "any characters"
                .replace("/", "\\/"); // Escape forward slashes

        // Ensure the pattern matches the end of the string if it does not contain wildcards
        if (!wildcardPattern.contains("*")) {
            escapedPattern = ".*" + escapedPattern + "$";
        }

        return Pattern.compile(escapedPattern);
    }

}
