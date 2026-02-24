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
package org.metaeffekt.reader.inspect.image;

import org.junit.jupiter.api.Test;
import org.metaeffekt.reader.inspect.image.model.ImageInspectData;
import org.metaeffekt.reader.inspect.image.model.ImageInspectElement;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class ImageInspectReaderTest {
    File singleImageInspect = new File("src/test/resources", "image-inspect-test-debian.json");
    File monsterImageInspect = new File("src/test/resources", "monster-image-inspect.json");

    @Test
    public void shouldReadImageInspect() {
        assertTrue(true);
        ImageInspectData data = ImageInspectReader.dataFromJson(singleImageInspect);
        ImageInspectElement el = data.stream().findAny().orElse(null);

        // check that it contains the element
        assertNotNull(el);

        // check that the element contains some of the data
        assertEquals("sha256:a178460bae579ffbbf8805d8ba8e47adbe96f693098c85bf309b79547d076c21", el.getId());
        assertEquals("2021-09-28T01:22:25.646405559Z", el.getCreated());

        // check that some of the nested arrays / objects work
        assertEquals("2c540234abd1", el.getContainerConfig().getHostname());
        assertEquals("sha256:28783f314d9db6f0bbf8b7baa180a0b357d872f7f6c38f5701afa5972d9b3961",
                el.getContainerConfig().getImage());
        assertEquals("layers", el.getRootFS().getType());
        assertEquals("0001-01-01T00:00:00Z", el.getMetadata().get("LastTagTime"));
    }

    @Test
    public void shouldReadMonsterInspect() {
        ImageInspectData data = ImageInspectReader.dataFromJson(monsterImageInspect);

        // monster inspect should have n elements:
        assertEquals(42, data.size());
    }
}
