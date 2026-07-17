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
package org.metaeffekt.core.inventory.resolver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ServerCredentialTest {

    @Test
    public void testResolveEnvVariables_WithEnvVariables() {
        // "PATH" is typically present on a system
        String pathEnv = System.getenv("PATH");
        assertNotNull(pathEnv, "PATH environment variable must exist for this test");

        ServerCredential credential = new ServerCredential();
        credential.setUsername("user_${PATH}");
        credential.setPassword("${PATH}_pass");
        credential.setToken("token_${PATH}_token");

        assertEquals("user_" + pathEnv, credential.getUsername());
        assertEquals(pathEnv + "_pass", credential.getPassword());
        assertEquals("token_" + pathEnv + "_token", credential.getToken());
    }

    @Test
    public void testResolveEnvVariables_WithUnknownEnvVariable() {
        ServerCredential credential = new ServerCredential();
        credential.setUsername("${UNKNOWN_ENV_VAR_12345}");
        credential.setPassword("${ANOTHER_UNKNOWN_VAR}");
        credential.setToken("prefix_${AND_ANOTHER}_suffix");

        // The current implementation replaces unknown variables with an empty string
        assertEquals("", credential.getUsername());
        assertEquals("", credential.getPassword());
        assertEquals("prefix__suffix", credential.getToken());
    }
}
