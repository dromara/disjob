/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import com.google.common.collect.ImmutableMap;
import groovy.lang.GroovyShell;
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
        assertThat((String) GroovyUtils.Evaluator. SHELL.eval(SCRIPT_TEXT, PARAMS)).isEqualTo(RESULT);
        assertThat((String) GroovyUtils.Evaluator.SCRIPT.eval(SCRIPT_TEXT, PARAMS)).isEqualTo(RESULT);
        assertThat((String) GroovyUtils.Evaluator. CLASS.eval(SCRIPT_TEXT, PARAMS)).isEqualTo(RESULT);

        String closureScript =
            "import cn.ponfee.disjob.common.util.Jsons; " +
            "{it -> Jsons.toJson(it.get('list')) + (it.get('a') + it.get('b')) + it.get('str').length()}";
        assertThat((String) GroovyUtils.Evaluator.CLOSURE.eval(closureScript, PARAMS)).isEqualTo(RESULT);
    }

    @Test
    public void testClosureAdd() {
        GroovyShell groovyShell = new GroovyShell();
        // Math::sqrt
        groovy.lang.Closure<?> closure = (groovy.lang.Closure<?>) groovyShell.parse("{a,b -> a+b}").run();
        Object result = closure.call(2, 3);
        System.out.println("type: " + result.getClass() + ", value: " + result);
    }

    @Test
    public void testClosureReduce() {
        GroovyShell groovyShell = new GroovyShell();
        // Math::sqrt
        groovy.lang.Closure<?> closure = (groovy.lang.Closure<?>) groovyShell.parse("{it -> it.stream().reduce(0, Integer::sum)}").run();
        Object result = closure.call(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        System.out.println("type: " + result.getClass() + ", value: " + result);
    }

    @Test
    public void testClosureSqrt() {
        GroovyShell groovyShell = new GroovyShell();
        // {it -> Math.sqrt(it)}
        groovy.lang.Closure<?> closure = (groovy.lang.Closure<?>) groovyShell.parse("Math::sqrt").run();
        Object result = closure.call(2);
        System.out.println("type: " + result.getClass() + ", value: " + result);
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
