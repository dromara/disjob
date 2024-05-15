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

package cn.ponfee.disjob.samples.worker.util;

import cn.ponfee.disjob.common.spring.ResourceScanner;
import cn.ponfee.disjob.common.util.Fields;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import org.apache.commons.lang3.StringUtils;
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

/**
 * Job handler parser
 *
 * @author Ponfee
 */
public class JobHandlerParser {
    private static final Logger LOG = LoggerFactory.getLogger(JobHandlerParser.class);
    private static final Map<String, Class<? extends JobHandler>> JOB_HANDLER_MAP;

    static {
        Map<String, Class<? extends JobHandler>> map = new HashMap<>();
        for (Class<?> clazz : new ResourceScanner("cn/ponfee/disjob/**/*.class").scan4class(new Class<?>[]{JobHandler.class}, null)) {
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            Class<? extends JobHandler> type = (Class<? extends JobHandler>) clazz;
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
        JOB_HANDLER_MAP = map;
    }

    public static void init() {
        String newLine = Files.SYSTEM_LINE_SEPARATOR;
        int lMaxLen = JOB_HANDLER_MAP.keySet().stream().mapToInt(String::length).max().getAsInt();
        int rMaxLen = JOB_HANDLER_MAP.values().stream().mapToInt(e -> e.getName().length()).max().getAsInt();
        int contentLen = lMaxLen + rMaxLen + 9;

        StringBuilder builder = new StringBuilder(newLine);
        builder.append(StringUtils.rightPad("/", contentLen, "-")).append("\\").append(newLine);
        builder.append(StringUtils.rightPad("| Job handler mapping:", contentLen, " ")).append("|").append(newLine);

        JOB_HANDLER_MAP.entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey(), e.getValue().getName()))
            .sorted(Comparator.<Entry<String, String>>nullsLast(Entry.comparingByValue()).thenComparing(Entry.comparingByKey()))
            .forEach(pair -> builder
                .append("|   ").append(StringUtils.rightPad(pair.getKey(), lMaxLen, " "))
                .append(" -> ")
                .append(StringUtils.rightPad(pair.getKey(), rMaxLen, " ")).append(" |")
                .append(newLine)
            );
        builder.append(StringUtils.rightPad("\\", contentLen, "-")).append("/").append(newLine);

        System.out.println(builder);
    }

    /**
     * 仅限测试环境使用
     *
     * @param param     object
     * @param fieldName field name
     */
    public static void parse(Object param, String fieldName) {
        String jobHandler = (String) Fields.get(param, fieldName);
        try {
            JobHandlerUtils.load(jobHandler);
        } catch (Exception ex) {
            Class<? extends JobHandler> handlerClass = JOB_HANDLER_MAP.get(jobHandler);
            if (handlerClass != null) {
                // maybe spring bean name
                Fields.put(param, fieldName, handlerClass.getName());
                LOG.info("Parse jobHandler: [{}] -> [{}]", jobHandler, handlerClass.getName());
            }
        }
    }

}
