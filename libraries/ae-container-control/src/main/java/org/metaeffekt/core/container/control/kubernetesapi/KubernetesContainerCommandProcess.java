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

import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("unused")
public class KubernetesContainerCommandProcess extends Process implements AutoCloseable {
    private boolean closed = false;

    private final ExecWatch watch;

    private final InputStream sout;
    private final InputStream serr;
    private final OutputStream sin;

    // if redirections for standard out and standard in were used, keep them so they can be closed cleanly
    private final ByteArrayOutputStream standardOutRedirected;
    private final ByteArrayOutputStream standardErrRedirected;

    private final Buffering bufferType;

    /**
     * Store the command that is being executed for later reference.
     */
    private final String[] command;

    public enum Buffering {
        /**
         * No buffering, expose whatever streams {@link #watch} gives us raw.
         */
        NONE,
        /**
         * Automatigally consume streams to a byte buffer for easy access after a command has completed.<br>
         * This may come with some limitations, such as writes becoming blocking after slightly less than 2G of output.
         * Use this with caution if you expect very large logs.
         */
        MEM_BUF
    }

    public KubernetesContainerCommandProcess(PodResource runnerPod, String... command) {
        this.standardOutRedirected = new ByteArrayOutputStream();
        this.standardErrRedirected = new ByteArrayOutputStream();

        this.command = command;
        this.watch = runnerPod
                .redirectingInput()
                .writingOutput(standardOutRedirected)
                .writingError(standardErrRedirected)
                .exec(command);

        this.sin = watch.getInput();
        this.sout = watch.getOutput();
        this.serr = watch.getError();
        this.bufferType = Buffering.MEM_BUF;
    }

    @Override
    public OutputStream getOutputStream() {
        return sin;
    }

    @Override
    public InputStream getInputStream() {
        return sout;
    }

    @Override
    public InputStream getErrorStream() {
        return serr;
    }

    @Override
    public int waitFor() throws InterruptedException {
        return watch.exitCode().join();
    }

    @Override
    public int exitValue() throws IllegalThreadStateException {
        int exitValue = watch.exitCode().getNow(-1);
        if (exitValue == -1) {
            throw new IllegalThreadStateException("Program hasn't terminated yet.");
        }
        return exitValue;
    }

    @Override
    public void destroy() {
        try {
            // the api doesn't seem to provide a way to exit forcibly, so we close gracefully.
            // should the process absolutely need to end, we need to delete and reinstantiate the pod
            watch.close();
            close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            closed = true;

            // close redirects if needed
            if (standardOutRedirected != null) {
                standardOutRedirected.close();
            }
            if (standardErrRedirected != null) {
                standardErrRedirected.close();
            }

            if (sin != null) {
                sin.close();
            }
            if (sout != null) {
                sout.close();
            }
            if (serr != null) {
                serr.close();
            }
            watch.close();
        }
    }

    public Buffering getBufferType() {
        return bufferType;
    }

    public void ensureBuffering() {
        if (bufferType != Buffering.MEM_BUF) {
            throw new IllegalStateException("Buffering is not turned on.");
        }
    }

    public byte[] getAllStdOut() {
        ensureBuffering();
        return this.standardOutRedirected.toByteArray();
    }

    public byte[] getAllStdErr() {
        ensureBuffering();
        return this.standardErrRedirected.toByteArray();
    }

    /**
     * Gets the command that is being executed.
     * @return the command that was executed
     */
    public String[] getCommand() {
        return command;
    }
}
