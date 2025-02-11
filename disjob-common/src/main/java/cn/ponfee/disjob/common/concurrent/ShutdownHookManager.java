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

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;

import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Shutdown hook manager
 *
 * @author Ponfee
 */
public class ShutdownHookManager {

    private static volatile PriorityBlockingQueue<ShutdownTask> tasks;

    public static synchronized void addShutdownHook(int priority, ThrowingRunnable<?> task) {
        init();
        tasks.add(new ShutdownTask(priority, task));
    }

    private static void init() {
        if (tasks == null) {
            tasks = new PriorityBlockingQueue<>();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                while (!tasks.isEmpty()) {
                    ShutdownTask task = tasks.poll();
                    if (task != null) {
                        task.run();
                    }
                }
            }));
        }
    }

    private static class ShutdownTask implements Comparable<ShutdownTask>, Runnable {
        private final int priority;
        private final ThrowingRunnable<?> task;

        public ShutdownTask(int priority, ThrowingRunnable<?> task) {
            this.priority = priority;
            this.task = Objects.requireNonNull(task);
        }

        @Override
        public int compareTo(ShutdownTask other) {
            // 数值越小，优先级越高
            return Integer.compare(this.priority, other.priority);
        }

        @Override
        public void run() {
            ThrowingRunnable.doCaught(task, "Shutdown task execute failed: {}");
        }
    }

}
