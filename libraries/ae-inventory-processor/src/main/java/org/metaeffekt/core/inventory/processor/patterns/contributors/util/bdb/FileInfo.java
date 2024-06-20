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

package org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb;

public class FileInfo {
    private String path;
    private short mode;
    private String digest;
    private int size;
    private String username;
    private String groupname;
    private FileFlags flags;

    public FileInfo(String path, short mode, String digest, int size, String username, String groupname, FileFlags flags) {
        this.path = path;
        this.mode = mode;
        this.digest = digest;
        this.size = size;
        this.username = username;
        this.groupname = groupname;
        this.flags = flags;
    }

    // Getters and Setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public short getMode() {
        return mode;
    }

    public void setMode(short mode) {
        this.mode = mode;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGroupname() {
        return groupname;
    }

    public void setGroupname(String groupname) {
        this.groupname = groupname;
    }

    public FileFlags getFlags() {
        return flags;
    }

    public void setFlags(FileFlags flags) {
        this.flags = flags;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "path='" + path + '\'' +
                ", mode=" + mode +
                ", digest='" + digest + '\'' +
                ", size=" + size +
                ", username='" + username + '\'' +
                ", groupname='" + groupname + '\'' +
                ", flags=" + flags +
                '}';
    }
}

