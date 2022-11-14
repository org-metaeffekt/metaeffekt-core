/*
 * Copyright 2009-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExecutionStatus {

    private final List<ExecutionStatusEntry> entryList = new ArrayList<>();

    public void add(ExecutionStatusEntry.SEVERITY severity, String message) {
        entryList.add(new ExecutionStatusEntry(severity, message));
    }

    public void info(String message) {
        entryList.add(new ExecutionStatusEntry(ExecutionStatusEntry.SEVERITY.INFO, message));
    }

    public void warn(String message) {
        entryList.add(new ExecutionStatusEntry(ExecutionStatusEntry.SEVERITY.WARN, message));
    }

    public void error(String message) {
        entryList.add(new ExecutionStatusEntry(ExecutionStatusEntry.SEVERITY.ERROR, message));
    }

    public boolean isError() {
        for (ExecutionStatusEntry entry : entryList) {
            if (entry.getSeverity() == ExecutionStatusEntry.SEVERITY.ERROR) {
                return true;
            }
        }
        return false;
    }

    public List<ExecutionStatusEntry> getEntries() {
        return Collections.unmodifiableList(entryList);
    }
}
