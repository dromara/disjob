/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.worker.util;

import cn.ponfee.disjob.common.spring.ResourceScanner;
import cn.ponfee.disjob.common.util.Fields;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        String ls = Files.SYSTEM_LINE_SEPARATOR;
        StringBuilder builder = new StringBuilder(ls);
        List<String> list = JOB_HANDLER_MAP
            .entrySet()
            .stream()
            .map(e -> "|   " + e.getKey() + " -> " + e.getValue().getName())
            .collect(Collectors.toList());
        int maxLen = list.stream().mapToInt(String::length).max().getAsInt();

        builder.append(StringUtils.rightPad("/", maxLen + 1, "-")).append("\\").append(ls);
        builder.append(StringUtils.rightPad("| Job handler map:", maxLen, " ")).append(" |").append(ls);

        list.forEach(e -> builder.append(StringUtils.rightPad(e, maxLen, " ")).append(" |").append(ls));

        builder.append(StringUtils.rightPad("\\", maxLen + 1, "-")).append("/").append(ls);

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