/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.enums;

import cn.ponfee.disjob.common.base.IntValueEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Execute state test
 *
 * @author Ponfee
 */
public class ExecuteStateTest {

    @Test
    public void testOf() {
        assertThat(IntValueEnum.of(ExecuteState.class, 10)).isSameAs(ExecuteState.WAITING);
        assertThat(ExecuteState.of(10)).isSameAs(ExecuteState.WAITING);
    }
}
