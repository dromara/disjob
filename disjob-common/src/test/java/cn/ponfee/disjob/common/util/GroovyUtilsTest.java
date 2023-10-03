/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GroovyUtils test
 *
 * @author Ponfee
 */
public class GroovyUtilsTest {

    private static final String SCRIPT_TEXT =
        "package cn.ponfee.disjob;                     \n" +
        "import cn.ponfee.disjob.common.util.Jsons;    \n" +
        "return Jsons.toJson(list)+(a+b)+str.length(); \n" ;

    private static final Map<String, Object> PARAMS = ImmutableMap.of(
        "list", Arrays.asList("x", "y"),
        "a", 1,
        "b", 2,
        "str", "string"
    );

    private static final String RESULT = "[\"x\",\"y\"]36";

    @Test
    public void test() throws Exception {
        assertThat(GroovyUtils.Evaluator.SHELL .eval(SCRIPT_TEXT, PARAMS).toString()).isEqualTo(RESULT);
        assertThat(GroovyUtils.Evaluator.SCRIPT.eval(SCRIPT_TEXT, PARAMS).toString()).isEqualTo(RESULT);
        assertThat(GroovyUtils.Evaluator.CLASS .eval(SCRIPT_TEXT, PARAMS).toString()).isEqualTo(RESULT);
    }

    @Test
    public void testPooledGroovyShell() throws Exception {
        Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int x = 0; x < 5; x++) {
                    try {
                        GroovyUtils.Evaluator.SHELL.eval(SCRIPT_TEXT, PARAMS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }

        Thread.sleep(1000);
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("end");
    }

}
