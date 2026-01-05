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
package org.metaeffekt.core.container.control;

import org.metaeffekt.core.container.control.exception.CommandExecutionFailed;
import org.metaeffekt.core.container.control.kubernetesapi.KubernetesCommandExecutor;
import org.metaeffekt.core.container.control.kubernetesapi.KubernetesContainerCommandProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class ExecutorUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ExecutorUtils.class);

    public static void demandSuccess(KubernetesCommandExecutor executor,
                                     String[] command,
                                     long time,
                                     TimeUnit timeUnit) throws CommandExecutionFailed {
        final int exitValue;
        final String stdout;
        final String stderr;
        try (KubernetesContainerCommandProcess process = executor.executeCommand(command)) {
            process.waitFor(time, timeUnit);
            exitValue = process.exitValue();
            stdout = new String(process.getAllStdOut(), StandardCharsets.UTF_8);
            stderr = new String(process.getAllStdErr(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CommandExecutionFailed("Failed to execute: execution failed with exception", e);
        }
        if (exitValue != 0) {
            LOG.debug("Failed command stdout: [{}]", stdout);
            LOG.debug("Failed command stderr: [{}]", stderr);
            throw new CommandExecutionFailed("Failed to execute: command failed with exit value " + exitValue);
        }
    }
}
