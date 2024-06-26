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
package org.metaeffekt.core.maven.kernel.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public abstract class AbstractLoggingMojo extends AbstractMojo {

    /**
     * Execute mojo logic.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
    }

    /**
     * Perform whatever build-process behavior this <code>Mojo</code> implements.
     *
     * @throws MojoExecutionException if an unexpected problem occurs.
     * Throwing this exception causes a "BUILD ERROR" message to be displayed.
     * @throws MojoFailureException if an expected problem (such as a compilation failure) occurs.
     * Throwing this exception causes a "BUILD FAILURE" message to be displayed.
     */
    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

}