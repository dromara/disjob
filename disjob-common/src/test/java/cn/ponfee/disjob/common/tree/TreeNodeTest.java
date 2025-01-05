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

package cn.ponfee.disjob.common.tree;

import cn.ponfee.disjob.common.util.Jsons;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * NodePath test
 *
 * @author Ponfee
 */
public class TreeNodeTest {

    @Test
    public void testSet() {
        Set<Integer> set = new HashSet<>();
        Assertions.assertThat(set.contains(null)).isFalse();
        Assertions.assertThat(set.add(null)).isTrue();
        Assertions.assertThat(set.add(null)).isFalse();
        Assertions.assertThat(set.contains(null)).isTrue();
        Assertions.assertThat(set.contains(1)).isFalse();
        Assertions.assertThat(set.add(1)).isTrue();
        Assertions.assertThat(set.add(1)).isFalse();
        Assertions.assertThat(set.contains(1)).isTrue();


        Map<Integer, Integer> map = new HashMap<>();
        Assertions.assertThat(map.containsKey(null)).isFalse();
        Assertions.assertThat(map.containsKey(1)).isFalse();
        map.put(null, null);
        Assertions.assertThat(map.containsKey(null)).isTrue();
        map.put(1, null);
        Assertions.assertThat(map.containsKey(1)).isTrue();
    }

    @Test
    public void testCheck() {
        TreeNode<Integer, Object> root1 = TreeNode.root();
        Assertions.assertThatThrownBy(() -> root1.mount(Collections.singletonList(new PlainNode<>(null, null, true, null)), false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Duplicated node id: null");


        TreeNode<Integer, Object> root2 = TreeNode.builder(1).pid(2).build();
        Assertions.assertThatThrownBy(() -> root2.mount(Collections.singletonList(new PlainNode<>(2, 1, true, null)), false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cycled node id: 1");


        TreeNode<Integer, Object> root3 = TreeNode.root();
        Assertions.assertThatThrownBy(() -> root3.mount(Arrays.asList(
                new PlainNode<>(1, null, true, null),
                new PlainNode<>(2, null, true, null),
                new PlainNode<>(2, null, true, null)
            ), false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Duplicated node id: 2");


        TreeNode<Integer, Object> root4 = TreeNode.builder(1).pid(null).build();
        Assertions.assertThatThrownBy(
                () -> root4.mount(Arrays.asList(
                    new PlainNode<>(null, 2, true, null),
                    new PlainNode<>(2, 1, true, null)
                ), true)
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Root node parent id must be null.");


        TreeNode<Integer, Object> root5 = TreeNode.builder(1).pid(0).build();
        Assertions.assertThatThrownBy(
                () -> root5.mount(Arrays.asList(
                    new PlainNode<>(0, 2, true, null),
                    new PlainNode<>(2, 1, true, null)
                ), true)
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cycled node id: 0");


        TreeNode<Integer, Object> root6 = TreeNode.builder(1).pid(0).build();
        Assertions.assertThatThrownBy(
                () -> root6.mount(Arrays.asList(
                    new PlainNode<>(2, 1, true, null),
                    new PlainNode<>(3, 4, true, null),
                    new PlainNode<>(4, 3, true, null)
                ), false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cycled node id: 3");


        TreeNode<Integer, Object> root7 = TreeNode.builder(1).pid(0).build();
        Assertions.assertThatThrownBy(
                () -> root7.mount(Arrays.asList(
                    new PlainNode<>(2, 1, true, null),
                    new PlainNode<>(3, 4, true, null),
                    new PlainNode<>(5, 6, true, null)
                ), false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unlinked node ids: [3, 5]");
    }

    @Test
    public void testMount() {
        TreeNode<Integer, Object> root1 = TreeNode.root();
        root1.mount(Arrays.asList(
            new PlainNode<>(100, null, true, null),
            new PlainNode<>(200, null, true, null),
            new PlainNode<>(300, null, true, null),
            new PlainNode<>(110, 100, true, null),
            new PlainNode<>(120, 100, true, null),
            new PlainNode<>(111, 110, true, null),
            new PlainNode<>(112, 110, true, null),
            new PlainNode<>(113, 110, true, null),
            new PlainNode<>(121, 120, true, null),
            new PlainNode<>(210, 200, true, null),
            new PlainNode<>(220, 200, true, null),
            new PlainNode<>(221, 220, true, null)
        ), false);
        System.out.println("root1 rot: " + Jsons.toJson(root1));
        System.out.println("root1 dfs: " + Jsons.toJson(root1.flatDFS()));
        System.out.println("root1 cfs: " + Jsons.toJson(root1.flatCFS()));
        System.out.println("root1 bfs: " + Jsons.toJson(root1.flatBFS()));


        TreeNode<Integer, Object> root2 = TreeNode.builder(1).pid(0).build();
        root2.mount(Arrays.asList(
            new PlainNode<>(2, 1, true, null),
            new PlainNode<>(3, 4, true, null)
        ), true);
        System.out.println("root2 rot: " + Jsons.toJson(root2));
        System.out.println("root2 dfs: " + Jsons.toJson(root2.flatDFS()));
        System.out.println("root2 cfs: " + Jsons.toJson(root2.flatCFS()));
        System.out.println("root2 bfs: " + Jsons.toJson(root2.flatBFS()));
    }

}
