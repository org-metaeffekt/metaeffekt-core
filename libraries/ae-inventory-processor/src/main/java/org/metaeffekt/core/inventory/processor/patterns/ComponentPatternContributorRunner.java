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
import java.util.regex.Matcher;
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
        final String lowercasedPathInContext = relativeAnchorFilePath.toLowerCase(ComponentPatternProducer.LocaleConstants.PATH_LOCALE);

        // provides a compiled pattern cache.
        final Map<String, Pattern> suffixPatternMap = new HashMap<>();

        for (Map.Entry<Integer, Map<String, List<ComponentPatternContributor>>> phaseEntry : phaseContributors.entrySet()) {

            final Set<Map.Entry<String, List<ComponentPatternContributor>>> contributorsForPhase = phaseEntry.getValue().entrySet();
            for (Map.Entry<String, List<ComponentPatternContributor>> suffixEntry : contributorsForPhase) {

                // check whether one of the contributor applies before perform expensive regex operations
                final List<ComponentPatternContributor> applicableCpcs = suffixEntry.getValue().stream().
                        filter(c -> c.applies(relativeAnchorFilePath)).collect(Collectors.toList());

                if (!applicableCpcs.isEmpty()) {
                    // FIXME-KKL: use regexp in contributors to avoid need to maintain the conversion logic
                    final String anchorFileWildcardPattern = suffixEntry.getKey();
                    final Pattern pattern = convertWildcardPatternToRegex(anchorFileWildcardPattern, suffixPatternMap);
                    final Matcher matcher = pattern.matcher(lowercasedPathInContext);
                    if (matcher.find()) {
                        for (ComponentPatternContributor contributor : applicableCpcs) {
                            try {
                                List<ComponentPatternData> componentPatterns = contributor.contribute(
                                        baseDir, relativeAnchorFilePath, checksum, context);

                                componentPatterns.forEach(cpd -> cpd.setContext(contributor.getClass().getName()));

                                results.addAll(componentPatterns);
                            } catch (Exception e) {
                                LOG.error("Contributor threw exception. Make contributor more robust.", e);
                            }
                        }
                    }
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
