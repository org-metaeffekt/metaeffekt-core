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
package org.metaeffekt.core.maven.kernel;


/**
 * Class providing access to general maven constants.
 *
 * @author Karsten Klein
 */
public abstract class MavenConstants {

    public static final String MAVEN_PACKAGING_POM = "pom";
    public static final String MAVEN_PACKAGING_JAR = "jar";

    private MavenConstants() {
    }
}
