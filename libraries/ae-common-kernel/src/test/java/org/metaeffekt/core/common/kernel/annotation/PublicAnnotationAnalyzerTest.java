/*
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.common.kernel.annotation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.metaeffekt.core.common.kernel.annotation.mock.MyPublic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PublicAnnotationAnalyzerTest {

    public static final String CLASSES_PREFIX = "org/metaeffekt/core/common/kernel/annotation/mock/";

    private final String[] expectedClasses = new String[]{
            CLASSES_PREFIX + "AnnotatedClass.class",
            CLASSES_PREFIX + "AnnotatedClassEnum.class",
            CLASSES_PREFIX + "AnnotatedClassEnum$Action.class",
            CLASSES_PREFIX + "AnnotatedClassEnum$Status.class",
            CLASSES_PREFIX + "AnnotatedClassInner.class",
            // org/metaeffekt/core/common/kernel/annotation/mock/annotatedClassInner$AnnotatedInner.class
            // org/metaeffekt/core/common/kernel/annotation/mock/annotatedClassInner$AnnotatedInner.class
            CLASSES_PREFIX + "AnnotatedClassInner$AnnotatedInner.class",
            CLASSES_PREFIX + "pack/PlainClass.class"
    };

    private final String[] unexpectedClasses = new String[]{
            CLASSES_PREFIX + "PlainClass.class",
            CLASSES_PREFIX + "PlainClassEnum.class",
            CLASSES_PREFIX + "PlainClassEnum$Action.class",
            CLASSES_PREFIX + "PlainClassInner.class",
            CLASSES_PREFIX + "PlainClassInner$PlainInner.class"};

    private File mockFolder = new File("target/test-classes");

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void checkAnnotatedDetection() {
        List<String> list = new ArrayList<String>();

        PublicAnnotationAnalyser analyser = new PublicAnnotationAnalyser(null, MyPublic.class);
        analyser.collectPublicTypes(mockFolder, mockFolder, list);

        assertEquals(expectedClasses.length, list.size());

        for (String unexpected : unexpectedClasses) {
            assertFalse("Didn't expect [" + unexpected + "] but received it!", list
                    .contains(unexpected));
        }

        for (String expected : expectedClasses) {
            assertTrue("Expected [" + expected + "] class but didn't receive it!", list
                    .contains(expected));
        }
    }

}
