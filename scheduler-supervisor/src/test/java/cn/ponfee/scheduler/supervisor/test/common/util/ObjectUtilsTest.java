package cn.ponfee.scheduler.supervisor.test.common.util;

import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Fields;
import cn.ponfee.scheduler.common.util.Networks;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.model.SchedJob;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

/**
 * @author Ponfee
 */
public class ObjectUtilsTest {

    @Test
    public void testNewInstance() {
        Assert.assertNotNull(Networks.getHostIp());
        Assert.assertTrue(new Boolean("True"));
        Assert.assertFalse(new Boolean("1"));
        Assert.assertFalse(new Boolean("0"));
        Assert.assertFalse(ObjectUtils.newInstance(boolean.class));
        Assert.assertFalse(ObjectUtils.newInstance(Boolean.class));
    }

    @Test
    public void testFields() {
        SchedJob job = new SchedJob();
        Assert.assertNull(Fields.get(job, "id"));
        Long value = 1L;
        Fields.put(job, "id", value);
        Assert.assertEquals(value, Fields.get(job, "id"));
    }

    @Test
    public void testModifyConstantFields() {
        String value1 = ObjectUtilsTest.TEST_NAME;
        String value2 = ObjectUtils.uuid32();
        Fields.put(ObjectUtilsTest.class, "TEST_NAME", value2);
        Assert.assertNotEquals(value1, value2);
        Assert.assertNotEquals(value1, ObjectUtilsTest.TEST_NAME);
        Assert.assertEquals(value2, ObjectUtilsTest.TEST_NAME);
        Assert.assertEquals(value2, Fields.get(ObjectUtilsTest.class, "TEST_NAME"));
    }

    @Test
    public void testStaticField() throws ClassNotFoundException {
        Supervisor supervisor = new Supervisor("127.0.0.1", 10);
        Class<?> aClass = Class.forName(Supervisor.class.getName() + "$Current");
        Fields.put(aClass, ClassUtils.getStaticField(aClass, "current"), supervisor);
        Assertions.assertEquals(supervisor, Supervisor.current());
    }

    private final static String TEST_NAME = ObjectUtils.uuid32();
}
