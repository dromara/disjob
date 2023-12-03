/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
