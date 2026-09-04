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

import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Properties;

public class AbstractRemoteAccessTest {

    // Concrete implementation for testing AbstractRemoteAccess
    private static class TestRemoteAccess extends AbstractRemoteAccess {
        public TestRemoteAccess(Properties properties) {
            super(properties);
        }
    }

    @Test
    public void testCreateGetRequest_NoCredentials() {
        TestRemoteAccess remoteAccess = new TestRemoteAccess(new Properties());
        HttpGet request = remoteAccess.createGetRequest("https://example.com/test");

        Header[] authHeaders = request.getHeaders("Authorization");
        assertEquals(0, authHeaders.length);
    }

    @Test
    public void testCreateGetRequest_BasicAuth() {
        TestRemoteAccess remoteAccess = new TestRemoteAccess(new Properties());

        ServerCredential credential = new ServerCredential();
        credential.setMatchUrl("https://example.com/");
        credential.setUsername("testuser");
        credential.setPassword("testpass");

        remoteAccess.setCredentials(Collections.singletonList(credential));

        HttpGet request = remoteAccess.createGetRequest("https://example.com/api/data");

        Header[] authHeaders = request.getHeaders("Authorization");
        assertEquals(1, authHeaders.length);
        // Base64 encoding of "testuser:testpass" is "dGVzdHVzZXI6dGVzdHBhc3M="
        assertEquals("Basic dGVzdHVzZXI6dGVzdHBhc3M=", authHeaders[0].getValue());
    }

    @Test
    public void testCreateGetRequest_BearerAuth() {
        TestRemoteAccess remoteAccess = new TestRemoteAccess(new Properties());

        ServerCredential credential = new ServerCredential();
        credential.setMatchUrl("https://api.github.com/");
        credential.setToken("my-secret-token");

        remoteAccess.setCredentials(Collections.singletonList(credential));

        HttpGet request = remoteAccess.createGetRequest("https://api.github.com/repos");

        Header[] authHeaders = request.getHeaders("Authorization");
        assertEquals(1, authHeaders.length);
        assertEquals("Bearer my-secret-token", authHeaders[0].getValue());
    }

    @Test
    public void testCreateGetRequest_UrlMismatch() {
        TestRemoteAccess remoteAccess = new TestRemoteAccess(new Properties());

        ServerCredential credential = new ServerCredential();
        credential.setMatchUrl("https://secure.example.com/");
        credential.setUsername("user");
        credential.setPassword("pass");

        remoteAccess.setCredentials(Collections.singletonList(credential));

        // The requested URL does NOT start with the matchUrl
        HttpGet request = remoteAccess.createGetRequest("https://public.example.com/data");

        Header[] authHeaders = request.getHeaders("Authorization");
        assertEquals(0, authHeaders.length);
    }

    @Test
    public void testCreateGetRequest_TokenPreferredOverBasicAuth() {
        TestRemoteAccess remoteAccess = new TestRemoteAccess(new Properties());

        ServerCredential credential = new ServerCredential();
        credential.setMatchUrl("https://example.com/");
        credential.setUsername("testuser");
        credential.setPassword("testpass");
        credential.setToken("my-secret-token");

        remoteAccess.setCredentials(Collections.singletonList(credential));

        HttpGet request = remoteAccess.createGetRequest("https://example.com/api/data");

        Header[] authHeaders = request.getHeaders("Authorization");
        assertEquals(1, authHeaders.length);
        assertEquals("Bearer my-secret-token", authHeaders[0].getValue());
    }
}
