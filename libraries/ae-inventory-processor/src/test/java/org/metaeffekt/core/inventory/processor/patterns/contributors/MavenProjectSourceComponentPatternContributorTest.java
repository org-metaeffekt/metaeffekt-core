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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;

public class MavenProjectSourceComponentPatternContributorTest {

    final MavenProjectSourcesComponentPatternContributor contributor = new MavenProjectSourcesComponentPatternContributor();

    @Test
    public void testVariant001() {
        File file = new File("src/test/resources/component-pattern-contributor/maven-001/pom.xml");

        final List<ComponentPatternData> cpdList = contributor.contribute(file.getParentFile(),
                "pom.xml", FileUtils.computeMD5Checksum(file));

        assertThat(cpdList.size()).isEqualTo(1);

        // NOTE: dependencies will not be handled on extraction (too few information available in case; deferred to resolver)
        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("hellofx-1.0-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("1.0-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("hellofx");

    }

}