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

import lombok.Getter;
import lombok.Setter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
public class ServerCredential {

    private String matchUrl;
    private String username;
    private String password;
    private String token;

    public String getUsername() {return resolveEnvVariables(username);}

    public String getPassword() {
        return resolveEnvVariables(password);
    }

    public String getToken() {
        return resolveEnvVariables(token);
    }

    private String resolveEnvVariables(String value) {
        if (value == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String envVar = matcher.group(1);
            String envValue = System.getenv(envVar);
            matcher.appendReplacement(sb, envValue != null ? Matcher.quoteReplacement(envValue) : "");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
