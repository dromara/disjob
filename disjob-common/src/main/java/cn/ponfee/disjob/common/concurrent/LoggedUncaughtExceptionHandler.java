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

/**
 * Logged uncaught exception handler
 *
 * @author Ponfee
 */
public final class LoggedUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Logger log;

    public LoggedUncaughtExceptionHandler(Logger log) {
        this.log = log;
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
