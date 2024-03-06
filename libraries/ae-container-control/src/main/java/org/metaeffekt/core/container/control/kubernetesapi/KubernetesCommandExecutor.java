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

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.PodResource;

import java.nio.file.Path;
import java.util.UUID;

public class KubernetesCommandExecutor implements AutoCloseable {
    private boolean closed = false;

    private final KubernetesClient client;
    private final Pod reservedPod;

    /**
     * Note that changing this will create another namespace on clients, old namespace may require manual deletion.
     */
    public static final String namespaceName = "ae-container-control";

    /**
     * Creates a new object.
     * @param imageIdentifier identifier of the container image to use in creation
     */
    public KubernetesCommandExecutor(String imageIdentifier) {
        KubernetesClient client = null;
        try {
            // TODO: constructor overloads that allow configuration of the client (such as endpoint addredd, port)
            client = new KubernetesClientBuilder().build();

            this.client = client;
            this.reservedPod = getPod(client, imageIdentifier);
        } catch (Exception e) {
            // manual closure only on exception
            if (client != null) {
                client.close();
            }
            throw new RuntimeException("Pod creation failed", e);
        }
    }

    protected static Pod getPod(KubernetesClient client, String imageIdentifier) {
        return getPod(client, imageIdentifier, UUID.randomUUID());
    }

    /**
     * Helper method for ensuring correct pod creation.
     * @param client the client to create the pod with
     * @param imageIdentifier the image identifier to use for this pod
     * @param runnerId a runner id, a UUID unique to this pod
     * @return returns the created pod object as returned by the api's {@link PodResource#create()}
     */
    protected static Pod getPod(
            KubernetesClient client,
            String imageIdentifier,
            UUID runnerId) {

        // prepare our own namespace for easier management and deletion of leftovers in case of any issues
        if (client.namespaces().withName(namespaceName).get() == null) {
            // create the namespace from scratch
            Namespace namespace = new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(namespaceName)
                    .endMetadata()
                    .build();

            client.resource(namespace).create();
        }

        // prepare names
        final String podName = "command-executor-pod-" + runnerId;
        final String containerName = "command-executor-container-" + runnerId;

        // create new pod, return pod object returned by creation
        Pod podPrototype = new PodBuilder()
                .withNewMetadata()
                .withName(podName)
                .withNamespace(namespaceName)
                .endMetadata()
                .withNewSpec()
                .withRestartPolicy("Never")
                // crude way of ensuring pods get stopped at some point if the jvm terminated unexpectedly
                .withActiveDeadlineSeconds(60L * 60 * 4)
                .addNewContainer()
                .withName(containerName)
                .withImage(imageIdentifier)
                .withStdin()
                .endContainer()
                .endSpec()
                .build();

        // not sure whether this is the same object from earlier so i'll just capture this for handling
        return client.pods().resource(podPrototype).create();
    }

    /**
     * Executes a command in the provisioned container.
     * @param command the command to run inside the container
     * @return an object representing the runnning process as a resource
     */
    public KubernetesContainerCommandProcess executeCommand(String... command) {
        PodResource podResource = client.pods().resource(reservedPod);

        // launch the new command
        return new KubernetesContainerCommandProcess(podResource, command);
    }

    /**
     * Copies a file from the pod to this machine using the api.<br>
     * Library has bad documentation of the copy call but appears it should return true on success.
     * Probably best look at how the kubectl copy command works to draw conclusions. It appears to throw exceptions on
     * failure.
     * @param pathInContainer the path for the file in the container
     * @param destination the destination on the host machine
     * @return true if the copy was successful
     */
    public boolean downloadFile(String pathInContainer, Path destination) {
        return client.pods().resource(reservedPod).file(pathInContainer).copy(destination);
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
