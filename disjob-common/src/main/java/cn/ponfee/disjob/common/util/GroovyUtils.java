/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.collect.PooledObjectProcessor;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;

import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Groovy utility
 *
 * @author Ponfee
 */
public final class GroovyUtils {

    /**
     * Groovy class loader
     */
    private static final GroovyClassLoader CLASS_LOADER = new GroovyClassLoader();

    /**
     * Groovy compile class cache
     */
    private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    public static <T> Class<T> parseClass(String sourceCode) {
        String sha1 = DigestUtils.sha1Hex(sourceCode);
        return (Class<T>) CLASS_CACHE.computeIfAbsent(sha1, key -> CLASS_LOADER.parseClass(sourceCode));
    }

    /**
     * 通用方法调用方式：InvokerHelper.invokeMethod(object, methodName, arguments);
     */
    public enum Evaluator {

        /**
         * Groovy script based GroovyShell，使用自定义对象池处理器
         *
         * <p>方法调用方式一：groovyShell.invokeMethod(methodName, args);
         * <p>方法调用方式二：script.invokeMethod(methodName, args);
         */
        SHELL() {
            final GroovyShell groovyShell = new GroovyShell();
            final PooledObjectProcessor<String, Script> processor = new PooledObjectProcessor<>(10, groovyShell::parse);

            @Override
            protected <T> T evaluate(String scriptText, Map<String, Object> params) throws Exception {
                // return (T) new GroovyShell(new Binding(params)).evaluate(scriptText);
                return processor.process(scriptText, script -> {
                    script.setBinding(new Binding(params));
                    return (T) script.run();
                });
            }
        },

        /**
         * Groovy script based ScriptEngine，在GroovyScriptEngineImpl内部有classMap来缓存script
         *
         * <p>javax方式创建：new javax.script.ScriptEngineManager().getEngineByExtension("groovy");
         *
         * <p>方法调用方式一：((Invocable) scriptEngine).invokeFunction(methodName, args);
         * <p>方法调用方式二：((Invocable) scriptEngine).invokeMethod(null, methodName, args);
         */
        SCRIPT() {
            final GroovyScriptEngineFactory scriptEngineFactory = new GroovyScriptEngineFactory();

            @Override
            protected <T> T evaluate(String scriptText, Map<String, Object> params) throws Exception {
                ScriptEngine scriptEngine = scriptEngineFactory.getScriptEngine();
                return (T) scriptEngine.eval(scriptText, new SimpleBindings(params));
            }
        },

        /**
         * Groovy script based JavaClass，在#parseClass时会缓存class
         *
         * <p>方法调用：script.invokeMethod(methodName, args);
         */
        CLASS() {
            @Override
            protected <T> T evaluate(String scriptText, Map<String, Object> params) throws Exception {
                Class<?> clazz = parseClass(scriptText);
                Script script = (Script) clazz.newInstance();
                script.setBinding(new Binding(params));
                return (T) script.run();
            }
        },

        ;

        public final <T> T eval(String scriptText, Map<String, Object> params) throws Exception {
            return evaluate(scriptText, params == null ? Collections.emptyMap() : params);
        }

        protected abstract <T> T evaluate(String scriptText, Map<String, Object> params) throws Exception;
    }

}
