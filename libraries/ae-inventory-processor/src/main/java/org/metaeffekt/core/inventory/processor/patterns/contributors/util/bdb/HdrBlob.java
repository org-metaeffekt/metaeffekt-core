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

import java.util.ArrayList;
import java.util.List;

public class HdrBlob {
    private List<EntryInfo> peList;
    private int il;
    private int dl;
    private int pvlen;
    private int dataStart;
    private int dataEnd;
    private int regionTag;
    private int ril;
    private int rdl;

    public HdrBlob() {
        this.peList = new ArrayList<>();
    }

    public List<EntryInfo> getPeList() {
        return peList;
    }

    public void setPeList(List<EntryInfo> peList) {
        this.peList = peList;
    }

    public int getIl() {
        return il;
    }

    public void setIl(int il) {
        this.il = il;
    }

    public int getDl() {
        return dl;
    }

    public void setDl(int dl) {
        this.dl = dl;
    }

    public int getPvlen() {
        return pvlen;
    }

    public void setPvlen(int pvlen) {
        this.pvlen = pvlen;
    }

    public int getDataStart() {
        return dataStart;
    }

    public void setDataStart(int dataStart) {
        this.dataStart = dataStart;
    }

    public int getDataEnd() {
        return dataEnd;
    }

    public void setDataEnd(int dataEnd) {
        this.dataEnd = dataEnd;
    }

    public int getRegionTag() {
        return regionTag;
    }

    public void setRegionTag(int regionTag) {
        this.regionTag = regionTag;
    }

    public int getRil() {
        return ril;
    }

    public void setRil(int ril) {
        this.ril = ril;
    }

    public int getRdl() {
        return rdl;
    }

    public void setRdl(int rdl) {
        this.rdl = rdl;
    }

    @Override
    public String toString() {
        return "HdrBlob{" +
                "peList=" + peList +
                ", il=" + il +
                ", dl=" + dl +
                ", pvlen=" + pvlen +
                ", dataStart=" + dataStart +
                ", dataEnd=" + dataEnd +
                ", regionTag=" + regionTag +
                ", ril=" + ril +
                ", rdl=" + rdl +
                '}';
    }
}

