/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
