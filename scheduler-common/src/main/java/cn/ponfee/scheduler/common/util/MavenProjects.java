/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.util;

import java.io.File;

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

    private static final String EXCLUSION_STRING = "[\r\n]"; // "\r|\n|\\s+"

    public static String getProjectBaseDir() {
        String path = Thread.currentThread().getContextClassLoader().getResource("").getFile();
        return Files.cleanPath(new File(path).getParentFile().getParentFile().getPath());
    }

    // --------------------------------------------------------------------------------------java
    public static File getMainJavaFile(Class<?> clazz) {
        return new File(getMainJavaPath("") + clazz.getCanonicalName().replace('.', '/') + ".java");
    }

    public static byte[] getMainJavaFileAsBytes(Class<?> clazz) {
        return Files.toByteArray(MavenProjects.getMainJavaFile(clazz));
    }

    public static String getMainJavaFileAsString(Class<?> clazz) {
        return Files.toString(MavenProjects.getMainJavaFile(clazz)).replaceAll(EXCLUSION_STRING, "");
    }

    public static File getTestJavaFile(Class<?> clazz) {
        return new File(getTestJavaPath("") + clazz.getCanonicalName().replace('.', '/') + ".java");
    }

    public static byte[] getTestJavaFileAsBytes(Class<?> clazz) {
        return Files.toByteArray(MavenProjects.getTestJavaFile(clazz));
    }

    public static String getTestJavaFileAsString(Class<?> clazz) {
        return Files.toString(MavenProjects.getTestJavaFile(clazz)).replaceAll(EXCLUSION_STRING, "");
    }

    public static String getMainJavaPath(String basePackage) {
        return getProjectBaseDir() + "/src/main/java/" + basePackage.replace('.', '/');
    }

    public static String getMainJavaPath(String basePackage, String filename) {
        return getMainJavaPath(basePackage) + "/" + filename;
    }

    public static String getTestJavaPath(String basePackage) {
        return getProjectBaseDir() + "/src/test/java/" + basePackage.replace('.', '/');
    }

    public static String getTestJavaPath(String basePackage, String filename) {
        return getTestJavaPath(basePackage) + "/" + filename;
    }

    // --------------------------------------------------------------------------------------scala
    public static File getMainScalaFile(Class<?> clazz) {
        return new File(getMainScalaPath("") + clazz.getCanonicalName().replace('.', '/') + ".scala");
    }

    public static byte[] getMainScalaFileAsBytes(Class<?> clazz) {
        return Files.toByteArray(MavenProjects.getMainScalaFile(clazz));
    }

    public static String getMainScalaFileAsString(Class<?> clazz) {
        return Files.toString(MavenProjects.getMainScalaFile(clazz)).replaceAll(EXCLUSION_STRING, "");
    }

    public static File getTestScalaFile(Class<?> clazz) {
        return new File(getTestScalaPath("") + clazz.getCanonicalName().replace('.', '/') + ".scala");
    }

    public static byte[] getTestScalaFileAsBytes(Class<?> clazz) {
        return Files.toByteArray(MavenProjects.getTestScalaFile(clazz));
    }

    public static String getTestScalaFileAsString(Class<?> clazz) {
        return Files.toString(MavenProjects.getTestScalaFile(clazz)).replaceAll(EXCLUSION_STRING, "");
    }

    public static String getMainScalaPath(String basePackage) {
        return getProjectBaseDir() + "/src/main/scala/" + basePackage.replace('.', '/');
    }

    public static String getMainScalaPath(String basePackage, String filename) {
        return getMainScalaPath(basePackage) + "/" + filename;
    }

    public static String getTestScalaPath(String basePackage) {
        return getProjectBaseDir() + "/src/test/scala/" + basePackage.replace('.', '/');
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

}
