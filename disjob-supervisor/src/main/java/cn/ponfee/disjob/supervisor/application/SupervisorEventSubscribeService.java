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

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.supervisor.base.SupervisorEvent;
import cn.ponfee.disjob.supervisor.base.SupervisorEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
public class SupervisorEventSubscribeService extends SingletonClassConstraint {
    private static final Logger LOG = LoggerFactory.getLogger(SupervisorEventSubscribeService.class);

    private static final ConcurrentMap<Type, SupervisorEvent> MAP = new ConcurrentHashMap<>();

    private final SchedGroupService groupService;

    public SupervisorEventSubscribeService(SchedGroupService groupService) {
        this.groupService = groupService;

        long initialDelay = 5000 + ThreadLocalRandom.current().nextLong(5000);
        ThreadPoolExecutors.commonScheduledPool().scheduleWithFixedDelay(this::process, initialDelay, 5000, TimeUnit.MILLISECONDS);
    }

    public static void subscribe(SupervisorEvent event) {
        if (event != null && event.getType() != null) {
            // add or update value
            MAP.put(event.getType(), event);
        }
    }

    private void process() {
        List<Type> types = new ArrayList<>(MAP.keySet());
        for (Type type : types) {
            ThrowingRunnable.doCaught(() -> process(MAP.remove(type)));
        }
    }

    private void process(SupervisorEvent event) {
        if (event == null) {
            return;
        }
        Type type = event.getType();

        if (type == Type.REFRESH_GROUP) {
            groupService.refresh();

        } else {
            LOG.error("Unsupported subscribe event type: {}", type);
        }
    }

}
