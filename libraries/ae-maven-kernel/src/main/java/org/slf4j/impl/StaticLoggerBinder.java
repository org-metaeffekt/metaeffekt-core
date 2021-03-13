/**
 * Copyright 2009-2021 the original author or authors.
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
package org.slf4j.impl;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.metaeffekt.core.maven.kernel.logging.MavenLoggerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {

    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
    private final static String LOGGER_FACTORY_CLASS_STR = MavenLoggerFactory.class.getSimpleName();
    private MavenLoggerFactory loggerFactory;
    private Log mavenLog;

    private StaticLoggerBinder() {
    }

    /**
     * Return the singleton of this class.
     *
     * @return the StaticLoggerBinder singleton
     */
    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    @Override
    public ILoggerFactory getLoggerFactory() {

        if (loggerFactory == null) {
            if (mavenLog == null) {
                mavenLog = new SystemStreamLog();
            }
            loggerFactory = new MavenLoggerFactory(mavenLog);
        }

        return loggerFactory;
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return LOGGER_FACTORY_CLASS_STR;
    }

    public Log getMavenLog() {
        return mavenLog;
    }

    public void setMavenLog(Log mavenLog) {
        this.mavenLog = mavenLog;
    }

}
