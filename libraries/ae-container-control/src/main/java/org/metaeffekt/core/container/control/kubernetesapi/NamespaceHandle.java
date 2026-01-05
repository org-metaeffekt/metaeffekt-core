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
package org.metaeffekt.core.container.control.kubernetesapi;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Creates a namespace and acts as the holder of this resource.<br>
 * Closure of this object results in deletion of the namespace (and so its content as per propagation policy).<br>
 * It is not truly threadsafe. It may just delete the namespace while executors are still using it, meaning their
 * underlying container may just get yanked. Closure of underlying resources is left to the user.
 */
@SuppressWarnings("unused")
public class NamespaceHandle implements AutoCloseable {
    /**
     * UUID to create a unique namespace for runtime.
     */
    private final UUID myId = UUID.randomUUID();

    /**
     * Custom namespace for this controller. Note it may have a maximum total length of 63 characters.
     */
    private final String namespace = "ae-container-control-temp-" + myId;

    /**
     * Starting time to label the current namespace with.
     */
    private final String utcStartTime = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    private final Config config;

    /**
     * Identification for this tool. For use with the "app.kubernetes.io/managed-by" label.
     */
    private static final String toolName = "ae-container-control";

    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new namespace and this handle object.
     *
     * @param kubeconfig configuration for the kubernetes client. may be null.
     */
    public NamespaceHandle(String kubeconfig) {
        this(Config.fromKubeconfig(kubeconfig));
    }

    /**
     * Creates a new namespace and this handle object.
     *
     * @param kubeconfig configuration for the kubernetes client. may be null.
     */
    public NamespaceHandle(Config kubeconfig) {
        this.config = kubeconfig;
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(kubeconfig).build()) {
            // create namespace with custom details
            if (client.namespaces().withName(namespace).get() == null) {
                // create the namespace from scratch
                Namespace toCreate = new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(namespace)
                        .withLabels(new HashMap<String, String>() {{
                            put("app.kubernetes.io/managed-by", toolName);
                            put("runtimeCreatedAt", utcStartTime.replaceAll("[^a-zA-Z0-9_.\\-]", "_"));
                        }})
                        .endMetadata()
                        .build();

                client.resource(toCreate).create();
            } else {
                // namespace already exists. mustn't happen
                throw new IllegalStateException("Can't happen: Namespace of newly, randomly created name ["
                        + namespace +
                        " already exists, so it's probably managed by another instance.");
            }
        }
    }

    @Override
    public void close() {
        if (!closed.getAndSet(true)) {
            // try to clean up our own object as best as we can
            try (KubernetesClient client = new KubernetesClientBuilder().withConfig(this.config).build()) {
                client.namespaces().withName(namespace).withGracePeriod(0).delete();
            }
        }
    }

    /**
     * Gets the name of the namespace which is managed by this object.
     * @return name of the managed namespace
     */
    public String getName() {
        return namespace;
    }

    public synchronized boolean getClosed() {
        return closed.get();
    }
}
