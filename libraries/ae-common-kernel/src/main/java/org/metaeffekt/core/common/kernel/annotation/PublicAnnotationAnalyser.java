/**
 * Copyright 2009-2020 the original author or authors.
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

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class PublicAnnotationAnalyser {

    private static final String CLASS_SUFFIX = ".class";

    private final ClassLoader classLoader;

    private final Class<? extends Annotation> annotationClass;

    public PublicAnnotationAnalyser(ClassLoader classLoader, Class<? extends Annotation> annotationClass) {
        this.classLoader = classLoader;
        this.annotationClass = annotationClass;
    }

    public List<String> collectPublicTypes(File classpathRoot, List<String> list) {
        return collectPublicTypes(classpathRoot, classpathRoot, list);
    }

    protected List<String> collectPublicTypes(File classpathRoot, File file, List<String> list) {

        if (file.isDirectory()) {
            for (File innerFile : file.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isDirectory() || (pathname.isFile()
                            && pathname.getName().endsWith(CLASS_SUFFIX)
                            && !pathname.getName().endsWith("package-info.class"));
                }
            })) {
                list = collectPublicTypes(classpathRoot, innerFile, list);
            }
        }
        if (file.isFile()) {
            String classFilenameWithoutExtension = extractClassFilenameWithoutExtension(classpathRoot, file);

            Class<? extends Object> clz = null;

            ClassLoader localClassLoader = classLoader;

            if (localClassLoader == null) {
                localClassLoader = ClassLoader.getSystemClassLoader();
            }

            String className = classFilenameWithoutExtension.replace('/', '.');

            try {
                clz = Class.forName(className, false, localClassLoader);
            } catch (Error e) {
                // skipping class not found
//                throw new IllegalStateException(e.getMessage(), e);
                // TODO: log a warning!!! How to handle logs on this level?
            } catch (Exception e) {
                // skipping class not found
//                throw new IllegalStateException(e.getMessage(), e);
                // TODO: log a warning!!! How to handle logs on this level?
            }

            if (clz != null && isPublicAnnotated(clz, annotationClass)) {
                list.add(classFilenameWithoutExtension + CLASS_SUFFIX);
                list.addAll(identifyAdditionalClasses(classpathRoot, file));
            }

        }
        return list;
    }

    private String extractClassFilenameWithoutExtension(File classpathRoot, File file) {
        String className = file.getAbsolutePath()
                .substring(0, file.getAbsolutePath().length() - CLASS_SUFFIX.length())
                .substring(classpathRoot.getAbsolutePath().length() + 1)
                .replace('\\', '/');
        return className;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean isPublicAnnotated(Class clz, Class<? extends Annotation> annotationClass) {
        if (clz == null) {
            return false;
        }
        if (clz.getPackage() == null) {
            return false;
        }

        // check package level
        if (clz.getPackage().getAnnotation(annotationClass) != null) {
            return true;
        }

        // check class level
        if (clz.getAnnotation(annotationClass) != null) {
            return true;
        }

        return false;
    }

    private List<String> identifyAdditionalClasses(File compileTarget, File currentPublicClass) {
        String absPath = currentPublicClass.getParent();

        File directory = new File(absPath);
        File[] additionals = directory.listFiles(new AdditionalFileFilter(currentPublicClass.getName()));
        List<String> classNames = new ArrayList<String>();

        for (File currentFile : additionals) {
            classNames.add(extractClassFilenameWithoutExtension(compileTarget, currentFile) + CLASS_SUFFIX);
        }

        return classNames;
    }

    public class AdditionalFileFilter implements FilenameFilter {

        private String basename;

        AdditionalFileFilter(String basename) {
            this.basename = basename.replace(CLASS_SUFFIX, "");
        }

        public boolean accept(File dir, String name) {
            if (name.startsWith(basename + "$")) {
                return true;
            }
            return false;
        }

    }
}