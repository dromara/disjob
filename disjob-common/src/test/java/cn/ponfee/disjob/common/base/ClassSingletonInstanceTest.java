/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ClassSingletonInstanceTest
 *
 * @author Ponfee
 */
public class ClassSingletonInstanceTest {

    @Test
    void test() {
        Assertions.assertThat(new SingletonInstanceSample()).isNotNull();
        for (int i = 0; i < 2; i++) {
            Assertions.assertThatThrownBy(SingletonInstanceSample::new)
                .isInstanceOf(Error.class)
                .hasMessageMatching("^Class '.+' instance already created.$");
        }
    }

    static class SingletonInstanceSample extends AbstractClassSingletonInstance {

    }

}
