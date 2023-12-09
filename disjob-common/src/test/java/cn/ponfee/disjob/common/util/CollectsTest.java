/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.collect.Collects;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Collects test
 *
 * @author Ponfee
 */
public class CollectsTest {

    @Test
    public void test() {
        assertThat(Collects.truncate(null, 2)).isNull();
        assertThat(Collects.truncate(Collections.emptySet(), 2)).isEmpty();
        assertThat(Collects.truncate(Sets.newSet(1), 0)).size().isEqualTo(1);
        assertThat(Collects.truncate(Sets.newSet(1, 2, 3, 4, 5), 3)).size().isEqualTo(3);
    }

}
