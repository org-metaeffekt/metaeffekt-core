/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.container.control.kubernetesapi;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Ignore("Requires a local Kubernetes (minikube or similar) instance.")
public class KubernetesCommandExecutorTest {

    @Test
    public void testContainerCreationHelloWorld() throws Exception {
        try (KubernetesCommandExecutor exec = new KubernetesCommandExecutor("docker.io/debian:latest")) {
            try (KubernetesContainerCommandProcess commandProcess =
                         exec.executeCommand("echo", "hello devil of cursed commands!")) {

                int returnValue = commandProcess.waitFor();
                if (returnValue != 0) {
                    fail("Failure executing the command.");
                }

                String output = new String(commandProcess.getAllStdOut(), StandardCharsets.UTF_8);
                if (!output.contains("hello devil of cursed commands!")) {
                    fail("output not correct");
                }
            }
        }
    }

    @Test
    public void testContainerCreationCursedCommands() throws Exception {
        try (KubernetesCommandExecutor exec = new KubernetesCommandExecutor("debian:latest")) {
            try (KubernetesContainerCommandProcess commandProcess = exec.executeCommand("cat", "/dev/urandom")) {
                assertFalse("Timeout wasn't reached against expectation.",
                        commandProcess.waitFor(2, TimeUnit.SECONDS)
                );
            }
        }
    }

    @Test
    public void testContainerCreationWithLargeBufferedOutput() throws Exception {
        try (KubernetesCommandExecutor exec = new KubernetesCommandExecutor("debian:latest")) {
            try (KubernetesContainerCommandProcess commandProcess =
                         exec.executeCommand("head", "-c", "536870912", "/dev/urandom")) {
                // should finish on any reasonably fast machine
                assertTrue("Timeout wasn't reached against expectation.",
                        commandProcess.waitFor(30, TimeUnit.SECONDS)
                );

                assertEquals(0, commandProcess.exitValue());

                byte[] output = commandProcess.getAllStdOut();
                assertEquals(536870912, output.length);
            }
        }
    }
}
