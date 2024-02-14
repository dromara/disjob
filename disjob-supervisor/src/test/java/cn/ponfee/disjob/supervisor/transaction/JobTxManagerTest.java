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

package cn.ponfee.disjob.supervisor.transaction;

import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * test job transaction
 *
 * @author Ponfee
 */
public class JobTxManagerTest extends TxManagerTestBase<JobTxManagerTestService, Long> {

    public JobTxManagerTest(@Autowired SchedJobMapper schedJobMapper) {
        super(() -> {
            List<Long> list = schedJobMapper.testListLimit(2);
            if (list.size() < 2) {
                throw new IllegalStateException("Not find enough sched job data.");
            }
            return Tuple2.of(list.get(0), list.get(1));
        });
    }

}
