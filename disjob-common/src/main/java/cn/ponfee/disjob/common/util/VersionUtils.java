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

import cn.ponfee.disjob.common.collect.Collects;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Version utility
 *
 * @author Ponfee
 */
public final class VersionUtils {

    private static final String VERSION = "version";

    public static String getVersion(Class<?> cls) {
        return getVersion(cls, null, null);
    }

    public static String getVersion(Class<?> cls, String groupId, String artifactId) {
        String version = cls.getPackage().getImplementationVersion();
        if (StringUtils.isNotBlank(version)) {
            return version;
        }

        URL url = cls.getProtectionDomain().getCodeSource().getLocation();
        try (JarFile jarFile = getJarFile(url)) {
            version = jarFile.getManifest().getMainAttributes().getValue(Name.IMPLEMENTATION_VERSION);
            if (StringUtils.isNotBlank(version)) {
                return version;
            }

            version = getPomPropertiesVersion(jarFile, groupId, artifactId);
            if (StringUtils.isNotBlank(version)) {
                return version;
            }

            // extract jar file base name: `/path/xxx.jar!/` or `/path/xxx.jar`  =>  `xxx`
            String jarFileName = Collects.getLast(url.getPath().replaceAll("(?:\\.jar!/|\\.jar)$", "").split("[/\\\\]"));
            if (StringUtils.isNotBlank(artifactId) && jarFileName.startsWith(artifactId + "-")) {
                return jarFileName.substring(artifactId.length() + 1);
            }
            return Collects.getLast(jarFileName.split("-"));
        } catch (FileNotFoundException ignored) {
            // e.g.: /Users/ponfee/scm/gitee/disjob/disjob-supervisor/target/classes/
            return "file".equals(url.getProtocol()) ? getLocalMavenPomVersion(url) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    // --------------------------------------------------------------------private methods

    private static JarFile getJarFile(URL url) throws Exception {
        URLConnection connection = url.openConnection();
        if (connection instanceof JarURLConnection) {
            // 对于springboot的url为`file:/path/xxx.jar!/BOOT-INF/lib/commons-lang3-3.12.0.jar!/`
            return ((JarURLConnection) connection).getJarFile();
        } else {
            return new JarFile(Files.toFile(url));
        }
    }

    private static String getPomPropertiesVersion(JarFile jarFile, String groupId, String artifactId) throws IOException {
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(artifactId)) {
            return null;
        }
        String pathFormat = "META-INF/maven/%s/%s/pom.properties";
        // e.g.: META-INF/maven/com.google.guava/guava/pom.properties
        JarEntry pomPropertiesEntry = jarFile.getJarEntry(String.format(pathFormat, groupId, artifactId));
        if (pomPropertiesEntry == null) {
            // e.g.: META-INF/maven/com/google/guava/guava/pom.properties
            pomPropertiesEntry = jarFile.getJarEntry(String.format(pathFormat, groupId.replace('.', '/'), artifactId));
        }
        if (pomPropertiesEntry == null) {
            return null;
        }

        Properties props = new Properties();
        props.load(jarFile.getInputStream(pomPropertiesEntry));
        return props.getProperty(VERSION);
    }

    private static String getLocalMavenPomVersion(URL url) {
        try (FileInputStream inputStream = new FileInputStream(getLocalMavenPomFile(url))) {
            Document document = Files.parseToXmlDocument(inputStream);
            XPath xpath = XPathFactory.newInstance().newXPath();
            String version = (String) xpath.evaluate("/project/version", document, XPathConstants.STRING);
            if (StringUtils.isBlank(version)) {
                version = (String) xpath.evaluate("/project/parent/version", document, XPathConstants.STRING);
            }
            return version;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String getLocalMavenPomFile(URL url) {
        return Files.toFile(url).getParentFile().getParentFile().getAbsolutePath() + "/pom.xml";
    }

}
