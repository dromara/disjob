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
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;

import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.TX_MANAGER_SPRING_BEAN_NAME;
import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.TX_TEMPLATE_SPRING_BEAN_NAME;

/**
 * test db_order_base
 *
 * @author Ponfee
 */
@Service
public class JobTxManagerTestService extends AbstractTxManagerTestService<SchedJob, Long> {

    public JobTxManagerTestService(SchedJobMapper mapper,
                                   @Qualifier(TX_TEMPLATE_SPRING_BEAN_NAME) TransactionTemplate transactionTemplate) {
        super(
            transactionTemplate,
            (id1, id2) -> mapper.testFindByJobIds(Arrays.asList(id1, id2)),
            mapper::testUpdateRemark,
            e -> Tuple2.of(e.getJobId(), e.getRemark())
        );
    }

    @Transactional(value = TX_MANAGER_SPRING_BEAN_NAME, rollbackFor = Exception.class)
    @Override
    public void testWithAnnotationTxHasError(Long id1, Long id2) {
        super.testWithAnnotationTxHasError(id1, id2);
    }

    @Transactional(value = TX_MANAGER_SPRING_BEAN_NAME, rollbackFor = Exception.class)
    @Override
    public void testWithAnnotationTxNoneError(Long id1, Long id2) {
        super.testWithAnnotationTxNoneError(id1, id2);
    }
}
