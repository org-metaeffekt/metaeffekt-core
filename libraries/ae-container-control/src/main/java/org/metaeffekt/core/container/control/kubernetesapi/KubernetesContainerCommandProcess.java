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

import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("unused")
public class KubernetesContainerCommandProcess extends Process implements AutoCloseable {
    public static final Logger LOG = LoggerFactory.getLogger(KubernetesContainerCommandProcess.class);

    private boolean closed = false;

    private final ExecWatch watch;

    /**
     * stdout from process.
     */
    private final InputStream sout;
    /**
     * stderr from process.
     */
    private final InputStream serr;
    /**
     * stdin from process.
     */
    private final OutputStream sin;

    // if redirections for standard out and standard in were used, keep them so they can be closed cleanly
    /**
     * Stream for collectiong stdout if buffered.
     * @see #bufferType
     */
    private final ByteArrayOutputStream standardOutRedirected;
    /**
     * Stream for collectiong stderr if buffered.
     * @see #bufferType
     */
    private final ByteArrayOutputStream standardErrRedirected;

    /**
     * Type of buffering used; either the output is streamed or buffered into a growing buffer in this class.
     * @see Buffering
     */
    private final Buffering bufferType;

    /**
     * Store the command that is being executed for later reference.
     */
    private final String[] command;

    /**
     * Types of buffering that this class may support.
     */
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

    /**
     * Runs the given command and configures this object to act as a interface with the running instance.
     * @param runnerPod to run the command inside of
     * @param command command to run
     */
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
    public void close() throws IOException {
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

    /**
     * Type of buffering used with this process.
     * @return type of buffering
     */
    public Buffering getBufferType() {
        return bufferType;
    }

    protected void ensureBuffering() {
        if (bufferType != Buffering.MEM_BUF) {
            throw new IllegalStateException("Buffering is not turned on.");
        }
    }

    /**
     * Gets stdout buffer with whatever has been written so far.
     * <br>
     * Does not wait for the process to complete!
     * @return whatever has been written so far
     */
    public byte[] getAllStdOut() {
        ensureBuffering();
        if (watch.exitCode().getNow(-1) == -1) {
            LOG.debug("Called getAllStdOut before command exited; Rarely done intentionally; may indicate bugs!");
        }
        return this.standardOutRedirected.toByteArray();
    }

    /**
     * Gets stderr buffer with whatever has been written so far.
     * <br>
     * Does not wait for the process to complete!
     * @return whatever has been written so far
     */
    public byte[] getAllStdErr() {
        ensureBuffering();
        if (watch.exitCode().getNow(-1) == -1) {
            LOG.debug("Called getAllStdErr before command exited; Rarely done intentionally; may indicate bugs!");
        }
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
