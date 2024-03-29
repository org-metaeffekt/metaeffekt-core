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
package org.metaeffekt.core.maven.kernel.log;

import org.apache.commons.logging.LogFactory;
import org.apache.maven.plugin.logging.Log;
import org.slf4j.impl.StaticLoggerBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The adapter enables to integrate commons logging with Maven Log.
 *
 * @author Karsten Klein
 */
public class MavenLogAdapter {

    private static List<Log> LOGS = Collections.synchronizedList(new ArrayList<Log>());

    /**
     * Initializes the {@link LogFactory} to use the {@link MavenLogAdapter}. The
     * specified {@link org.apache.maven.plugin.logging.Log} instance is used when
     * later initializing the individual log instances.
     *
     * @param log The {@link org.apache.maven.plugin.logging.Log} instance to use.
     */
    public static void initialize(Log log) {
        LOGS.add(StaticLoggerBinder.getSingleton().getMavenLog());
        StaticLoggerBinder.getSingleton().setMavenLog(log);
    }

    /**
     * Releases the current factory and re-installs the original factory.
     */
    public static void release() {
        Log log = LOGS.get(LOGS.size() - 1);
        StaticLoggerBinder.getSingleton().setMavenLog(log);
    }

}
