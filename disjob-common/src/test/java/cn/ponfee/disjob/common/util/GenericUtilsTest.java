/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * GenericUtilsTest
 *
 * @author Ponfee
 */
class GenericUtilsTest {

    /**
     * getGenericTypes(Bean) 顺序为：[Sup&lt;String&gt;, If1&lt;Integer&gt;, If2&lt;Long, Short&gt;]，
     * 各参数化类型的实参扁平索引为：0-&gt;String, 1-&gt;Integer, 2-&gt;Long, 3-&gt;Short。
     * <p>当查询的 genericArgsIndex 跨过当前类型时，条件 {@code acts.length + index <= genericArgsIndex}
     * 成立，从而走到 {@code index += acts.length;}（第 77 行）累加索引后继续查找下一个类型。
     */
    @Test
    void testGetActualTypeArgumentAccumulatesIndex() {
        // index=0：首个类型即命中 else 分支，不经过第 77 行
        Assertions.assertEquals(String.class, GenericUtils.getActualTypeArgument(Sub.class, 0));

        // index=1：处理 Sup<String> 时 1+0<=1 成立 -> 执行第 77 行 index+=1，再在 If1<Integer> 命中
        Assertions.assertEquals(Integer.class, GenericUtils.getActualTypeArgument(Sub.class, 1));

        // index=2：连续两次经过第 77 行（Sup、If1），最终在 If2 命中 acts[0]=Long
        Assertions.assertEquals(Long.class, GenericUtils.getActualTypeArgument(Sub.class, 2));

        // index=3：经过第 77 行累加后，命中 If2 的 acts[1]=Short
        Assertions.assertEquals(Short.class, GenericUtils.getActualTypeArgument(Sub.class, 3));

        // index=4：全部类型都执行第 77 行累加，循环结束后兜底返回 Object.class
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> GenericUtils.getActualTypeArgument(Sub.class, 4));
    }

    static class Sup<A> {
    }

    interface If1<B> {
    }

    interface If2<C, D> {
    }

    static class Sub extends Sup<String> implements If1<Integer>, If2<Long, Short> {
    }

}
