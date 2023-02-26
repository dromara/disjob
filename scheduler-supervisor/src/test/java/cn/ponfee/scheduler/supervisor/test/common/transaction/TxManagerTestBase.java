/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.common.transaction;

import cn.ponfee.scheduler.common.base.tuple.Tuple2;
import cn.ponfee.scheduler.supervisor.SpringBootTestBase;
import cn.ponfee.scheduler.supervisor.config.AbstractTxManagerTestService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Supplier;

/**
 * test database transaction
 *
 * @param <S> service type
 * @param <I> table id type
 * @author Ponfee
 */
public abstract class TxManagerTestBase<S extends AbstractTxManagerTestService<?, I>, I> extends SpringBootTestBase<S> {

    private final I id1, id2;

    public TxManagerTestBase(I id1, I id2) {
        this.id1 = id1;
        this.id2 = id2;
    }

    public TxManagerTestBase(Supplier<Tuple2<I, I>> supplier) {
        Tuple2<I, I> tuple2 = supplier.get();
        this.id1 = tuple2.a;
        this.id2 = tuple2.b;
    }

    @Test
    public void testWithoutTxHasError() {
        Map<I, String> before = bean.queryData(id1, id2);
        try {
            bean.testWithoutTxHasError(id1, id2);
        } catch (Exception ignored) {
        }
        Map<I, String> after = bean.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertEquals(before.get(id2), after.get(id2));
        log.info("-------------testWithoutTxHasError done " + before + ", " + after);
    }

    @Test
    public void testWithAnnotationTxHasError() {
        Map<I, String> before = bean.queryData(id1, id2);
        try {
            bean.testWithAnnotationTxHasError(id1, id2);
        } catch (Exception ignored) {
        }
        Map<I, String> after = bean.queryData(id1, id2);
        Assertions.assertEquals(before.get(id1), after.get(id1));
        Assertions.assertEquals(before.get(id2), after.get(id2));
        log.info("-------------testWithAnnotationTxHasError done" + before + ", " + after);
    }

    @Test
    public void testWithTemplateTxHasError() {
        Map<I, String> before = bean.queryData(id1, id2);
        try {
            bean.testWithTemplateTxHasError(id1, id2);
        } catch (Exception ignored) {
        }
        Map<I, String> after = bean.queryData(id1, id2);
        Assertions.assertEquals(before.get(id1), after.get(id1));
        Assertions.assertEquals(before.get(id2), after.get(id2));
        log.info("-------------testWithTemplateTxHasError done" + before + ", " + after);
    }

    @Test
    public void testWithoutTxNoneError() {
        Map<I, String> before = bean.queryData(id1, id2);
        bean.testWithoutTxNoneError(id1, id2);
        Map<I, String> after = bean.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertNotEquals(before.get(id2), after.get(id2));
        log.info("-------------testWithoutTxNoneError done" + before + ", " + after);
    }

    @Test
    public void testWithAnnotationTxNoneError() {
        Map<I, String> before = bean.queryData(id1, id2);
        bean.testWithAnnotationTxNoneError(id1, id2);
        Map<I, String> after = bean.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertNotEquals(before.get(id2), after.get(id2));
        log.info("-------------testWithAnnotationTxNoneError done" + before + ", " + after);
    }

    @Test
    public void testWithTemplateTxNoneError() {
        Map<I, String> before = bean.queryData(id1, id2);
        bean.testWithTemplateTxNoneError(id1, id2);
        Map<I, String> after = bean.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertNotEquals(before.get(id2), after.get(id2));
        log.info("-------------testWithTemplateTxNoneError done" + before + ", " + after);
    }

}
