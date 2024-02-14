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

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Ponfee
 */
public class ScheduledExecutorTest {

    @Test
    public void testSubmit() throws InterruptedException {
        ScheduledThreadPoolExecutor registryScheduledExecutor = new ScheduledThreadPoolExecutor(1, ThreadPoolExecutors.DISCARD);
        registryScheduledExecutor.scheduleWithFixedDelay(() -> System.out.println("scheduled"), 500, 200, TimeUnit.MILLISECONDS);


        System.out.println(new Date());
        registryScheduledExecutor.submit(() -> System.out.println("submit"));
        registryScheduledExecutor.execute(() -> System.out.println("execute"));
        System.out.println(new Date());

        Thread.sleep(1000);
    }
}
