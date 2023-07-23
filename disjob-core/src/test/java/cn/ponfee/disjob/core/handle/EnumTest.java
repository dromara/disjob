/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.common.base.IntValueEnum;
import cn.ponfee.disjob.common.spring.ResourceScanner;
import cn.ponfee.disjob.core.enums.TriggerType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test enum
 *
 * @author Ponfee
 */
public class EnumTest {

    private static final Map<String, Class<IntValueEnum<?>>> ENUM_MAP = new ResourceScanner("cn/ponfee/disjob/**/*.class")
        .scan4class(new Class<?>[]{IntValueEnum.class}, null)
        .stream()
        // cn.ponfee.disjob.core.enums.TriggerType$1：Class#getSimpleName为空串，e.isAnonymousClass()为true
        .filter(e -> e.isEnum() && !e.isAnonymousClass())
        .collect(Collectors.toMap(Class::getSimpleName, e -> (Class<IntValueEnum<?>>) e));

    @Test
    public void testLoad() {
        Set<Class<?>> classes = new ResourceScanner("cn/ponfee/disjob/**/*.class").scan4class(new Class<?>[]{IntValueEnum.class}, null);
        classes.forEach(System.out::println);
        System.out.println("\n------------\n");
        classes.forEach(e -> System.out.println("|" + e.getSimpleName() + "| => " + e.isAnonymousClass() + "," + e.isLocalClass() + "," + e.isMemberClass()));
    }

    @Test
    public void testValues() throws ClassNotFoundException {
        Class<?> type1 = TriggerType.class;
        assertTrue(type1.isEnum());
        assertTrue(Modifier.isAbstract(type1.getModifiers()));
        assertFalse(type1.isAnonymousClass());

        Class<?> type2 = Class.forName("cn.ponfee.disjob.core.enums.TriggerType$1");
        System.out.println(type2);
        assertFalse(type2.isEnum());
        assertFalse(Modifier.isAbstract(type2.getModifiers()));
        assertTrue(type2.isAnonymousClass());
        assertThat(type2.getSimpleName()).isEmpty();

        assertNotNull(ENUM_MAP.get("TriggerType"));
        assertNull(ENUM_MAP.get("TriggerType$1"));
    }

}
