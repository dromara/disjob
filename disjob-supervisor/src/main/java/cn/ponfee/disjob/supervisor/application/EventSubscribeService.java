/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.param.supervisor.EventParam;
import cn.ponfee.disjob.core.param.supervisor.EventParam.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Event subscribe service
 *
 * @author Ponfee
 */
@Service
public class EventSubscribeService {
    private static final Logger LOG = LoggerFactory.getLogger(EventSubscribeService.class);

    private static final ConcurrentMap<Type, EventParam> CACHED = new ConcurrentHashMap<>();

    private final SchedGroupService schedGroupService;

    public EventSubscribeService(SchedGroupService schedGroupService) {
        this.schedGroupService = schedGroupService;

        long initialDelay = 5000 + ThreadLocalRandom.current().nextLong(5000);
        ThreadPoolExecutors.commonScheduledPool().scheduleWithFixedDelay(this::process, initialDelay, 5000, TimeUnit.MILLISECONDS);
    }

    public static void subscribe(EventParam param) {
        if (param != null && param.getType() != null) {
            // putIfAbsent不会更新param
            CACHED.compute(param.getType(), (k, v) -> param);
        }
    }

    private void process() {
        Set<Type> types = new HashSet<>(CACHED.keySet());
        for (Type type : types) {
            ThrowingRunnable.doCaught(() -> process(CACHED.remove(type)));
        }
    }

    private void process(EventParam param) {
        if (param == null) {
            return;
        }
        Type type = param.getType();

        if (type == Type.REFRESH_GROUP) {
            schedGroupService.refresh();

        } else {
            LOG.error("Unsupported publish event type: {}", type);
        }
    }

}
