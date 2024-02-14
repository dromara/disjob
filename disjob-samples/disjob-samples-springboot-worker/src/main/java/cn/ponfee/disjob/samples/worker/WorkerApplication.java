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

package cn.ponfee.disjob.samples.worker;

import cn.ponfee.disjob.samples.common.AbstractSamplesApplication;
import cn.ponfee.disjob.worker.configuration.EnableWorker;
import org.springframework.boot.SpringApplication;

/**
 * Worker application based spring boot
 *
 * banner:
 *  https://patorjk.com/software/taag/#p=display&h=1&v=1&f=Graffiti&t=Disjob
 *  https://patorjk.com/software/taag/#p=display&h=1&f=Big&t=Disjob
 *
 * @author Ponfee
 */
@EnableWorker
public class WorkerApplication extends AbstractSamplesApplication {

    static {
        // for log4j log file dir
        System.setProperty("app.name", "springboot-worker");
    }

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }

}
