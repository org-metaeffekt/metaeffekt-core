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
package org.metaeffekt.core.util;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Unifies handling with java.lang.Process class on a common abstraction-level. Supports dealing with the involved
 * streams and timeout handling.
 */
@Slf4j
public abstract class ExecUtils {

    public static final int TIMEOUT_NEVER = -1;

    public static final OutputStream STREAM_DEV_NULL = NullOutputStream.INSTANCE;

    private ExecUtils() {
    }

    @Deprecated // use ExecParam variant instead
    public static int waitForProcessToTerminate(Process exec, String execCommand) throws IOException {
        while (exec.isAlive()) {
            IOUtils.copy(exec.getInputStream(), System.out);
            IOUtils.copy(exec.getErrorStream(), System.out);
            waitIncrementForProcess(exec);
        }
        final int exitCode = exec.exitValue();

        log.debug("Process exited with code [{}] for command: {}", exitCode, execCommand);
        return exitCode;
    }

    public static ExecMonitor executeCommand(ExecParam execParam) throws IOException {
        final Process execProcess = Runtime.getRuntime().exec(execParam.getCommandParts(), execParam.getPenv(), execParam.getWorkingDir());
        return new ExecMonitor(execProcess, execParam);
    }

    public static ExecMonitor executeCommandAndWaitForProcessToTerminate(ExecParam execParam) throws IOException, InterruptedException {
        final ExecMonitor execMonitor = executeCommand(execParam);
        return waitForProcessToTerminate(execMonitor);
    }

    public static ExecMonitor waitForProcessToTerminate(ExecMonitor execMonitor) throws InterruptedException {
        long maxMillis = execMonitor.computeTimeoutMillis();

        final Process process = execMonitor.getProcess();
        final ExecParam execParam = execMonitor.getExecParam();

        do {
            manageStreams(execMonitor, process);

            if (process.isAlive()) {
                waitIncrementForProcess(process);
            }

            // check timout condition
            if (maxMillis != TIMEOUT_NEVER) {
                if (System.currentTimeMillis() > maxMillis) {
                    // wait another (last) iteration
                    waitIncrementForProcess(process);

                    // in case the process is still running
                    if (process.isAlive()) {
                        log.debug("Process timed out for command: {}", execParam.getCommandString());

                        // destroy the process if demanded
                        if (execParam.destroyOnTimeout) {
                            destroyProcess(execMonitor);
                        }
                    }
                }
            }

        } while (process.isAlive());

        execMonitor.exitCode = Optional.of(process.exitValue());
        execMonitor.status = ExecStatus.TERMINATED;

        log.debug("Process exited with code [{}] for command: {}", execMonitor.exitCode, execParam.getCommandString());

        return execMonitor;
    }

    private static void manageStreams(ExecMonitor execMonitor, Process process) {
        try {
            // NOTE: process.getInputStream() is an input stream connected to the output stream of the subprocess; see javadoc
            IOUtils.copy(process.getInputStream(), execMonitor.getOutputStreamSink());
        } catch (IOException e) {
            log.warn("Exception draining process output stream.", e);
        }

        try {
            // NOTE: process.getErrorStream() is an input stream connected to the error stream of the subprocess; see javadoc
            IOUtils.copy(process.getErrorStream(), execMonitor.getErrorStreamSink());
        } catch (IOException e) {
            log.warn("Exception draining error output stream.", e);
        }

        // NOTE: only an extension proposal; may provide the option to interact interactively with the executed command
        // IOUtils.copy(process.getOutputStream(), execParam.getInputStreamDrain());
    }

    private static void destroyProcess(ExecMonitor execMonitor) throws InterruptedException {
        log.debug("Destroying process for command: {}.", execMonitor.getExecParam().getCommandString());

        final Process process = execMonitor.process;
        process.destroy();

        // in case the process is still running after graceful destroy
        if (process.isAlive()) {
            // wait another iteration
            waitIncrementForProcess(process);
            process.destroyForcibly();
        }

        // manage streams (capturing further information)
        manageStreams(execMonitor, process);

        // mark monitor
        execMonitor.status = ExecStatus.DESTROYED;

        // notify invoker of the timeout
        throw new InterruptedException("Command [%s] timed out.");
    }

    private static void waitIncrementForProcess(Process exec) {
        try {
            exec.waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // do nothing; timeout and interrupts handled on a different level
        }
    }

    public static class ExecParam {

        @Getter
        final private String[] commandParts;

        @Getter
        final private String commandString;

        @Getter
        @Setter
        private File workingDir;

        @Getter
        @Setter
        private String[] penv = null;

        private OutputStream outputStreamSink = System.out;
        private OutputStream errorStreamSink = System.err;

