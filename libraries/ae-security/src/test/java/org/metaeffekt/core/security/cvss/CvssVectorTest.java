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
package org.metaeffekt.core.security.cvss;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.security.cvss.v3.Cvss3P1;

public class CvssVectorTest {

    @Test
    public void parseStaticVersionTest() {
        Assert.assertEquals(Cvss3P1.class, CvssVector.classFromVersionName(Cvss3P1.getVersionName()));
    }

}