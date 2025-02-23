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

package cn.ponfee.disjob.common.concurrent;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ScheduledTaskTest
 *
 * @author Ponfee
 */
public class ScheduledTaskTest {

    @Test
    public void testException() throws InterruptedException, ExecutionException {
        NamedThreadFactory threadFactory = NamedThreadFactory.builder()
            .prefix("scheduled_task_test")
            .priority(Thread.MAX_PRIORITY)
            .build();
        ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(5, threadFactory);

        ScheduledFuture<?> schedule1 = scheduledExecutor.schedule(() -> System.out.println("timeout 1"), 1, TimeUnit.SECONDS);
        ScheduledFuture<?> schedule2 = scheduledExecutor.schedule(() -> {
            int i = 1 / 0;
            System.out.println("timeout 2");
        }, 2, TimeUnit.SECONDS);
        ScheduledFuture<?> schedule3 = scheduledExecutor.schedule(() -> System.out.println("timeout 3"), 3, TimeUnit.SECONDS);

        Thread.sleep(5000);

        Assertions.assertThat(schedule1.isDone()).isTrue();
        Assertions.assertThat(schedule2.isDone()).isTrue();
        Assertions.assertThat(schedule3.isDone()).isTrue();

        Assertions.assertThat(schedule1.get()).isNull();
        Assertions.assertThatThrownBy(schedule2::get).isInstanceOf(ExecutionException.class).hasMessage("java.lang.ArithmeticException: / by zero");
        Assertions.assertThat(schedule3.get()).isNull();
    }

    @Test
    public void testSubmit() throws InterruptedException {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
        executor.scheduleWithFixedDelay(() -> System.out.println("scheduled"), 500, 200, TimeUnit.MILLISECONDS);

        System.out.println("\n");
        executor.submit(() -> System.out.println("submit"));
        executor.execute(() -> System.out.println("execute"));
        System.out.println("\n");

        Thread.sleep(1000);
    }

}
