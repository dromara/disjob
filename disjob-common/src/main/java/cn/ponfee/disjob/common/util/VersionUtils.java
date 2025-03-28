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

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
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
        String implementationVersion = cls.getPackage().getImplementationVersion();
        if (StringUtils.isNotBlank(implementationVersion)) {
            return implementationVersion;
        }

        URL url = cls.getProtectionDomain().getCodeSource().getLocation();
        try (JarFile jarFile = getJarFile(url)) {
            implementationVersion = jarFile.getManifest().getMainAttributes().getValue(Name.IMPLEMENTATION_VERSION);
            if (StringUtils.isNotBlank(implementationVersion)) {
                return implementationVersion;
            }

            JarEntry jarEntry = getJarEntry(jarFile, groupId, artifactId);
            if (jarEntry == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(jarFile.getInputStream(jarEntry));
            return props.getProperty(VERSION);
        } catch (FileNotFoundException ignored) {
            return "file".equals(url.getProtocol()) ? getLocalMavenPomVersion(url) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    // --------------------------------------------------------------------private methods

    private static JarFile getJarFile(URL url) throws Exception {
        URLConnection connection = url.openConnection();
        if (connection instanceof JarURLConnection) {
            return ((JarURLConnection) connection).getJarFile();
        } else {
            return new JarFile(new File(url.toURI()));
        }
    }

    private static JarEntry getJarEntry(JarFile jarFile, String groupId, String artifactId) {
        if (groupId == null || artifactId == null) {
            return null;
        }
        String pathFormat = "META-INF/maven/%s/%s/pom.properties";
        // e.g.: META-INF/maven/com.google.guava/guava/pom.properties
        JarEntry jarEntry = jarFile.getJarEntry(String.format(pathFormat, groupId, artifactId));
        if (jarEntry == null) {
            // e.g.: META-INF/maven/com/google/guava/guava/pom.properties
            jarEntry = jarFile.getJarEntry(String.format(pathFormat, groupId.replace(".", "/"), artifactId));
        }
        return jarEntry;
    }

    private static String getLocalMavenPomVersion(URL url) {
        try {
            // e.g.: /Users/ponfee/scm/gitee/disjob/disjob-supervisor/target/classes/
            String mavenPomFilePath = new File(url.toURI()).getParentFile().getParentFile().getAbsolutePath() + "/pom.xml";
            @SuppressWarnings("all")
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(mavenPomFilePath);
            document.getDocumentElement().normalize();
            Element root = document.getDocumentElement();
            if (!"project".equals(root.getNodeName())) {
                return null;
            }

            // <project>
            //   <version>1.0.0</version>
            // </project>
            Node node = getChildNode(root, VERSION);
            if (node == null) {
                // <project>
                //   <parent>
                //     <version>1.0.0</version>
                //   </parent>
                // </project>
                node = Optional.ofNullable(getChildNode(root, "parent")).map(e -> getChildNode(e, VERSION)).orElse(null);
            }
            return node == null ? null : StringUtils.trim(node.getTextContent());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Node getChildNode(Node node, String childNodeName) {
        NodeList childNodes = node.getChildNodes();
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node childNode = childNodes.item(i);
            if (childNodeName.equals(childNode.getNodeName())) {
                return childNode;
            }
        }
        return null;
    }

}
