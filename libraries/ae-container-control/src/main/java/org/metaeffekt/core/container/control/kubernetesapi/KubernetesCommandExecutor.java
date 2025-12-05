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
package org.metaeffekt.core.container.control.kubernetesapi;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.metaeffekt.core.container.control.exception.CommandExecutionFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstracts an "environment for running commands in".
 * <br>
 * Must guarantee that the environment doesn't change erratically between commands unless modified by the commands
 * themselves. Therefore we maintain a dependency on a single pod / container.
 */
@SuppressWarnings("unused")
public class KubernetesCommandExecutor implements AutoCloseable {
    /**
     * A default namespace for use with this tool. This may (should) not be the "default" namespace of kubernetes.
     */
    @SuppressWarnings("unused")
    public static final String defaultNamespace = "ae-container-control";
    public static final long waitSecondsForPodCreation = 60;
    /**
     * How long to wait for before warning the user (via log warn message) when pod is pending for a while.
     */
    public static final long secondsToWarnOnPodPending = 30;
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesCommandExecutor.class);
    private final KubernetesClient client;
    private final Pod reservedPod;
    private boolean closed = false;

    /**
     * Creates a new object.
     *
     * @param namespace       namespace to use for creation of resources
     * @param imageIdentifier identifier of the container image to use in creation
     * @throws CommandExecutionFailed on failure to create the pod
     */
    public KubernetesCommandExecutor(String namespace, String imageIdentifier) throws CommandExecutionFailed {
        this(new ConfigBuilder().build(), namespace, imageIdentifier);
    }

    /**
     * Creates a new object.
     *
     * @param kubeconfig      kubeconfig for client creation, null for default config
     * @param namespaceName   namespace to use for creation of resources
     * @param imageIdentifier identifier of the container image to use in creation
     * @throws CommandExecutionFailed on failure to create the pod
     */
    public KubernetesCommandExecutor(Config kubeconfig, String namespaceName, String imageIdentifier)
            throws CommandExecutionFailed {
        this(kubeconfig, namespaceName, imageIdentifier, null);
    }

    /**
     * Creates a new object.
     *
     * @param kubeconfig      kubeconfig for client creation, null for default config
     * @param namespaceName   namespace to use for creation of resources
     * @param imageIdentifier identifier of the container image to use in creation
     * @param envVars         environment for command execution
     * @throws CommandExecutionFailed on failure to create the pod
     */
    public KubernetesCommandExecutor(
            Config kubeconfig,
            String namespaceName,
            String imageIdentifier,
            List<EnvVar> envVars
    ) throws CommandExecutionFailed {
        KubernetesClient client = null;
        try {
            // TODO: constructor overloads that allow configuration of the client (such as endpoint address, port)
            client = new KubernetesClientBuilder().withConfig(kubeconfig).build();

            this.client = client;
            this.reservedPod = getPod(client, namespaceName, imageIdentifier, UUID.randomUUID(), envVars);
        } catch (Exception e) {
            LOG.debug(
                    "Constructor of [{}] failed for image identifier [{}].",
                    this.getClass().getSimpleName(),
                    imageIdentifier
            );

            // manual closure only on exception; can't run regular close cause reservedPod and such are not initialized
            if (client != null) {
                client.close();
            }

            throw e;
        }
    }

    // TODO: consider a mode for running pods behind VPN for internet access?

    /**
     * Helper method for ensuring correct pod creation.
     *
     * @param client          the client to create the pod with
     * @param namespaceName   name of the namespace to use for resource creation
     * @param imageIdentifier the image identifier to use for this pod
     * @param runnerId        a runner id, a UUID unique to this pod
     * @param envVars         environment for command execution
     * @return returns the created pod object as returned by the api's {@link PodResource#create()}
     * @throws KubernetesClientTimeoutException if pod creation fails with timeout
     * @throws CommandExecutionFailed           on failure to create the pod
     */
    protected static Pod getPod(
            KubernetesClient client,
            String namespaceName,
            String imageIdentifier,
            UUID runnerId,
            List<EnvVar> envVars
    ) throws KubernetesClientTimeoutException, CommandExecutionFailed {

        // prepare our own namespace for easier management and deletion of leftovers in case of any issues
        if (client.namespaces().withName(namespaceName).get() == null) {
            // create the namespace from scratch
            Namespace namespace =
                    new NamespaceBuilder().withNewMetadata().withName(namespaceName).endMetadata().build();

            client.resource(namespace).create();
        }

        // prepare names
        final String podName = "command-executor-pod-" + runnerId;
        final String containerName = "command-executor-container-" + runnerId;

        final int failureThreshold = 3;

        // create new pod, return pod object returned by creation
        Pod podPrototype = new PodBuilder()
                .withNewMetadata()
                .withName(podName)
                .withNamespace(namespaceName)
                .endMetadata()

                .withNewSpec()
                // death of a container destroys our environment's state. thus the entire pod must fail
                .withRestartPolicy("Never")
                // crude way of ensuring pods get stopped at some point if the jvm terminated unexpectedly
                .withActiveDeadlineSeconds(60L * 60 * 4)

                .addNewContainer()
                .withName(containerName)
                .withEnv(envVars)
                .withImage(imageIdentifier)
                .withStdin()
                .endContainer()

                .endSpec()
                .build();

        /*
        NOTE: not sure whether this is the same object from earlier, so we use the returned one.
         */
        final Pod pod = client.pods().resource(podPrototype).create();

        /*
        NOTE: we wait until the pod for this image exits "Pending" phase instead of potentially crashing on
        a later command (which may have much shorter timeouts due to expecting the pod to be ready).
        we also log if a pod is taking longer than we'd prefer to start up.
         */
        final AtomicBoolean shouldLog = new AtomicBoolean(true);
        final long startTime = System.currentTimeMillis();
        final AtomicReference<String> lastSeenPhase = new AtomicReference<>(null);
        client.pods().resource(pod).waitUntilCondition(
                p -> {
                    if (shouldLog.get()) {
                        final long current = System.currentTimeMillis();

                        long secondsToWarnFloored = Math.min(secondsToWarnOnPodPending, waitSecondsForPodCreation - 2);
                        if (((current - startTime) / 1000) > secondsToWarnFloored &&
                                shouldLog.getAndSet(false)) {
                            // inform user that a pod is taking a long time to start
                            LOG.info(
                                    "Pod for image spec [{}] has been pending for more than [{}] seconds." +
                                            " Could indicate slow download or" +
                                            " unreachable image id (in which case we're stuck forever).",
                                    imageIdentifier,
                                    secondsToWarnOnPodPending
                            );
                        }
                    }

                    // actual switching logic: check whether we're still "Pending"
                    final String phase = p.getStatus().getPhase();
                    lastSeenPhase.set(phase);
                    // stop waiting as soon as state is not "Pending" any more.
                    return !"Pending".equals(phase);
                },
                /*
                this is a balance of application responseiveness (cancelling invalid image names or non-matching ISA)
                and being tolerant of slow machines / internet connections (pulls for larger images may take longer).
                 */
                waitSecondsForPodCreation, TimeUnit.SECONDS
        );

        if (!"Running".equals(lastSeenPhase.get())) {
            // pod.getStatus().getPhase() does not return the same, it seems, so we get phase from inside our wait
            LOG.debug(
                    "Last seen state in checker is [{}] for podname [{}] with identifier [{}]. Will throw!",
                    lastSeenPhase.get(),
                    podName,
                    imageIdentifier
            );

            // insta-delete the pod
            try {
                LOG.debug("Instantly deleting pod [{}] after it didn't transition to running phase.", podName);
                client.pods().resource(pod).withGracePeriod(0).delete();
            } catch (KubernetesClientException e) {
                LOG.debug("Ignoring client exception from forceful pod deletion on invalid pod [{}].", podName, e);
            }

            throw new CommandExecutionFailed("Pod not 'Running' after startup procedure.");
        }

        LOG.debug(
                "Returning pod after seemingly successful creation: namespace=[{}], name=[{}], identifier [{}]",
                namespaceName,
                podName,
                imageIdentifier
        );
        return pod;
    }

    /**
     * Executes a command in the provisioned container.
     *
     * @param command the command to run inside the container
     * @return an object representing the runnning process as a resource
     */
    public KubernetesContainerCommandProcess executeCommand(String... command) {
        PodResource podResource = client.pods().resource(reservedPod);

        // launch the new command
        return new KubernetesContainerCommandProcess(podResource, command);
    }

    /**
     * Copies a file from the pod to this machine using the api.<br> Library has bad documentation of the copy call but
     * appears it should return true on success. Probably best look at how the kubectl copy command works to draw
     * conclusions. It appears to throw exceptions on failure.
     * <br>
     * Note that while the fabric8 api seems to infer a filename if a given destination is a directory but input is a
     * file, it might be cleaner to specify a file as destination to avoid fringe bugs with odd input filenames.
     *
     * @param pathInContainer the path for the file in the container
     * @param destination     the destination file on the host machine
     * @return true if the copy was successful
     */
    public boolean downloadFile(String pathInContainer, Path destination) {
        return client.pods().resource(reservedPod).file(pathInContainer).copy(destination);
    }

    public InputStream readFile(String pathInContainer) {
        return client.pods().resource(reservedPod).file(pathInContainer).read();
    }

    public boolean uploadFile(InputStream input, String pathInContainer) {
        return client.pods().resource(reservedPod).file(pathInContainer).upload(input);
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;

            // delete the pod (and container)
            client.resource(this.reservedPod).withGracePeriod(0).delete();
            // close our client endpoint
            client.close();
        }
    }
}
