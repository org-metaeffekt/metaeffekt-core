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
package org.metaeffekt.core.util;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ExecUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ExecUtils.class);

    public static int executeCommandAndWaitForProcessToTerminate(List<String> commandParts, String[] penv, File dir) throws IOException {
        final String commandAsString = commandParts.stream().collect(Collectors.joining(" "));
        System.out.println(commandAsString);
        return waitForProcessToTerminate(Runtime.getRuntime().exec(commandParts.toArray(new String[commandParts.size()]), penv, dir),
                commandAsString);
    }

    public static int waitForProcessToTerminate(Process exec, String execCommand) throws IOException {
        while (exec.isAlive()) {
            IOUtils.copy(exec.getInputStream(), System.out);
            IOUtils.copy(exec.getErrorStream(), System.out);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
        final int exitCode = exec.exitValue();

        LOG.debug("Process {} exited with code {}.", execCommand, exitCode);
        return exitCode;
    }

}
