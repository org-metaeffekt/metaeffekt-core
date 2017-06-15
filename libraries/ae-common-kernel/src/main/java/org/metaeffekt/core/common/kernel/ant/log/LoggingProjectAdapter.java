/**
 * Copyright 2009-2017 the original author or authors.
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
package org.metaeffekt.core.common.kernel.ant.log;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * <p>
 * The {@link LoggingProjectAdapter} bridges Ant and Commons Logging in 
 * respect to harmonize logging.
 *
 * Since Ant and Commons Logging are not equivalent and specific to Ant tasks
 * an escalation mechanism is supported. In particular when Ant logs to
 * verbose level the message can be escalated to another log level and
 * the build can be caused to fail.
 * </p>
 *
 * @author Karsten Klein
 */
public class LoggingProjectAdapter extends Project {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingProjectAdapter.class);

    private boolean escalate = true;

    private Set<String> warnEscalationTerms;
    private Set<String> errorEscalationTerms;

    private boolean failOnWarnEscalation = false;
    private boolean failOnErrorEscalation = true;

    @Override
    public void log(String message, int msgLevel) {
        if (escalateIfNecessary(message)) {
            return;
        }

        if (msgLevel == Project.MSG_VERBOSE) {
            LOG.debug(message);
            return;
        }
        if (msgLevel == Project.MSG_DEBUG) {
            LOG.debug(message);
            return;
        }
        if (msgLevel == Project.MSG_INFO) {
            LOG.info(message);
            return;
        }
        if (msgLevel == Project.MSG_WARN) {
            LOG.warn(message);
            return;
        }
        if (msgLevel == Project.MSG_ERR) {
            LOG.error(message);
            return;
        }
    }

    private boolean escalateIfNecessary(String message) {
        if (escalate) {
            if (errorEscalationTerms != null) {
                for (String term : errorEscalationTerms) {
                    if (message.contains(term)) {
                        LOG.error(message);
                        if (failOnErrorEscalation) {
                            throw new EscalationException(message);
                        }
                        return true;
                    }
                }
            }
            if (warnEscalationTerms != null) {
                for (String term : warnEscalationTerms) {
                    if (message.contains(term)) {
                        LOG.warn(message);
                        if (failOnWarnEscalation) {
                            throw new EscalationException(message);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void log(String message, Throwable throwable, int msgLevel) {
        if (escalateIfNecessary(message)) {
            return;
        }
        if (msgLevel == Project.MSG_VERBOSE) {
            LOG.debug(message, throwable);
            return;
        }
        if (msgLevel == Project.MSG_DEBUG) {
            LOG.debug(message, throwable);
            return;
        }
        if (msgLevel == Project.MSG_INFO) {
            LOG.info(message, throwable);
            return;
        }
        if (msgLevel == Project.MSG_WARN) {
            LOG.warn(message, throwable);
            return;
        }
        if (msgLevel == Project.MSG_ERR) {
            LOG.error(message, throwable);
            return;
        }
    }

    @Override
    public void log(String message) {
        LOG.info(message);
    }

    @Override
    public void log(Target target, String message, int msgLevel) {
        log(message, msgLevel);
    }

    @Override
    public void log(Target target, String message, Throwable throwable, int msgLevel) {
        log(message, throwable, msgLevel);
    }

    @Override
    public void log(Task task, String message, int msgLevel) {
        log(message, msgLevel);
    }

    @Override
    public void log(Task task, String message, Throwable throwable, int msgLevel) {
        super.log(message, throwable, msgLevel);
    }

    public boolean isEscalate() {
        return escalate;
    }

    public void setEscalate(boolean escalate) {
        this.escalate = escalate;
    }

    public Set<String> getWarnEscalationTerms() {
        return warnEscalationTerms;
    }

    public void setWarnEscalationTerms(Set<String> warnEscalationTerms) {
        this.warnEscalationTerms = warnEscalationTerms;
    }

    public Set<String> getErrorEscalationTerms() {
        return errorEscalationTerms;
    }

    public void setErrorEscalationTerms(Set<String> errorEscalationTerms) {
        this.errorEscalationTerms = errorEscalationTerms;
    }

    public boolean isFailOnWarnEscalation() {
        return failOnWarnEscalation;
    }

    public void setFailOnWarnEscalation(boolean failOnWarnEscalation) {
        this.failOnWarnEscalation = failOnWarnEscalation;
    }

    public boolean isFailOnErrorEscalation() {
        return failOnErrorEscalation;
    }

    public void setFailOnErrorEscalation(boolean failOnErrorEscalation) {
        this.failOnErrorEscalation = failOnErrorEscalation;
    }

    public Logger getLog() {
        return LOG;
    }

}
