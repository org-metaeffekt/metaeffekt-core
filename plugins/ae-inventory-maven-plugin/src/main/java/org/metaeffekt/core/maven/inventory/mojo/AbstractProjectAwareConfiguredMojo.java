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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;


public abstract class AbstractProjectAwareConfiguredMojo extends AbstractProjectAwareMojo {

    // FIXME: using mixed annotations
    /**
     * The project to be checked: the current project (readonly)
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public MavenProject getProject() {
        return project;
    }

    protected <T> T createInstanceOf(String className, Class<T> type) {
        if (className == null) return null;
        try {
            Class<?> instanceClass = Class.forName(className);
            return (T) instanceClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot access class: " + className, e);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Cannot instantiate class: " + className, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot instantiate class: " + className, e);
        } catch (ClassCastException e) {
            throw new IllegalStateException("Instantiated class not of expected type " + type + ", : " + className, e);
        }
    }


}
