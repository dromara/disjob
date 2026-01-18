/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.worker.util;

import cn.ponfee.disjob.common.util.UuidUtils;
import com.google.common.collect.ImmutableMap;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GroovyUtils test
 *
 * @author Ponfee
 */
public class GroovyUtilsTest {

    private static final GroovyShell groovyShell = new GroovyShell();
    private static final ScriptEngine scriptEngine = new GroovyScriptEngineFactory().getScriptEngine();

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
    public void testEvaluator() throws Exception {
        assertThat((String) GroovyUtils.Evaluator.SHELL.eval(SCRIPT_TEXT, PARAMS)).isEqualTo(RESULT);
        assertThat((String) GroovyUtils.Evaluator.SCRIPT.eval(SCRIPT_TEXT, PARAMS)).isEqualTo(RESULT);
        assertThat((String) GroovyUtils.Evaluator.CLASS.eval(SCRIPT_TEXT, PARAMS)).isEqualTo(RESULT);

        // test closure: { it -> expression }
        assertThat((String) GroovyUtils.Evaluator.CLOSURE.eval("import cn.ponfee.disjob.common.util.Jsons; { it -> Jsons.toJson(list)+(a+b)+str.length() }", PARAMS)).isEqualTo(RESULT);
        assertThat((Boolean) GroovyUtils.Evaluator.CLOSURE.eval("{ it -> a>0 && b<3 && str=='string' }", PARAMS)).isEqualTo(true);

        // test closure: { it -> "${ expression }" }
        Object result = GroovyUtils.Evaluator.CLOSURE.eval("{ it -> \"${ a>0 && b<3 && str=='string' }\" }", PARAMS);
        assertThat(result).isInstanceOf(org.codehaus.groovy.runtime.GStringImpl.class);
        assertThat(result.toString()).isEqualTo("true");
    }

    @Test
    public void testScriptInvoke() throws Exception {
        // Method can be static
        Script scriptObject = groovyShell.parse("int sum(int a, int b) { return a + b }");
        Assertions.assertEquals(3, scriptObject.invokeMethod("sum", new Object[]{1, 2}));
        Assertions.assertEquals(3, InvokerHelper.invokeMethod(scriptObject, "sum", new Object[]{1, 2}));
        Assertions.assertEquals(3, ((Invocable) scriptEngine).invokeMethod(scriptObject, "sum", 1, 2));

        Assertions.assertEquals(3, groovyShell.invokeMethod("evaluate", new Object[]{"1+2"}));
        Assertions.assertEquals(3, scriptEngine.eval("1+2"));
        //Assertions.assertEquals("Script1@4f0100a7", ((Invocable) scriptEngine).invokeFunction("toString"));
        ((Invocable) scriptEngine).invokeFunction("println", "Hello World!");
    }

    @Test
    public void testClosureImport() {
        String closureScript =
            "import cn.ponfee.disjob.common.util.Jsons; " +
            "{ it -> Jsons.toJson(it.get('list')) + (it.get('a') + it.get('b')) + it.get('str').length() }";
        Closure<?> closure = (Closure<?>) groovyShell.parse(closureScript).run();
        assertThat((String) closure.call(PARAMS)).isEqualTo(RESULT);
    }

    @Test
    public void testClosureLambda() {
        Closure<?> closure = (Closure<?>) groovyShell.parse("{ a,b -> a+b }").run();
        Object result = closure.call(2, 3);
        System.out.println("type: " + result.getClass() + ", value: " + result);
        assertThat(result).isEqualTo(5);

        closure = (Closure<?>) groovyShell.parse("{ (a,b) -> a+b }").run();
        result = closure.call(2, 3);
        System.out.println("type: " + result.getClass() + ", value: " + result);
        assertThat(result).isEqualTo(5);

        // { (a,b) -> Integer.sum(a,b) }
        closure = (Closure<?>) groovyShell.parse("Integer::sum").run();
        result = closure.call(2, 3);
        System.out.println("type: " + result.getClass() + ", value: " + result);
        assertThat(result).isEqualTo(5);

        // { it -> Math.sqrt(it) }
        closure = (Closure<?>) groovyShell.parse("Math::sqrt").run();
        result = closure.call(2);
        System.out.println("type: " + result.getClass() + ", value: " + result);
        assertThat(Math.abs((double) result - Math.sqrt(2)) < 0.000000000001D).isTrue();

        // stream reduce
        closure = (Closure<?>) groovyShell.parse("{ it -> it.stream().reduce(0, Integer::sum) }").run();
        result = closure.call(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        System.out.println("type: " + result.getClass() + ", value: " + result);
        assertThat(result).isEqualTo(45);
    }

    @Test
    public void testThreadSafe_CLOSURE() throws Exception {
        // { it -> str.hashCode() }
        // { () -> str.hashCode() }
        // {    -> str.hashCode() }
        multithreadEval("{ () -> str.hashCode() }", GroovyUtils.Evaluator.CLOSURE::eval);
    }

    @Test
    public void testThreadSafe_SHELL() throws Exception {
        multithreadEval("str.hashCode()", GroovyUtils.Evaluator.SHELL::eval);
    }

    @Test
    public void testThreadSafe_SCRIPT() throws Exception {
        multithreadEval("str.hashCode()", GroovyUtils.Evaluator.SCRIPT::eval);
    }

    @Test
    public void testThreadSafe_CLASS() throws Exception {
        multithreadEval("str.hashCode()", GroovyUtils.Evaluator.CLASS::eval);
    }

    // ---------------------------------------------------------------------private methods

    private static void multithreadEval(String script, EvalFunction func) throws InterruptedException {
        AtomicInteger failedCounter = new AtomicInteger(0);

        Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int x = 0; x < 1000; x++) {
                    String str = UuidUtils.uuid22();
                    try {
                        Thread.sleep(1L);
                        int hashCode = func.apply(script, ImmutableMap.of("str", str));
                        if (hashCode != str.hashCode()) {
                            failedCounter.incrementAndGet();
                            System.err.println("eval '" + str + "' failed: " + hashCode + "!=" + str.hashCode());
                        }
                    } catch (Exception e) {
                        failedCounter.incrementAndGet();
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        Assertions.assertEquals(0, failedCounter.get());
    }

    private interface EvalFunction {
        int apply(String script, Map<String, Object> params) throws Exception;
    }

}
