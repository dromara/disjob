package cn.ponfee.scheduler.supervisor.test.common.transaction;

import cn.ponfee.scheduler.supervisor.config.AbstractTxManagerTestService;
import cn.ponfee.scheduler.supervisor.test.SpringBootTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * test database transaction
 *
 * @param <S> service type
 * @param <I> table id type
 * @author Ponfee
 */
public abstract class AbstractTxManagerTest<S extends AbstractTxManagerTestService<?, I>, I> extends SpringBootTestBase<S> {

    private final Logger logger = LoggerFactory.getLogger(AbstractTxManagerTest.class);

    private final I id1, id2;
    private AbstractTxManagerTestService<?, I> service;

    public AbstractTxManagerTest(I id1, I id2) {
        this.id1 = id1;
        this.id2 = id2;
    }

    @Override
    protected void initiate() {
        service = bean();
    }

    @Test
    public void testWithoutTxHasError() {
        Map<I, String> before = service.queryData(id1, id2);
        try {
            service.testWithoutTxHasError(id1, id2);
        } catch (Exception ignored) {
        }
        Map<I, String> after = service.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertEquals(before.get(id2), after.get(id2));
        logger.info("-------------testWithoutTxHasError done " + before + ", " + after);
    }

    @Test
    public void testWithAnnotationTxHasError() {
        Map<I, String> before = service.queryData(id1, id2);
        try {
            service.testWithAnnotationTxHasError(id1, id2);
        } catch (Exception ignored) {
        }
        Map<I, String> after = service.queryData(id1, id2);
        Assertions.assertEquals(before.get(id1), after.get(id1));
        Assertions.assertEquals(before.get(id2), after.get(id2));
        logger.info("-------------testWithAnnotationTxHasError done" + before + ", " + after);
    }

    @Test
    public void testWithTemplateTxHasError() {
        Map<I, String> before = service.queryData(id1, id2);
        try {
            service.testWithTemplateTxHasError(id1, id2);
        } catch (Exception ignored) {
        }
        Map<I, String> after = service.queryData(id1, id2);
        Assertions.assertEquals(before.get(id1), after.get(id1));
        Assertions.assertEquals(before.get(id2), after.get(id2));
        logger.info("-------------testWithTemplateTxHasError done" + before + ", " + after);
    }

    @Test
    public void testWithoutTxNoneError() {
        Map<I, String> before = service.queryData(id1, id2);
        service.testWithoutTxNoneError(id1, id2);
        Map<I, String> after = service.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertNotEquals(before.get(id2), after.get(id2));
        logger.info("-------------testWithoutTxNoneError done" + before + ", " + after);
    }

    @Test
    public void testWithAnnotationTxNoneError() {
        Map<I, String> before = service.queryData(id1, id2);
        service.testWithAnnotationTxNoneError(id1, id2);
        Map<I, String> after = service.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertNotEquals(before.get(id2), after.get(id2));
        logger.info("-------------testWithAnnotationTxNoneError done" + before + ", " + after);
    }

    @Test
    public void testWithTemplateTxNoneError() {
        Map<I, String> before = service.queryData(id1, id2);
        service.testWithTemplateTxNoneError(id1, id2);
        Map<I, String> after = service.queryData(id1, id2);
        Assertions.assertNotEquals(before.get(id1), after.get(id1));
        Assertions.assertNotEquals(before.get(id2), after.get(id2));
        logger.info("-------------testWithTemplateTxNoneError done" + before + ", " + after);
    }

}
