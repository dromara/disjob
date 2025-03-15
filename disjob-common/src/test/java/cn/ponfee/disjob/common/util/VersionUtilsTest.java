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

import com.google.common.graph.Graph;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

/**
 * VersionUtilsTest
 *
 * @author Ponfee
 */
class VersionUtilsTest {

    @Test
    void test() throws Exception {
        Assertions.assertEquals("3.12.0", VersionUtils.getVersion(StringUtils.class));
        Assertions.assertEquals("33.4.0-jre", VersionUtils.getVersion(Graph.class, "com.google.guava", "guava"));
        Assertions.assertEquals("${revision}", VersionUtils.getVersion(VersionUtils.class, "cn.ponfee", "disjob-common"));

        String path = "/Applications/Movist Pro.app/Contents/PlugIns/SafariExtension Pro.appex/Contents/Info.plist";
        File file = new File(path);
        Assertions.assertEquals(path, file.getPath());
        Assertions.assertEquals(file.getPath(), file.getAbsolutePath());
        Assertions.assertSame(file.getPath(), file.toString());

        URL url = file.toURI().toURL();
        Assertions.assertEquals(file.getPath(), new File(url.toURI()).toPath().toString());
        Assertions.assertEquals(file.getPath(), new File(url.toURI()).getAbsolutePath());
        Assertions.assertEquals(file.getPath(), url.toURI().getPath());

        Assertions.assertEquals("file:/Applications/Movist%20Pro.app/Contents/PlugIns/SafariExtension%20Pro.appex/Contents/Info.plist", url.toString());
        Assertions.assertEquals(url.toString(), url.toURI().toString());
        Assertions.assertEquals("/Applications/Movist%20Pro.app/Contents/PlugIns/SafariExtension%20Pro.appex/Contents/Info.plist", url.getPath());
        Assertions.assertEquals(url.getPath(), url.getFile());

        //System.out.println(IOUtils.toString(url.toURI(), StandardCharsets.UTF_8));
        //System.out.println(FileUtils.readFileToString(new File(url.toURI()), StandardCharsets.UTF_8));
    }

}
