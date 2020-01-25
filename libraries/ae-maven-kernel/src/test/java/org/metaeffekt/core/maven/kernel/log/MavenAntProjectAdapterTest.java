/**
 * Copyright 2009-2020 the original author or authors.
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

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.metaeffekt.core.common.kernel.ant.log.EscalationException;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;


public class MavenAntProjectAdapterTest {

    private ByteArrayOutputStream err;
    private PrintStream orginalErr = System.err;

    private ByteArrayOutputStream out;
    private PrintStream orginalOut = System.out;

    @Before
    public void setupStreams() {
        MavenLogAdapter.initialize(new SystemStreamLog());

        err = new ByteArrayOutputStream();
        out = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        System.setOut(new PrintStream(out));
    }

    @Before
    public void setupLogFactory() {
        err = new ByteArrayOutputStream();
        out = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        System.setOut(new PrintStream(out));
    }

    @After
    public void resetStreams() {
        System.setErr(orginalErr);
        System.setOut(orginalOut);
        MavenLogAdapter.release();
    }

    @Test
    public void testLoggingInfo() {
        LoggingProjectAdapter adapter = new LoggingProjectAdapter();
        adapter.log("Hello Info");
        Assert.assertTrue(new String(out.toByteArray()).contains("Hello Info"));
    }

    @Test
    public void testLoggingError() {
        LoggingProjectAdapter adapter = new LoggingProjectAdapter();
        adapter.log("Hello Error", LoggingProjectAdapter.MSG_ERR);
        Assert.assertTrue(new String(err.toByteArray()).contains("Hello Error"));
    }

    @Test(expected = EscalationException.class)
    public void testVerboseEscalatedToError() {
        LoggingProjectAdapter adapter = new LoggingProjectAdapter();
        adapter.setEscalate(true);
        adapter.setErrorEscalationTerms(Collections.singleton("[verbose]"));
        adapter.log("Hello escalated Error [verbose]", LoggingProjectAdapter.MSG_VERBOSE);
        Assert.assertTrue(new String(err.toByteArray()).contains("Hello escalated Error [verbose]"));
    }

    @Test
    public void testVerboseEscalatedToWarn() {
        LoggingProjectAdapter adapter = new LoggingProjectAdapter();
        adapter.setEscalate(true);
        adapter.setWarnEscalationTerms(Collections.singleton("[verbose]"));
        adapter.log("Hello escalated Warn [verbose]", LoggingProjectAdapter.MSG_VERBOSE);
        Assert.assertTrue(new String(out.toByteArray()).contains("Hello escalated Warn [verbose]"));
    }

}
