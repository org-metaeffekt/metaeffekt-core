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
package org.metaeffekt.core.dependency.analysis.linkres;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Helper class, only for use within link resolution classes.<br>
 * Furthermore, only use an instance of this object with the same set of symlinks or resolution might get unstable.
 */
public class ResolverPathHolder {
    private static final Logger LOG = LoggerFactory.getLogger(ResolverPathHolder.class);

    protected String currentPath;
    protected final Set<String> previousPositions;
    protected ResolverStatus status = ResolverStatus.INFLIGHT;

    public ResolverPathHolder(String inputPath) {
        this(inputPath, null);
    }

    public ResolverPathHolder(String inputPath, Set<String> previousPositions) {
        Objects.requireNonNull(inputPath, "inputPath must not be null");

        if (!inputPath.startsWith("/")) {
            throw new IllegalArgumentException("Resolver inputPath must be absolute");
        }

        currentPath = inputPath;
        if (previousPositions != null) {
            this.previousPositions = new HashSet<>(previousPositions);
        } else {
            this.previousPositions = new HashSet<>();
        }
    }

    protected void checkInFlight() {
        if (this.getStatus() != ResolverStatus.INFLIGHT) {
            throw new IllegalStateException("Holder is not in flight any more and may not be modified.");
        }
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        Objects.requireNonNull(currentPath);

        checkInFlight();

        if (!currentPath.startsWith("/")) {
            throw new IllegalArgumentException("currentPath must be absolute");
        }

        this.currentPath = currentPath;
    }

    public Set<String> getPreviousPositions() {
        return Collections.unmodifiableSet(previousPositions);
    }

    /**
     * Add a position to mark it as "tested" or "resolved".<br>
     * If we get back to this position, we know we are running in circles.
     * @param attemptedPath an absolute path string
     */
    public void addPreviousPosition(String attemptedPath) {
        Objects.requireNonNull(attemptedPath);

        checkInFlight();

        if (!currentPath.startsWith("/")) {
            throw new IllegalArgumentException("attemptedPath must be absolute");
        }

        previousPositions.add(attemptedPath);
    }

    public ResolverStatus getStatus() {
        return status;
    }

    public void setStatus(ResolverStatus status) {
        Objects.requireNonNull(status, "status must not be null");

        if (status != ResolverStatus.INFLIGHT) {
            LOG.debug("Overriding status from [{}] to [{}]", this.status, status);
        }

        this.status = status;
    }
}
