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
package org.metaeffekt.core.inventory.processor.report.adapter;

import java.util.*;

public abstract class ReportAdapterLoader {
    public static <T extends ReportAdapter> List<T> getAdapters(Class<T> type) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final ServiceLoader<T> serviceLoader = ServiceLoader.load(type, contextClassLoader);

        final List<T> adapters = new ArrayList<>();
        for (T adapter : serviceLoader) adapters.add(adapter);
        return adapters;
    }

    public static <T extends ReportAdapter> Optional<T> getAdapter(Class<T> type) {
        return getAdapters(type).stream().findFirst();
    }

    public static <T extends ReportAdapter> T getAdapterOrThrow(Class<T> type) {
        return getAdapters(type).stream().findFirst().orElseThrow(() -> {
            final int totalAdapters = getAllAdapters().size();
            return new AdapterNotFoundException("No implementations for report adapter [" + type.getSimpleName() + "] found (" + (totalAdapters > 0 ? totalAdapters + " other adapters available" : "in fact, not a single adapter was loaded") + "). " +
                    "Ensure the classpath contains an implementation that is registered as a service for loading via a ServiceLoader. " +
                    "The implementation you are most likely to be using is located at [com.metaeffekt.artifact.analysis:ae-artifact-analysis] and can be configured in your Maven POM <pluginManagement> to be applied to all reports.");
        });
    }

    public static List<ReportAdapter> getAllAdapters() {
        return getAdapters(ReportAdapter.class);
    }

    public static class AdapterNotFoundException extends RuntimeException {
        public AdapterNotFoundException() {
            super();
        }

        public AdapterNotFoundException(String message) {
            super(message);
        }

        public AdapterNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
