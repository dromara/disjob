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

package cn.ponfee.disjob.common.collect;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding window
 *
 * @author TJxiaobao
 */
public class SlidingWindow {

    /**
     * Maximum number of requests
     */
    private final int maxRequests;
    /**
     * Window size (milliseconds)
     */
    private final long windowSizeInMillis;
    /**
     * Current request count
     */
    private final AtomicInteger requestCount;
    /**
     * Window start time
     */
    private volatile long windowStart;

    public SlidingWindow(int maxRequests, long windowSizeInMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInMillis;
        this.requestCount = new AtomicInteger(0);
        this.windowStart = System.currentTimeMillis();
    }

    /**
     * Try to obtain a request quota.
     */
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        if (now - windowStart > windowSizeInMillis) {
            windowStart = now;
            requestCount.set(0);
        }
        if (requestCount.get() < maxRequests) {
            requestCount.incrementAndGet();
            return true;
        }
        return false;
    }

}
