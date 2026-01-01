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

package cn.ponfee.disjob.worker.util;

import cn.ponfee.disjob.common.collect.PooledObjectProcessor;
import groovy.lang.*;
import groovy.util.Expando;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Groovy utility
 *
 * @author Ponfee
 */
@SuppressWarnings("unchecked")
public final class GroovyUtils {

    private static final Logger LOG = LoggerFactory.getLogger(GroovyUtils.class);

    /**
     * Pattern of qualified class name
     */
    private static final Pattern QUALIFIED_CLASS_NAME_PATTERN = Pattern.compile("^([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*$");

    /**
     * Groovy compile class cache
     */
    private static final ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    /**
     * Groovy class loader
     */
    private static final GroovyClassLoader CLASS_LOADER = new GroovyClassLoader();

    /**
     * Groovy shell
     */
    private static final GroovyShell GROOVY_SHELL = new GroovyShell();

    /**
     * Returns class object for text, can be class qualifier name or source code
     *
     * @param text the class qualifier name or source code
     * @return class object
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        text = text.trim();
        if (QUALIFIED_CLASS_NAME_PATTERN.matcher(text).matches()) {
            try {
                return (Class<T>) Class.forName(text);
            } catch (Exception ignored) {
                // ignored
            }
        }
        try {
            return parseClass(text);
        } catch (Exception e) {
            LOG.warn("Parse source class code occur error.", e);
            return null;
        }
    }

    public static <T> Class<T> parseClass(String sourceCode) {
        return (Class<T>) CLASS_CACHE.computeIfAbsent(sourceCode, CLASS_LOADER::parseClass);
    }

    /**
     * <pre>
     * 1）调用script中声明的方法：scriptObject.invokeMethod(methodName, args);
     * 2）通用方法调用方式：InvokerHelper.invokeMethod(scriptObject, methodName, args);
     * 3）调用script中声明的方法：((Invocable) scriptEngine).invokeMethod(scriptObject, methodName, args);
     * </pre>
     */
    public enum Evaluator {

        /**
         * <pre>
         * Groovy closure script lambda format: `{ (arg_0, ..., arg_n) -> expression }`
         *
         * The current closure script lambda only supported three format:
         *  1) { it -> expression }, `it` arg is null
         *  2) { () -> expression }
         *  3) {    -> expression }
         * </pre>
         */
        CLOSURE() {
            final ConcurrentMap<String, Closure<?>> cache = new ConcurrentHashMap<>();

            @Override
            protected <T> T evaluate(String scriptText, Map<String, Object> params) {
                Closure<?> closure = cache.computeIfAbsent(scriptText, e -> (Closure<?>) GROOVY_SHELL.parse(e).run());
                Closure<?> rehydrateClosure = closure.rehydrate(new Expando(), null, null);
                rehydrateClosure.setResolveStrategy(Closure.DELEGATE_ONLY);
                params.forEach(rehydrateClosure::setProperty);
                // 调用call方法传入的参数即是lambda闭包参数，无参就为null：{ it -> it==null }
                return (T) rehydrateClosure.call();
            }
        },

        /**
         * Groovy script based GroovyShell，使用自定义对象池处理器
         */
        SHELL() {
            final PooledObjectProcessor<String, Script> pool = new PooledObjectProcessor<>(10, GROOVY_SHELL::parse);

            @Override
            protected <T> T evaluate(String scriptText, Map<String, Object> params) throws Exception {
                // return (T) new GroovyShell(new Binding(params)).evaluate(scriptText);
                return pool.process(scriptText, script -> {
                    script.setBinding(new Binding(params));
                    return (T) script.run();
                });
            }
        },

        /**
         * Groovy script based ScriptEngine，在GroovyScriptEngineImpl内部有classMap来缓存script
         * <p>javax方式创建：new javax.script.ScriptEngineManager().getEngineByExtension("groovy");
         */
        SCRIPT() {
            final ScriptEngine scriptEngine = new GroovyScriptEngineFactory().getScriptEngine();

            @Override
            protected <T> T evaluate(String scriptText, Map<String, Object> params) throws Exception {
                return (T) scriptEngine.eval(scriptText, new SimpleBindings(params));
            }
        },

        /**
         * Groovy script based JavaClass，在#parseClass时会缓存class
         */
        CLASS() {
            @Override
            protected <T> T evaluate(String scriptText, Map<String, Object> params) throws Exception {
                Script script = (Script) parseClass(scriptText).newInstance();
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
