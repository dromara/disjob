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

package cn.ponfee.disjob.samples.worker;

import cn.ponfee.disjob.common.spring.ResourceScanner;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.worker.executor.JobExecutor;
import cn.ponfee.disjob.worker.util.JobExecutorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * JobExecutor mapping
 *
 * @author Ponfee
 */
public class JobExecutorMapping {
    private static final Logger LOG = LoggerFactory.getLogger(JobExecutorMapping.class);
    private static final Map<String, Class<? extends JobExecutor>> JOB_EXECUTOR_MAP;

    static {
        Map<String, Class<? extends JobExecutor>> map = new HashMap<>();
        for (Class<?> clazz : new ResourceScanner("cn/ponfee/disjob/**/*.class").scan4class(new Class<?>[]{JobExecutor.class}, null)) {
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Class<? extends JobExecutor> type = (Class<? extends JobExecutor>) clazz;
            map.put(type.getName(), type);
            Component component = AnnotatedElementUtils.findMergedAnnotation(clazz, Component.class);
            if (component == null) {
                continue;
            }

            String beanName = component.value();
            if (StringUtils.isBlank(beanName)) {
                //beanName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, type.getSimpleName());
                beanName = Introspector.decapitalize(ClassUtils.getShortName(type.getName()));
            }
            map.put(beanName, type);
        }
        JOB_EXECUTOR_MAP = map;
    }

    static void init() {
        Function<Class<?>, String> classToString = c -> "class@" + c.getName();
        String newLine = Files.SYSTEM_LINE_SEPARATOR;
        int lMaxLen = JOB_EXECUTOR_MAP.keySet().stream().mapToInt(String::length).max().getAsInt();
        int rMaxLen = JOB_EXECUTOR_MAP.values().stream().mapToInt(e -> classToString.apply(e).length()).max().getAsInt();
        int contentLen = lMaxLen + rMaxLen + 9;

        StringBuilder builder = new StringBuilder(newLine);
        builder.append(StringUtils.rightPad("/", contentLen, "-")).append("\\").append(newLine);
        builder.append(StringUtils.rightPad("| JobExecutor mapping (name -> class):", contentLen, " ")).append("|").append(newLine);
        builder.append(StringUtils.rightPad("| ", contentLen, " ")).append("|").append(newLine);
        JOB_EXECUTOR_MAP.entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey(), classToString.apply(e.getValue())))
            .sorted(Comparator.<Entry<String, String>>nullsLast(Entry.comparingByValue()).thenComparing(Entry.comparingByKey()))
            .forEach(e -> builder
                .append("|   ").append(StringUtils.rightPad(e.getKey(), lMaxLen, " "))
                .append(" -> ")
                .append(StringUtils.rightPad(e.getValue(), rMaxLen, " ")).append(" |")
                .append(newLine)
            );
        builder.append(StringUtils.rightPad("\\", contentLen, "-")).append("/").append(newLine);

        System.out.println(builder);
    }

    /**
     * 由于是在非Spring环境，jobExecutor可能设置的是beanName，因此需要校正为真实的jobExecutor
     *
     * @param param     param object
     * @param fieldName field name
     */
    public static void correctParamJobExecutor(Object param, String fieldName) throws IllegalAccessException {
        String jobExecutor = (String) FieldUtils.readField(param, fieldName, true);
        try {
            JobExecutorUtils.loadJobExecutor(jobExecutor);
        } catch (Exception ex) {
            Class<? extends JobExecutor> executorClass = JOB_EXECUTOR_MAP.get(jobExecutor);
            if (executorClass != null) {
                // maybe spring bean name
                FieldUtils.writeField(param, fieldName, executorClass.getName(), true);
                LOG.info("Parse jobExecutor: [{}] -> [{}]", jobExecutor, executorClass.getName());
            } else {
                LOG.error("Parse jobExecutor error: {}, {}", param, fieldName);
            }
        }
    }

}
