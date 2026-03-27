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
package org.metaeffekt.core.inventory.processor.patterns;

import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.patterns.contributors.ComponentPatternContributor;
import org.metaeffekt.core.inventory.processor.patterns.contributors.EvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComponentPatternContributorRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentPatternContributorRunner.class);

    /**
     * This map is used to store the contributors in the order of their phase.
     * <br>
     * The key is the phase number, the value is a list of contributors that are registered for that phase.
     * <br>
     * This is used to run the contributors in the correct order.
     */
    private TreeMap<Integer, List<ComponentPatternContributor>> phaseContributors;

    public static class ComponentPatternContributorRunnerBuilder {
        private final TreeMap<Integer, List<ComponentPatternContributor>> phaseContributors = new TreeMap<>();

        /**
         * Use {@link ComponentPatternContributorRunner#builder()}.
         */
        protected ComponentPatternContributorRunnerBuilder() {}

        /**
         * Adds a contributor to the resolver, used for later running.
         *
         * @param contributor the contributor to be added
         *
         * @return the builder
         */
        public synchronized ComponentPatternContributorRunnerBuilder add(ComponentPatternContributor contributor) {
            int phase = contributor.getExecutionPhase();
            phaseContributors.computeIfAbsent(phase, k -> new ArrayList<>()).add(contributor);
            return this;
        }

        /**
         * Builds a finalized object.
         *
         * @return the built object
         */
        public synchronized ComponentPatternContributorRunner build() {
            return new ComponentPatternContributorRunner(phaseContributors);
        }
    }

    private ComponentPatternContributorRunner(TreeMap<Integer, List<ComponentPatternContributor>> phaseContributors) {
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
     *
     * @param baseDir The baseDir of the scan.
     * @param relativeAnchorFilePath relative path to the file to check. Relative to baseDir (not virtualRootPath).
     * @param checksum the checksum of the anchor file.
     * @param context {@link EvaluationContext} for collecting.
     *
     * @return returns a list of generated component patterns
     */
    public List<ComponentPatternData> collectApplicable(File baseDir, String relativeAnchorFilePath, String checksum, EvaluationContext context) {
        final List<ComponentPatternData> results = new ArrayList<>();

        // the applicable contributors are collected in phase order
        for (Map.Entry<Integer, List<ComponentPatternContributor>> phaseEntry : phaseContributors.entrySet()) {

            final List<ComponentPatternContributor> contributorsForPhase = phaseEntry.getValue();

            // filter applicable contributors
            final List<ComponentPatternContributor> applicableContributors = contributorsForPhase.stream()
                    .filter(cpd -> cpd.applies(relativeAnchorFilePath))
                    .collect(Collectors.toList());

            // let the applicable contributors contribute their patterns
            if (!applicableContributors.isEmpty()) {
                for (ComponentPatternContributor contributor : applicableContributors) {
                    try {
                        final List<ComponentPatternData> componentPatterns =
                                contributor.contribute(baseDir, relativeAnchorFilePath, checksum, context);

                        // modulate patterns with the given context
                        componentPatterns.forEach(cpd -> cpd.setContext(contributor.getClass().getName()));

                        results.addAll(componentPatterns);
                    } catch (Exception e) {
                        LOG.error("Contributor threw exception. Ensure the contributor is robust.", e);
                    }

                    // NOTE: currently the resulted patterns are not aware of the phase they have been applied. This
                    //   implies that the file mapping may be further optimized by phases; a subsequent phase may
                    //   be applied to only the remaining files (instead of all files). Yet this would require further
                    //   analysis.
                }
            }
        }
        return results;
    }

    /**
     * Converts a wildcard pattern to a regex pattern.
     *
     * @param wildcardPattern the pattern to convert
     *
     * @return the converted pattern
     */
    private Pattern convertWildcardPatternToRegex(String wildcardPattern, Map<String, Pattern> suffixPatternMap) {

        // check for cached pattern
        final Pattern cachedPattern = suffixPatternMap.get(wildcardPattern);
        if (cachedPattern != null) return cachedPattern;

        // escape special regex characters except "*" (which we'll handle separately)
        String escapedPattern = wildcardPattern
                .replace(".", "\\.")
                .replace("?", "\\?")
                .replace("**", ".*") // Convert "**" to ".*" in regex, which means "any characters"
                .replace("/", "\\/"); // Escape forward slashes

        // Ensure the pattern matches the end of the string if it does not contain wildcards
        if (!wildcardPattern.contains("*")) {
            escapedPattern = ".*" + escapedPattern + "$";
        }

        final Pattern compiledPattern = Pattern.compile(escapedPattern);

        // cache pattern
        suffixPatternMap.put(wildcardPattern, compiledPattern);

        return compiledPattern;
    }

}
