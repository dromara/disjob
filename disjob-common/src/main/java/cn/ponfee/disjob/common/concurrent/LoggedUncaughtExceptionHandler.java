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

import org.slf4j.Logger;

import java.util.Objects;

/**
 * <pre>
 * Logged uncaught exception handler
 *
 * 1、`ThreadPoolExecutor#submit()`会被封装成`FutureTask`，当抛异常时会通过`FutureTask#setException(ex)`将异常赋值到outcome变量中。
 *    异常信息不被打印，只有通过`FutureTask#get`获取结果时会重新抛出异常`throw new ExecutionException((Throwable)x);`
 *
 * 2、`ScheduledThreadPoolExecutor#scheduleWithFixedDelay()`会被封装成`ScheduledFutureTask`，
 *    当执行`Runnable#run`内部抛异常时，`ScheduledFutureTask.super.runAndReset()`方法返回false，导致不会设置下次执行时间
 * </pre>
 *
 * @author Ponfee
 */
public final class LoggedUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Logger log;

    public LoggedUncaughtExceptionHandler(Logger log) {
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof java.lang.ThreadDeath) {
            log.warn("Uncaught exception handle, thread death: {}, {}", t.getName(), e.getMessage());
        } else if (e instanceof InterruptedException) {
            log.warn("Uncaught exception handle, thread interrupted: {}, {}", t.getName(), e.getMessage());
        } else {
            log.error("Uncaught exception handle, occur error: " + t.getName(), e);
        }
    }

}
