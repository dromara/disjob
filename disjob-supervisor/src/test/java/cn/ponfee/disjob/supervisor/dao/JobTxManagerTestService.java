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

package cn.ponfee.disjob.supervisor.dao;

import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.transaction.AbstractTxManagerTestService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;

/**
 * test disjob transaction
 *
 * @author Ponfee
 */
@Service
public class JobTxManagerTestService extends AbstractTxManagerTestService<SchedJob, Long> {

    public JobTxManagerTestService(JobTxMapper jobTxMapper,
                                   @Qualifier(SupervisorDataSourceConfig.SPRING_BEAN_NAME_TX_TEMPLATE) TransactionTemplate transactionTemplate) {
        super(
            transactionTemplate,
            (id1, id2) -> jobTxMapper.findByJobIds(Arrays.asList(id1, id2)),
            jobTxMapper::updateRemark,
            e -> Tuple2.of(e.getJobId(), e.getRemark())
        );
    }

    @Transactional(value = SupervisorDataSourceConfig.SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    @Override
    public void testWithAnnotationTxHasError(Long id1, Long id2) {
        super.testWithAnnotationTxHasError(id1, id2);
    }

    @Transactional(value = SupervisorDataSourceConfig.SPRING_BEAN_NAME_TX_MANAGER, rollbackFor = Exception.class)
    @Override
    public void testWithAnnotationTxNoneError(Long id1, Long id2) {
        super.testWithAnnotationTxNoneError(id1, id2);
    }
}
