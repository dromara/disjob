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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Collects test
 *
 * @author Ponfee
 */
public class CollectsTest {

    @Test
    public void testTruncate() {
        assertThat(Collects.truncate(null, 2)).isNull();
        assertThat(Collects.truncate(Collections.emptySet(), 2)).isEmpty();
        assertThat(Collects.truncate(Sets.newSet(1), 0)).size().isEqualTo(1);
        assertThat(Collects.truncate(Sets.newSet(1, 2, 3, 4, 5), 3)).size().isEqualTo(3);
    }

    @Test
    public void testNewArray() {
        List<List<Integer>> list = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(6, 7, 8));
        Integer[] array1 = list.stream().flatMap(List::stream).toArray(length -> Collects.newArray(Integer[].class, length));
        System.out.println(Arrays.toString(array1));
        assertThat(array1).hasSize(9);

        Object[] array2 = list.stream().flatMap(List::stream).toArray(length -> Collects.newArray(Object[].class, length));
        System.out.println(Arrays.toString(array2));
        assertThat(array2).hasSize(9);
    }

}
