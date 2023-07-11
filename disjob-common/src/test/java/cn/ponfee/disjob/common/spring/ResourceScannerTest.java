/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.tuple.Tuple;
import org.apache.commons.lang3.builder.Builder;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Resource scanner test
 *
 * @author Ponfee
 */
public class ResourceScannerTest {

    @Test
    public void test() {
        Set<Class<?>> classes = new ResourceScanner("/cn/ponfee/disjob/**/*.class").scan4class(new Class[]{Tuple.class}, null);
        assertEquals(6, classes.size());

        classes = new ResourceScanner("org/apache/commons/lang3/**/*.class").scan4class(new Class[]{Builder.class}, null);
        assertEquals(11, classes.size());

        String file = "copy-right.txt";
        Map<String, String> map = new ResourceScanner(file).scan4text();
        assertThat(map.get("copy-right.txt")).contains(" http://www.ponfee.cn ").startsWith("/* __________ ");
    }
}
