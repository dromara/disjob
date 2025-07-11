/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.common.util;

import java.io.File;
import java.net.URL;

/**
 * Maven标准的项目文件工具类(Only use in test case)
 * <ul>
 *   <li>new File("src/test/resources/test.txt");</li>
 *   <li>new File("src/test/java/test/test1.java");</li>
 *   <li>new File("src/main/resources/log4j2.xml");</li>
 * </ul>
 *
 * @author Ponfee
 */
public class MavenProjects {

    /**
     * \r|\n|\\s+
     */
    private static final String EXCLUSION_STRING = "[\r\n]";

    public static String getProjectBaseDir() {
        URL url = Thread.currentThread().getContextClassLoader().getResource("");
        return Files.cleanPath(Files.toFile(url).getParentFile().getParentFile().getAbsolutePath());
    }

    // --------------------------------------------------------------------------------------java

    public static File getMainJavaFile(Class<?> clazz) {
        return new File(getMainJavaPath("") + ClassUtils.getPackagePath(clazz.getCanonicalName()) + ".java");
    }

    public static byte[] getMainJavaFileAsBytes(Class<?> clazz) {
        return Files.toByteArray(getMainJavaFile(clazz));
    }

    public static String getMainJavaFileAsString(Class<?> clazz) {
        return Files.toString(getMainJavaFile(clazz)).replaceAll(EXCLUSION_STRING, "");
    }

    public static File getTestJavaFile(Class<?> clazz) {
        return new File(getTestJavaPath("") + ClassUtils.getPackagePath(clazz.getCanonicalName()) + ".java");
    }

    public static byte[] getTestJavaFileAsBytes(Class<?> clazz) {
        return Files.toByteArray(getTestJavaFile(clazz));
    }

    public static String getTestJavaFileAsString(Class<?> clazz) {
        return Files.toString(getTestJavaFile(clazz)).replaceAll(EXCLUSION_STRING, "");
    }

    public static String getMainJavaPath(String basePackage) {
        return getProjectBaseDir() + "/src/main/java/" + ClassUtils.getPackagePath(basePackage);
    }

    public static String getMainJavaPath(String basePackage, String filename) {
        return getMainJavaPath(basePackage) + "/" + filename;
    }

    public static String getTestJavaPath(String basePackage) {
        return getProjectBaseDir() + "/src/test/java/" + ClassUtils.getPackagePath(basePackage);
    }

    public static String getTestJavaPath(String basePackage, String filename) {
        return getTestJavaPath(basePackage) + "/" + filename;
    }

    // --------------------------------------------------------------------------------------scala

    public static File getMainScalaFile(Class<?> clazz) {
        return new File(getMainScalaPath("") + ClassUtils.getPackagePath(clazz.getCanonicalName()) + ".scala");
    }

    public static byte[] getMainScalaFileAsBytes(Class<?> clazz) {
        return Files.toByteArray(getMainScalaFile(clazz));
    }

    public static String getMainScalaFileAsString(Class<?> clazz) {
        return Files.toString(getMainScalaFile(clazz)).replaceAll(EXCLUSION_STRING, "");
    }

    public static File getTestScalaFile(Class<?> clazz) {
        return new File(getTestScalaPath("") + ClassUtils.getPackagePath(clazz.getCanonicalName()) + ".scala");
    }

    public static byte[] getTestScalaFileAsBytes(Class<?> clazz) {
        return Files.toByteArray(getTestScalaFile(clazz));
    }

    public static String getTestScalaFileAsString(Class<?> clazz) {
        return Files.toString(getTestScalaFile(clazz)).replaceAll(EXCLUSION_STRING, "");
    }

    public static String getMainScalaPath(String basePackage) {
        return getProjectBaseDir() + "/src/main/scala/" + ClassUtils.getPackagePath(basePackage);
    }

    public static String getMainScalaPath(String basePackage, String filename) {
        return getMainScalaPath(basePackage) + "/" + filename;
    }

    public static String getTestScalaPath(String basePackage) {
        return getProjectBaseDir() + "/src/test/scala/" + ClassUtils.getPackagePath(basePackage);
    }

    public static String getTestScalaPath(String basePackage, String filename) {
        return getTestScalaPath(basePackage) + "/" + filename;
    }

    // --------------------------------------------------------------------------------------resources

    public static String getMainResourcesPath() {
        return getProjectBaseDir() + "/src/main/resources/";
    }

    public static String getMainResourcesPath(String followPath) {
        return getMainResourcesPath() + followPath;
    }

    public static String getTestResourcesPath() {
        return getProjectBaseDir() + "/src/test/resources/";
    }

    public static String getTestResourcesPath(String followPath) {
        return getTestResourcesPath() + followPath;
    }

    // --------------------------------------------------------------------------------------webapp

    public static String getWebAppPath() {
        return getProjectBaseDir() + "/src/main/webapp/";
    }

    public static String getWebAppPath(String webappPath) {
        return getWebAppPath() + webappPath;
    }

    // --------------------------------------------------------------------------------------class

    public static String getMainClassPath(String basePackage) {
        return getProjectBaseDir() + "/target/classes/" + ClassUtils.getPackagePath(basePackage);
    }

    public static File getMainClassFile(Class<?> clazz) {
        return new File(getMainClassPath("") + ClassUtils.getPackagePath(clazz.getName()) + ".class");
    }

    public static byte[] getMainClassFileAsBytes(Class<?> clazz) {
        return Files.toByteArray(getMainClassFile(clazz));
    }

    public static String getTestClassPath(String basePackage) {
        return getProjectBaseDir() + "/target/test-classes/" + ClassUtils.getPackagePath(basePackage);
    }

    public static File getTestClassFile(Class<?> clazz) {
        return new File(getTestClassPath("") + ClassUtils.getPackagePath(clazz.getName()) + ".class");
    }

    public static byte[] getTestClassFileAsBytes(Class<?> clazz) {
        return Files.toByteArray(getTestClassFile(clazz));
    }

}
