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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A class for resolving linux symlinks using only string paths.<br>
 * This is not perfect if root differs. In other words, root must be ROOT. Symlinks to the outside may not resolve.
 */
public class LinuxSymlinkResolver {
    private static final Logger LOG = LoggerFactory.getLogger(LinuxSymlinkResolver.class);
    protected Map<String, String> symlinks = new HashMap<>();

    // should be way higher than on any reasonable system.
    protected static long maxDepth = 128;

    /**
     * Constructs a new resolver using the provided symlinks.<br>
     * Note that input symlink paths must be absolute.
     * @param symlinks the map of input symlinks, from a symlink's full path to its target.
     */
    public LinuxSymlinkResolver(Map<String, String> symlinks) {
        for (Map.Entry<String, String> symlink : symlinks.entrySet()) {
            if (symlink.getKey() == null) {
                throw new IllegalArgumentException("input symlink paths must not be null");
            }
            if (symlink.getValue() == null) {
                throw new IllegalArgumentException("input symlink values must not be null");
            }
            if (!symlink.getKey().startsWith("/")) {
                throw new IllegalArgumentException("input symlink path must be absolute");
            }
            if (symlink.getKey().contains("/../") || symlink.getKey().endsWith("/..")) {
                throw new IllegalArgumentException("input symlink path contained traversal");
            }
            if (symlink.getKey().contains("\0")) {
                throw new IllegalArgumentException("input symlink paths must not contain NUL characters");
            }
            if (symlink.getValue().contains("\0")) {
                throw new IllegalArgumentException("input symlink target paths must not contain NUL characters");
            }

            // removing double slashes before putting. ideally, our source data should already be clean...
            this.symlinks.put(normalizeValidPathString(symlink.getKey()), normalizeValidPathString(symlink.getValue()));
        }
    }

    protected static String removeDoubleSlashes(String pathString) {
        // duplicate (n) slashes are meaningless on linux and resolve to a single slash
        return pathString.replaceAll("//+", "/");
    }

    protected static String removeTrailingSlash(String pathString) {
        // trailing slashes after directories are usually harmless but hurt us when resolving with maps.
        if (pathString.length() > 1 && pathString.endsWith("/")) {
            return pathString.substring(0, pathString.length() - 1);
        }

        return pathString;
    }

    protected String normalizeValidPathString(String pathString) {
        return removeTrailingSlash(removeDoubleSlashes(pathString));
    }

    public ResolverPathHolder resolve(String inputPath) {
        if (!inputPath.startsWith("/")) {
            LOG.error("path [{}] is not absolute. refusing to resolve.", inputPath);
            throw new IllegalArgumentException("inputPath must be absolute (start with '/')");
        }

        ResolverPathHolder holder = new ResolverPathHolder(inputPath);
        resolve(holder);

        return holder;
    }

    /**
     * Tries to resolve all links to get to the final (actual) path.
     * @param holder helper object used as in-out
     */
    protected void resolve(ResolverPathHolder holder) {
        for (long i = 0; i < maxDepth; i++) {
            resolveFirstLink(holder);

            // only resolve until status is set away from INFLIGHT
            if (holder.getStatus() != ResolverStatus.INFLIGHT) {
                return;
            }
        }

        // if the link is still INFLIGHT, mark it as cyclic after all attempts are used up
        if (holder.getStatus() == ResolverStatus.INFLIGHT) {
            LOG.debug("resolver ran out of depth at [{}] after [{}] cycles", holder.getCurrentPath(), maxDepth);
            holder.setStatus(ResolverStatus.CYCLIC);
        }
    }

    // FIXME: need to cover certain special cases when resolving symlinks such as:
    //  - proc being special (https://unix.stackexchange.com/questions/197854/how-does-the-proc-pid-exe-symlink-differ-from-ordinary-symlinks)
    //  - (should probably ignore proc altogether and warn if a package tries to resolve anything within proc!

    /**
     * Method to resolve the first occurring link in th
     * @param holder helper object used as in-out
     */
    protected void resolveFirstLink(ResolverPathHolder holder) {
        // remove double slashes as they are meaningless
        String sanitizedInputPath = removeDoubleSlashes(holder.getCurrentPath());

        if (holder.getStatus() != ResolverStatus.INFLIGHT) {
            throw new IllegalArgumentException("Holder's status must be INFLIGHT. refusing to resolve object.");
        }

        // check that we weren't previously at this exact position
        if (holder.getPreviousPositions().contains(sanitizedInputPath)) {
            // we are running in circles! abort mission.
            holder.setStatus(ResolverStatus.CYCLIC);
            return;
        } else {
            holder.addPreviousPosition(sanitizedInputPath);
        }

        // split the sanitized input path at slashes
        String[] nodes = sanitizedInputPath.split(Pattern.quote("/"));
        nodes = Arrays.copyOfRange(nodes, 1, nodes.length);

        // this string will evolve to become our output
        String resolvedPath = sanitizedInputPath;

        // then add segments of the link, each time looking to resolve symlinks
        StringBuilder pathPart = new StringBuilder();
        for (String nodeName : nodes) {
            if (nodeName.isEmpty()) {
                throw new IllegalStateException("Can't happen: nodeName is empty");
            }

            if (nodeName.equals("..")) {
                // no symlink until now and traversal means we should go up one dir and keep resolving.
                if (pathPart.length() < 2) {
                    // meaning we are already at the level known to our resolver as root ("/"). can't continue, err out
                    holder.setStatus(ResolverStatus.BAD_TRAVERSAL);
                    return;
                }



                int index = pathPart.lastIndexOf("/");
                // temporarily append .. so we can easily replace it by index in resolvedPath
                pathPart.append("/..");
                int length = pathPart.length();
                // otherwise traverse (meaning delete the last directory, ignore the ".." and keep going)
                resolvedPath = resolvedPath.substring(0, index) + resolvedPath.substring(length);
                pathPart.delete(index, pathPart.length());

                // we know this can't be a symlink since we previously checked the part before "..". set and continue.
                holder.setCurrentPath(resolvedPath);
                continue;
            } else {
                pathPart.append("/");
                pathPart.append(nodeName);
            }

            // construct partial path for the actual symlink lookup via map
            String constructedPart = pathPart.toString();

            // sanity check construction
            if (!resolvedPath.startsWith(constructedPart)) {
                throw new IllegalStateException("Can't happen: resolving path doesn't contain a constructed part.");
            }

            // lookup
            if (symlinks.containsKey(constructedPart)) {
                // this partial path is a symlink. resolve it and replace it in the path.

                // get and sanity check target
                String target = Objects.requireNonNull(symlinks.get(constructedPart),
                        "The symlink map mustn't have null-valued entries.");

                if (!target.startsWith("/")) {
                    // if this symlink is relative, we need to append it in the correct way.
                    target = constructedPart.replaceAll("/[^/]*$", "") + '/' + target;
                }

                resolvedPath = resolvedPath.replaceFirst(Pattern.quote(constructedPart), target);

                holder.setCurrentPath(resolvedPath);

                // after replacing or err-ing out, we have resolved the first symlink and should return
                return;
            }
        }

        // if we got here, we could not resolve anything.
        if (holder.getStatus() == ResolverStatus.INFLIGHT) {
            holder.setStatus(ResolverStatus.DONE);
        }
    }
}
