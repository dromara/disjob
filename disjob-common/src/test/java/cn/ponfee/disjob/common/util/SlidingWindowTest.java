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

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.collect.SlidingWindow;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * SlidingWindow test
 *
 * @author Ponfee
 */
@Disabled
class SlidingWindowTest {

    @Test
    void testSlidingWindow() throws InterruptedException {
        SlidingWindow limiter = new SlidingWindow(5, 1000);
        for (int i = 0; i < 100; i++) {
            if (limiter.tryAcquire()) {
                System.out.println("Request " + (i + 1) + ": Allowed");
            } else {
                System.out.println("Request " + (i + 1) + ": Denied");
            }
            Thread.sleep(50);
        }
    }

    @Test
    void testRateLimiter() throws InterruptedException {
        RateLimiter limiter = RateLimiter.create(5/*, Duration.ofSeconds(1)*/);
        for (int i = 0; i < 100; i++) {
            if (limiter.tryAcquire()) {
                System.out.println("Request " + (i + 1) + ": Allowed");
            } else {
                System.out.println("Request " + (i + 1) + ": Denied");
            }
            Thread.sleep(50);
        }
    }

}
