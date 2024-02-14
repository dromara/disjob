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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * Files teset
 *
 * @author Ponfee
 */
public class FilesTest {

    @Test
    public void testFile() {
        File path = new File("/Users/ponfee/scm/gitee/logs/axxxxx/b/c");
        Assertions.assertFalse(path.isFile());
        Assertions.assertFalse(path.isDirectory());
        Assertions.assertFalse(path.exists());
        Assertions.assertTrue(path.isAbsolute());
    }

    @Test
    public void testPath() {
        File path1 = new File("/Users/ponfee/scm/gitee/a");
        File path2 = new File("/Users/ponfee/scm/gitee/a/");
        Assertions.assertEquals(path1, path2);
        Assertions.assertEquals(path1.toPath(), path2.toPath());
        Assertions.assertEquals("/Users/ponfee/scm/gitee/a", path1.toPath().toString());
        Assertions.assertEquals("/Users/ponfee/scm/gitee/a", path2.toPath().toString());
    }

}
