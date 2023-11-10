/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.spring.ResourceScanner;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.Fields;
import cn.ponfee.disjob.common.util.NetUtils;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.model.SchedJob;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Ponfee
 */
public class ObjectUtilsTest {

    private static final String TEST_NAME = ObjectUtils.uuid32();

    @Test
    public void testNewInstance() {
        Assertions.assertNotNull(NetUtils.getLocalHost());
        Assertions.assertTrue(new Boolean("True"));
        Assertions.assertFalse(new Boolean("1"));
        Assertions.assertFalse(new Boolean("0"));
        Assertions.assertFalse(ObjectUtils.newInstance(boolean.class));
        Assertions.assertFalse(ObjectUtils.newInstance(Boolean.class));
    }

    @Test
    public void testFields() {
        SchedJob job = new SchedJob();
        Assertions.assertNull(Fields.get(job, "id"));
        Long value = 1L;
        Fields.put(job, "id", value);
        Assertions.assertEquals(value, Fields.get(job, "id"));
    }

    @Test
    public void testModifyConstantFields() {
        String value1 = ObjectUtilsTest.TEST_NAME;
        String value2 = ObjectUtils.uuid32();
        Fields.put(ObjectUtilsTest.class, "TEST_NAME", value2);
        Assertions.assertNotEquals(value1, value2);
        Assertions.assertNotEquals(value1, ObjectUtilsTest.TEST_NAME);
        Assertions.assertEquals(value2, ObjectUtilsTest.TEST_NAME);
        Assertions.assertEquals(value2, Fields.get(ObjectUtilsTest.class, "TEST_NAME"));
    }

    @Test
    public void testStaticField() throws ClassNotFoundException {
        Supervisor supervisor = new Supervisor("127.0.0.1", 10);
        Class<?> aClass = Class.forName(Supervisor.class.getName() + "$Current");
        Fields.put(aClass, ClassUtils.getStaticField(aClass, "current"), supervisor);
        Assertions.assertEquals(supervisor, Supervisor.current());
    }


    @Test
    public void testStaticFinalMethod() {
        findlStaticFinalMethod("cn.ponfee.disjob.common");
        findlStaticFinalMethod("cn.ponfee.disjob.core");
        //findlStaticFinalMethod("cn.ponfee.disjob.id");
        findlStaticFinalMethod("cn.ponfee.disjob.worker");
        findlStaticFinalMethod("cn.ponfee.disjob.dispatch");
        findlStaticFinalMethod("cn.ponfee.disjob.registry");
    }

    public void findlStaticFinalMethod(String packageName) {
        for (Class<?> clazz : new ResourceScanner(packageName).scan4class()) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) && Modifier.isFinal(method.getModifiers())) {
                    String cname = method.getDeclaringClass().getName();
                    if (!cname.contains("$") && cname.startsWith("cn.ponfee")) {
                        System.err.println(clazz+"  "+method);
                    }
                }
            }
            for (Method method : clazz.getMethods()) {
                if (Modifier.isStatic(method.getModifiers()) && Modifier.isFinal(method.getModifiers())) {
                    String cname = method.getDeclaringClass().getName();
                    if (!cname.contains("$") && cname.startsWith("cn.ponfee")) {
                        System.err.println(clazz+"  "+method);
                    }
                }
            }
        }
    }

}
