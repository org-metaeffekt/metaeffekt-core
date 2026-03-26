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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class AdapterRegistry {

    private final List<ReportAdapter> adapters = new ArrayList<>();

    public AdapterRegistry() {
        this.loadAdapters();
    }

    private void loadAdapters() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final ServiceLoader<ReportAdapter> serviceLoader = ServiceLoader.load(ReportAdapter.class, contextClassLoader);

        for (ReportAdapter adapter : serviceLoader) {
            this.adapters.add(adapter);
        }
    }

    public List<ReportAdapter> getAdapters() {
        return Collections.unmodifiableList(this.adapters);
    }

    public <T extends ReportAdapter> List<T> getAdapters(Class<T> type) {
        return this.adapters.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public <T extends ReportAdapter> T getAdapter(Class<T> type) {
        return this.adapters.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElse(null);
    }
}
