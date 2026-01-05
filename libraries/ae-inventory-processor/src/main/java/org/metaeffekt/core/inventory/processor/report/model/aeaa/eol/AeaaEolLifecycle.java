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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.eol;

import org.json.JSONArray;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.state.AeaaLtsState;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.state.AeaaSupportState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class AeaaEolLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(AeaaEolLifecycle.class);

    private final String product;
    private final List<AeaaEolCycle> cycles;

    public AeaaEolLifecycle(String product, List<AeaaEolCycle> cycles) {
        this.product = product;
        this.cycles = cycles;
    }

    public AeaaEolLifecycle(String product) {
        this.product = product;
        this.cycles = new ArrayList<>();
    }

    public String getProduct() {
        return product;
    }

    public List<AeaaEolCycle> getCycles() {
        return cycles;
    }

    public void addCycle(AeaaEolCycle cycle) {
        this.cycles.add(cycle);
    }

    public AeaaEolCycle findCycleFromVersion(String cycleQueryVersion) {
        if (cycleQueryVersion == null) {
            return null;
        }
        return cycles.stream()
                .max((Comparator.comparingInt(o -> o.matchVersion(cycleQueryVersion))))
                .orElse(null);
    }

    public AeaaEolCycle findCycleFromCycle(String cycle) {
        if (cycle == null) {
            return null;
        }
        return cycles.stream()
                .filter(c -> c.getCycle().equals(cycle))
                .findFirst()
                .orElse(null);
    }

    public List<AeaaEolCycle> findCyclesAfter(AeaaEolCycle cycle) {
        if (cycle == null) {
            return null;
        }

        final List<AeaaEolCycle> result = new ArrayList<>();
        boolean found = false;

        for (AeaaEolCycle c : cycles) {
            if (found) {
                result.add(c);
            }
            if (c.equals(cycle)) {
                found = true;
            }
        }

        return result;
    }

    public List<AeaaEolCycle> findCyclesBefore(AeaaEolCycle cycle) {
        if (cycle == null) {
            return null;
        }

        final List<AeaaEolCycle> result = new ArrayList<>();
        boolean found = false;

        for (AeaaEolCycle c : cycles) {
            if (c.equals(cycle)) {
                found = true;
            }
            if (!found) {
                result.add(c);
            }
        }

        return result;
    }

    public AeaaEolCycle findNextSupportedCycle(AeaaEolCycle cycle) {
        if (cycle == null) {
            return null;
        }

        final List<AeaaEolCycle> cyclesAfter = findCyclesAfter(cycle);
        if (cyclesAfter.isEmpty()) {
            return null;
        }

        for (AeaaEolCycle c : cyclesAfter) {
            final AeaaSupportState supportState = c.getSupportState();
            if (supportState == AeaaSupportState.SUPPORT || supportState == AeaaSupportState.UPCOMING_SUPPORT_END_DATE || supportState == AeaaSupportState.DISTANT_SUPPORT_END_DATE) {
                return c;
            }
        }

        return null;
    }

    public AeaaEolCycle findNextExtendedSupportCycle(AeaaEolCycle cycle) {
        if (cycle == null) {
            return null;
        }

        final List<AeaaEolCycle> cyclesAfter = findCyclesAfter(cycle);
        if (cyclesAfter.isEmpty()) {
            return null;
        }

        for (AeaaEolCycle c : cyclesAfter) {
            final AeaaSupportState supportState = c.getExtendedSupportState();
            if (supportState == AeaaSupportState.SUPPORT || supportState == AeaaSupportState.UPCOMING_SUPPORT_END_DATE || supportState == AeaaSupportState.DISTANT_SUPPORT_END_DATE) {
                return c;
            }
        }

        return null;
    }

    /**
     * Finds the closest active LTS (Long Term Support) version cycle to the given cycle.
     * <p>
     * Only returns cycles that have LTS support. Will first try to find a cycle that comes after the given cycle, then
     * as fallback will go back from the cycle and return the first cycle backwards that has LTS support.
     *
     * @param cycle the cycle to find the closest active LTS version cycle for
     * @return the closest active LTS version cycle, or null if no active LTS version cycle is found
     */
    public AeaaEolCycle findClosestActiveLtsVersionCycle(AeaaEolCycle cycle) {
        if (cycle == null) {
            return null;
        }

        {
            final AeaaLtsState ltsState = cycle.getLtsState();
            if (ltsState == AeaaLtsState.LTS || ltsState == AeaaLtsState.LTS_DATE_REACHED) {
                return cycle;
            }
        }

        final List<AeaaEolCycle> cyclesAfter = findCyclesAfter(cycle);
        for (AeaaEolCycle c : cyclesAfter) {
            final AeaaLtsState ltsState = c.getLtsState();
            if (ltsState == AeaaLtsState.LTS || ltsState == AeaaLtsState.LTS_DATE_REACHED) {
                return c;
            }
        }

        final List<AeaaEolCycle> cyclesBefore = findCyclesBefore(cycle);
        for (int i = cyclesBefore.size() - 1; i >= 0; i--) {
            final AeaaEolCycle c = cyclesBefore.get(i);
            final AeaaLtsState ltsState = c.getLtsState();
            if (ltsState == AeaaLtsState.LTS || ltsState == AeaaLtsState.LTS_DATE_REACHED) {
                return c;
            }
        }

        return null;
    }

    public AeaaEolCycle findLatestActiveLtsVersionCycle(AeaaEolCycle cycle) {
        if (cycle == null) {
            return null;
        }

        final List<AeaaEolCycle> cyclesAfter = findCyclesAfter(cycle);
        for (int i = cyclesAfter.size() - 1; i >= 0; i--) {
            final AeaaEolCycle c = cyclesAfter.get(i);
            final AeaaLtsState ltsState = c.getLtsState();
            if (ltsState == AeaaLtsState.LTS || ltsState == AeaaLtsState.LTS_DATE_REACHED) {
                return c;
            }
        }

        return null;
    }

    public JSONArray toJson() {
        final JSONArray json = new JSONArray();

        for (AeaaEolCycle cycle : cycles) {
            json.put(cycle.toJson());
        }

        return json;
    }

    public static AeaaEolLifecycle fromJson(String product, JSONArray json) {
        final AeaaEolLifecycle productInfo = new AeaaEolLifecycle(product);

        for (int i = 0; i < json.length(); i++) {
            final AeaaEolCycle cycle = AeaaEolCycle.fromJson(json.getJSONObject(i));
            cycle.setProduct(product);
            productInfo.addCycle(cycle);
        }

        return productInfo;
    }

    public String toLink() {
        return "https://endoflife.date/" + product;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public String getLatestVersion() {
        return cycles.stream()
                .map(AeaaEolCycle::getLatest)
                .peek(cycle -> {
                    if (cycle == null) {
                        LOG.warn("Cycle is null for product, will filter out: {}", product);
                        LOG.warn("                                  All data: {}", this);
                    }
                })
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
