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

package cn.ponfee.disjob.registry.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base server registry autoConfiguration
 *
 * @author Ponfee
 */
public abstract class BaseServerRegistryAutoConfiguration {

    private static final AtomicBoolean MUTEX = new AtomicBoolean(false);

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected BaseServerRegistryAutoConfiguration() {
        if (MUTEX.compareAndSet(false, true)) {
            log.info("Enabled registry center.");
        } else {
            throw new Error("Registry center already imported.");
        }
    }

}
