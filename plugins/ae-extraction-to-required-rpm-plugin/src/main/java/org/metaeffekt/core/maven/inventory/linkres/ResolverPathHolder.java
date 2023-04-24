package org.metaeffekt.core.maven.inventory.linkres;

import org.sonatype.guice.bean.reflect.Logs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Helper class, only for use within link resolution classes.<br>
 * Furthermore, only use an instance of this object with the same set of symlinks or resolution might get unstable.
 */
public class ResolverPathHolder {
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
            Logs.debug("Overriding status from [{}] to [{}]", this.status, status);
        }

        this.status = Objects.requireNonNull(status, "status may not be null");
    }
}
