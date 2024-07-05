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

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.dto.supervisor.UpdateTaskWorkerParam;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedTaskMapper;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Ponfee
 */
public class DistributedJobManagerTest extends SpringBootTestBase<SchedJobMapper> {

    @Resource
    private IdGenerator idGenerator;

    @Resource
    private SchedTaskMapper taskMapper;

    @Resource
    private DistributedJobManager distributedJobManager;

    @Test
    public void testUpdateTaskWorkerNonDeadlock() throws Throwable {
        long instanceId = idGenerator.generateId();
        int count = 197;
        List<SchedTask> tasks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SchedTask task = new SchedTask();
            task.setTaskId(idGenerator.generateId());
            task.setInstanceId(instanceId);
            task.setTaskNo(i);
            task.setTaskCount(count);
            task.setExecuteState(ExecuteState.WAITING.value());
            task.setCreatedAt(new Date());
            task.setUpdatedAt(new Date());
            tasks.add(task);
        }
        taskMapper.batchInsert(tasks);

        Runnable runnable = () -> {
            List<UpdateTaskWorkerParam> list = tasks.stream()
                .map(e -> new UpdateTaskWorkerParam(e.getTaskId(), new Worker("g", UuidUtils.uuid32(), "127.0.0.1", 80)))
                .collect(Collectors.toList());
            for (int i = 0; i < 5; i++) {
                Collections.shuffle(list);
                List<UpdateTaskWorkerParam> subList = new ArrayList<>(list.subList(0, list.size() / 2));
                Collections.shuffle(subList);
                distributedJobManager.updateTaskWorker(subList);
            }
        };

        AtomicReference<Throwable> exceptionHolder = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(runnable);
            thread.setUncaughtExceptionHandler((t, e) -> exceptionHolder.compareAndSet(null, e));
            threads.add(thread);
        }

        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }

        Throwable throwable = exceptionHolder.get();
        if (throwable != null) {
            throw throwable;
        }
    }

}
