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
import org.apache.commons.codec.digest.DigestUtils;
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
     * Groovy class loader
     */
    private static final GroovyClassLoader CLASS_LOADER = new GroovyClassLoader();

    /**
     * Groovy compile class cache
     */
    private static final ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

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
        String sha1 = DigestUtils.sha1Hex(sourceCode);
        return (Class<T>) CLASS_CACHE.computeIfAbsent(sha1, key -> CLASS_LOADER.parseClass(sourceCode));
    }

    /**
     * 通用方法调用方式：InvokerHelper.invokeMethod(object, methodName, arguments);
     */
    public enum Evaluator {

        /**
         * Groovy script closure
         */
        CLOSURE() {
            final ConcurrentMap<String, Script> scripCache = new ConcurrentHashMap<>();

            @Override
            protected <T> T evaluate(String scriptText, Map<String, Object> params) {
                Script script = scripCache.computeIfAbsent(scriptText, GROOVY_SHELL::parse);
                Closure<?> closure = (Closure<?>) script.run();
                return (T) closure.call(params);
            }
        },

        /**
         * Groovy script based GroovyShell，使用自定义对象池处理器
         *
         * <p>方法调用方式一：groovyShell.invokeMethod(methodName, args);
         * <p>方法调用方式二：script.invokeMethod(methodName, args);
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
