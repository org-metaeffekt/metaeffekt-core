/**
 * Copyright 2009-2018 the original author or authors.
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
package org.metaeffekt.core.maven.kernel.logging;

import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

public class MavenLogger extends MarkerIgnoringBase implements Logger {

    private static final long serialVersionUID = -5633359196835524947L;

    private Log mavenLog;

    public MavenLogger(Log mavenLog) {
        this.mavenLog = mavenLog;
    }

    @Override
    public boolean isTraceEnabled() {
        return isDebugEnabled();
    }

    @Override
    public void trace(String msg) {
        debug(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        debug(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        debug(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        debug(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return mavenLog.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            mavenLog.debug(msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (isDebugEnabled()) {
            mavenLog.debug(MessageFormatter.format(format, arg).getMessage());
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            mavenLog.debug(MessageFormatter.format(format, arg1, arg2).getMessage());
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (isDebugEnabled()) {
            mavenLog.debug(MessageFormatter.arrayFormat(format, arguments).getMessage());
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            mavenLog.debug(msg, t);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return mavenLog.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled()) {
            mavenLog.info(msg);
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (isInfoEnabled()) {
            mavenLog.info(MessageFormatter.format(format, arg).getMessage());
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            mavenLog.info(MessageFormatter.format(format, arg1, arg2).getMessage());
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (isInfoEnabled()) {
            mavenLog.info(MessageFormatter.arrayFormat(format, arguments).getMessage());
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            mavenLog.info(msg, t);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return mavenLog.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled()) {
            mavenLog.warn(msg);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if (isWarnEnabled()) {
            mavenLog.warn(MessageFormatter.format(format, arg).getMessage());
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (isWarnEnabled()) {
            mavenLog.warn(MessageFormatter.arrayFormat(format, arguments).getMessage());
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            mavenLog.warn(MessageFormatter.format(format, arg1, arg2).getMessage());
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            mavenLog.warn(msg, t);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return mavenLog.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled()) {
            mavenLog.error(msg);
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (isErrorEnabled()) {
            mavenLog.error(MessageFormatter.format(format, arg).getMessage());
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            mavenLog.error(MessageFormatter.format(format, arg1, arg2).getMessage());
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (isErrorEnabled()) {
            mavenLog.error(MessageFormatter.arrayFormat(format, arguments).getMessage());
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (isErrorEnabled()) {
            mavenLog.error(msg, t);
        }
    }

}
