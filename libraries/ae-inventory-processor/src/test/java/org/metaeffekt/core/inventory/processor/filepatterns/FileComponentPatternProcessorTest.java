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
package org.metaeffekt.core.inventory.processor.filepatterns;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class FileComponentPatternProcessorTest {

    private final FileComponentPatternProcessor processor = new FileComponentPatternProcessor();

    @Test
    public void process001() {
        FileMetaData fileMetaData = processor.deriveFileMetaData("gems/prometheus-client-mmap-1.2.10-x86_64-linux-gnu/Cargo.toml");
        Assertions.assertThat(fileMetaData).isNotNull();
        Assertions.assertThat(fileMetaData.getVersion()).isEqualTo("1.2.10");
        Assertions.assertThat(fileMetaData.getName()).isEqualTo("prometheus-client-mmap");
        Assertions.assertThat(fileMetaData.getQualifier()).isEqualTo("prometheus-client-mmap-1.2.10-x86_64-linux-gnu");
    }

    @Test
    public void process002() {
        FileMetaData fileMetaData = processor.deriveFileMetaData("gems/prometheus-client_mmap-1.2.10-x86_64-linux-gnu/Cargo.toml");
        Assertions.assertThat(fileMetaData).isNotNull();
        Assertions.assertThat(fileMetaData.getVersion()).isEqualTo("1.2.10");
        Assertions.assertThat(fileMetaData.getName()).isEqualTo("prometheus-client_mmap");
        Assertions.assertThat(fileMetaData.getQualifier()).isEqualTo("prometheus-client_mmap-1.2.10-x86_64-linux-gnu");
    }

    @Test
    public void process003() {
        FileMetaData fileMetaData = processor.deriveFileMetaData("ruby/gems/3.4.0/cache/[drb-2.2.1.gem]/[data.tar.gz]/[data.tar]/drb.gemspec");
        Assertions.assertThat(fileMetaData).isNotNull();
        Assertions.assertThat(fileMetaData.getVersion()).isEqualTo("2.2.1");
        Assertions.assertThat(fileMetaData.getName()).isEqualTo("drb");
        Assertions.assertThat(fileMetaData.getQualifier()).isEqualTo("drb-2.2.1");
    }

    @Test
    public void process004() {
        FileMetaData fileMetaData = processor.deriveFileMetaData("ruby/gems/3.4.0/cache/[dr_b-2.2.1.gem]/[data.tar.gz]/[data.tar]/dr_b.gemspec");
        Assertions.assertThat(fileMetaData).isNotNull();
        Assertions.assertThat(fileMetaData.getVersion()).isEqualTo("2.2.1");
        Assertions.assertThat(fileMetaData.getName()).isEqualTo("dr_b");
        Assertions.assertThat(fileMetaData.getQualifier()).isEqualTo("dr_b-2.2.1");
    }

}