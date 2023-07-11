/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.worker.util;

import cn.ponfee.disjob.common.spring.ResourceScanner;
import cn.ponfee.disjob.core.handle.JobHandler;
import com.google.common.base.CaseFormat;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Job handler holder
 *
 * @author Ponfee
 */
public class JobHandlerHolder {

    private static final Map<String, Class<? extends JobHandler<?>>> JOB_HANDLER_MAP;

    static {
        Map<String, Class<? extends JobHandler<?>>> map = new HashMap<>();
        for (Class<?> clazz : new ResourceScanner("**/*.class").scan4class(new Class<?>[]{JobHandler.class}, null)) {
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }
            Class<? extends JobHandler<?>> type = (Class<? extends JobHandler<?>>) clazz;
            map.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, type.getSimpleName()), type);
            Component component = AnnotatedElementUtils.findMergedAnnotation(clazz, Component.class);
            if (component != null) {
                map.put(component.value(), type);
            }
        }
        JOB_HANDLER_MAP = map;
    }

    public static Class<? extends JobHandler<?>> getJobHandler(String jobHandler) {
        return JOB_HANDLER_MAP.get(jobHandler);
    }

}
