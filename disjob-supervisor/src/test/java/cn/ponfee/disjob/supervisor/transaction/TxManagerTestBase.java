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
import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(TxManagerTestBase.class);

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
        LOG.info("-------------testWithoutTxHasError done " + before + ", " + after);
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
        LOG.info("-------------testWithAnnotationTxHasError done" + before + ", " + after);
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
        LOG.info("-------------testWithTemplateTxHasError done" + before + ", " + after);
    }

    @Test
    public void testWithoutTxNoneError() {
        Map<I, String> before = bean.queryData(id1, id2);
        bean.testWithoutTxNoneError(id1, id2);
        Map<I, String> after = bean.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertNotEquals(before.get(id2), after.get(id2));
        LOG.info("-------------testWithoutTxNoneError done" + before + ", " + after);
    }

    @Test
    public void testWithAnnotationTxNoneError() {
        Map<I, String> before = bean.queryData(id1, id2);
        bean.testWithAnnotationTxNoneError(id1, id2);
        Map<I, String> after = bean.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertNotEquals(before.get(id2), after.get(id2));
        LOG.info("-------------testWithAnnotationTxNoneError done" + before + ", " + after);
    }

    @Test
    public void testWithTemplateTxNoneError() {
        Map<I, String> before = bean.queryData(id1, id2);
        bean.testWithTemplateTxNoneError(id1, id2);
        Map<I, String> after = bean.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertNotEquals(before.get(id2), after.get(id2));
        LOG.info("-------------testWithTemplateTxNoneError done" + before + ", " + after);
    }

}