        private long maxDuration = TIMEOUT_NEVER;
        private TimeUnit maxDurationUnit = null;

        private boolean destroyOnTimeout = true;

        public ExecParam(List<String> commandParts) {
            this(commandParts.toArray(new String[commandParts.size()]));
        }

        public ExecParam(String... commandParts) {
            this.commandParts = commandParts;
            this.commandString = Arrays.stream(this.commandParts).collect(Collectors.joining(" "));
        }

        @Override
        public String toString() {
            return commandString;
        }

        public void retainOutputs() {
            // NOTE: since output may be very long; this is dangerous in particular for the outputStreamSink
            //  a buffered implementation retaining the last 4096 bytes may be a solution
            this.outputStreamSink = new ByteArrayOutputStream();
            this.errorStreamSink = new ByteArrayOutputStream();
        }

        public void ignoreOutputs() {
            this.outputStreamSink = STREAM_DEV_NULL;
            this.errorStreamSink = STREAM_DEV_NULL;
        }

        public void retainErrorOutputs() {
            this.outputStreamSink = STREAM_DEV_NULL;
            this.errorStreamSink = new ByteArrayOutputStream();
        }

        public void destroyOnTimeout(boolean destroyOnTimeout) {
            this.destroyOnTimeout = destroyOnTimeout;
        }

        public void timeoutAfter(long duration, TimeUnit durationUnit) {
            this.maxDuration = duration;
            this.maxDurationUnit = durationUnit;
        }
    }

    public static class ExecMonitor {

        @Getter
        private final ExecParam execParam;

        @Getter
        private final Process process;

        @Getter
        public ExecStatus status;

        @Getter
        public Optional<Integer> exitCode = Optional.empty();

        public ExecMonitor(Process process, ExecParam execParam) {
            this.execParam = execParam;
            this.process = process;
        }

        public Optional<String> getErrorOutput() {
            return getStreamAsString(execParam.errorStreamSink);
        }

        public Optional<String> getOutput() {
            return getStreamAsString(execParam.outputStreamSink);
        }

        private Optional<String> getStreamAsString(OutputStream out) {
            if (out instanceof ByteArrayOutputStream) {
                return Optional.of(out.toString());
            } else {
                return Optional.empty();
            }
        }

        public long computeTimeoutMillis() {
            if (execParam.maxDuration == TIMEOUT_NEVER) {
                return TIMEOUT_NEVER;
            }
            return System.currentTimeMillis() + execParam.maxDurationUnit.convert(execParam.maxDuration, TimeUnit.MILLISECONDS);
        }

        public OutputStream getOutputStreamSink() {
            return execParam.outputStreamSink;
        }

        public OutputStream getErrorStreamSink() {
            return execParam.errorStreamSink;
        }
    }

    public enum ExecStatus {
        /**
         * The real process status is only known to the process. We do not claim to know anything more.
         */
        UNDETERMINED,

        /**
         * The process has been terminated. Success or failure is indicated by the exit code.
         */
        TERMINATED,

        /**
         * The process has been actively destroyed.
         */
        DESTROYED;
    }

    /**
     * This is a specific, but current pattern for using ExecUtils. The consumer must not specifically deal with
     * timeouts.
     *
     * @param execParam Parameter for execution of command.
     *
     * @return Returns the ExecMonitor conveying more details on the execution.
     *
     * @throws IOException An IOException is thrown in case of failure or timeout.
     */
    public static ExecMonitor executeAndThrowIOExceptionOnFailure(ExecParam execParam) throws IOException {
        try {
            final ExecMonitor execMonitor = executeCommandAndWaitForProcessToTerminate(execParam);
            if (execMonitor.getExitCode().isPresent()) {
                final int exitCode = execMonitor.getExitCode().get();
                if (exitCode != 0) {
                    log.debug("Execution failed with exit code [{}] for command: {}", exitCode, execParam.getCommandString());
                    // in case the error output was recorded, append it to the debug log
                    if (execMonitor.getErrorOutput().isPresent()) {
                        Arrays.stream(execMonitor.getErrorOutput().get().split("\\n")).forEach(log::debug);
                    }
                    throw new IOException(String.format("Execution failed with exit code [%s] for command: %s", exitCode, execParam.getCommandString()));
                } else {
                    // exitCode == 0; all fine; do nothing
                }
                return execMonitor;
            } else {
                throw new IOException(String.format("Execution in undetermined state for command: %s", execParam.getCommandString()));
            }
        } catch (InterruptedException e) {
            log.debug("Execution timed out for for command: {}", execParam.getCommandString());
            throw new IOException(String.format("Execution timed out for command: %s", execParam.getCommandString()));
        }
    }
}
