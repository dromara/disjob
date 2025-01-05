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
        Assertions.assertThatThrownBy(() -> root1.mount(Collections.singletonList(new PlainNode<>(null, null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Duplicated node id: null");


        TreeNode<Integer, Object> root2 = new TreeNode<>(1, 2);
        Assertions.assertThatThrownBy(() -> root2.mount(Collections.singletonList(new PlainNode<>(2, 1))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cycled node id: 1");


        TreeNode<Integer, Object> root3 = TreeNode.root();
        Assertions.assertThatThrownBy(() -> root3.mount(Arrays.asList(
                new PlainNode<>(1, null),
                new PlainNode<>(2, null),
                new PlainNode<>(2, null)
            )))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Duplicated node id: 2");


        TreeNode<Integer, Object> root4 = new TreeNode<>(1, null);
        Assertions.assertThatThrownBy(
                () -> root4.mount(Arrays.asList(
                    new PlainNode<>(null, 2),
                    new PlainNode<>(2, 1)
                ), true, true, Comparator.comparing(TreeNode::getNid))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Root node parent id must be null.");


        TreeNode<Integer, Object> root5 = new TreeNode<>(1, 0);
        Assertions.assertThatThrownBy(
                () -> root5.mount(Arrays.asList(
                    new PlainNode<>(0, 2),
                    new PlainNode<>(2, 1)
                ), true, true, Comparator.comparing(TreeNode::getNid))
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cycled node id: 0");


        TreeNode<Integer, Object> root6 = new TreeNode<>(1, 0);
        Assertions.assertThatThrownBy(
                () -> root6.mount(Arrays.asList(
                    new PlainNode<>(2, 1),
                    new PlainNode<>(3, 4),
                    new PlainNode<>(4, 3)
                )))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cycled node id: 3");


        TreeNode<Integer, Object> root7 = new TreeNode<>(1, 0);
        Assertions.assertThatThrownBy(
                () -> root7.mount(Arrays.asList(
                    new PlainNode<>(2, 1),
                    new PlainNode<>(3, 4),
                    new PlainNode<>(5, 6)
                )))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unlinked node ids: [3, 5]");
    }

    @Test
    public void testMount() {
        TreeNode<Integer, Object> root1 = TreeNode.root();
        root1.mount(Arrays.asList(
            new PlainNode<>(100, null),
            new PlainNode<>(200, null),
            new PlainNode<>(300, null),
            new PlainNode<>(110, 100),
            new PlainNode<>(120, 100),
            new PlainNode<>(111, 110),
            new PlainNode<>(112, 110),
            new PlainNode<>(113, 110),
            new PlainNode<>(121, 120),
            new PlainNode<>(210, 200),
            new PlainNode<>(220, 200),
            new PlainNode<>(221, 220)
        ));
        Assertions.assertThat(Jsons.toJson(root1)).isEqualTo("{\"enabled\":true,\"available\":true,\"level\":1,\"path\":[null],\"degree\":3,\"leftLeafCount\":0,\"treeDepth\":4,\"treeNodeCount\":13,\"treeMaxDegree\":3,\"treeLeafCount\":7,\"childrenCount\":3,\"siblingOrdinal\":1,\"children\":[{\"nid\":100,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,100],\"degree\":2,\"leftLeafCount\":0,\"treeDepth\":3,\"treeNodeCount\":7,\"treeMaxDegree\":3,\"treeLeafCount\":4,\"childrenCount\":2,\"siblingOrdinal\":1,\"children\":[{\"nid\":110,\"pid\":100,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,100,110],\"degree\":3,\"leftLeafCount\":0,\"treeDepth\":2,\"treeNodeCount\":4,\"treeMaxDegree\":3,\"treeLeafCount\":3,\"childrenCount\":3,\"siblingOrdinal\":1,\"children\":[{\"nid\":111,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,111],\"degree\":0,\"leftLeafCount\":0,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"children\":[]},{\"nid\":112,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,112],\"degree\":0,\"leftLeafCount\":1,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":2,\"children\":[]},{\"nid\":113,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,113],\"degree\":0,\"leftLeafCount\":2,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":3,\"children\":[]}]},{\"nid\":120,\"pid\":100,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,100,120],\"degree\":1,\"leftLeafCount\":3,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":2,\"children\":[{\"nid\":121,\"pid\":120,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,120,121],\"degree\":0,\"leftLeafCount\":3,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"children\":[]}]}]},{\"nid\":200,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,200],\"degree\":2,\"leftLeafCount\":4,\"treeDepth\":3,\"treeNodeCount\":4,\"treeMaxDegree\":2,\"treeLeafCount\":2,\"childrenCount\":2,\"siblingOrdinal\":2,\"children\":[{\"nid\":210,\"pid\":200,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,200,210],\"degree\":0,\"leftLeafCount\":4,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"children\":[]},{\"nid\":220,\"pid\":200,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,200,220],\"degree\":1,\"leftLeafCount\":5,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":2,\"children\":[{\"nid\":221,\"pid\":220,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,200,220,221],\"degree\":0,\"leftLeafCount\":5,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"children\":[]}]}]},{\"nid\":300,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,300],\"degree\":0,\"leftLeafCount\":6,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":3,\"children\":[]}]}");
        Assertions.assertThat(Jsons.toJson(root1.flatDFS())).isEqualTo("[{\"enabled\":true,\"available\":true,\"level\":1,\"path\":[null],\"degree\":3,\"leftLeafCount\":0,\"treeDepth\":4,\"treeNodeCount\":13,\"treeMaxDegree\":3,\"treeLeafCount\":7,\"childrenCount\":3,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":100,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,100],\"degree\":2,\"leftLeafCount\":0,\"treeDepth\":3,\"treeNodeCount\":7,\"treeMaxDegree\":3,\"treeLeafCount\":4,\"childrenCount\":2,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":110,\"pid\":100,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,100,110],\"degree\":3,\"leftLeafCount\":0,\"treeDepth\":2,\"treeNodeCount\":4,\"treeMaxDegree\":3,\"treeLeafCount\":3,\"childrenCount\":3,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":111,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,111],\"degree\":0,\"leftLeafCount\":0,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true},{\"nid\":112,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,112],\"degree\":0,\"leftLeafCount\":1,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":2,\"leaf\":true},{\"nid\":113,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,113],\"degree\":0,\"leftLeafCount\":2,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":3,\"leaf\":true},{\"nid\":120,\"pid\":100,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,100,120],\"degree\":1,\"leftLeafCount\":3,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":2,\"leaf\":false},{\"nid\":121,\"pid\":120,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,120,121],\"degree\":0,\"leftLeafCount\":3,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true},{\"nid\":200,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,200],\"degree\":2,\"leftLeafCount\":4,\"treeDepth\":3,\"treeNodeCount\":4,\"treeMaxDegree\":2,\"treeLeafCount\":2,\"childrenCount\":2,\"siblingOrdinal\":2,\"leaf\":false},{\"nid\":210,\"pid\":200,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,200,210],\"degree\":0,\"leftLeafCount\":4,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true},{\"nid\":220,\"pid\":200,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,200,220],\"degree\":1,\"leftLeafCount\":5,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":2,\"leaf\":false},{\"nid\":221,\"pid\":220,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,200,220,221],\"degree\":0,\"leftLeafCount\":5,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true},{\"nid\":300,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,300],\"degree\":0,\"leftLeafCount\":6,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":3,\"leaf\":true}]");
        Assertions.assertThat(Jsons.toJson(root1.flatCFS())).isEqualTo("[{\"enabled\":true,\"available\":true,\"level\":1,\"path\":[null],\"degree\":3,\"leftLeafCount\":0,\"treeDepth\":4,\"treeNodeCount\":13,\"treeMaxDegree\":3,\"treeLeafCount\":7,\"childrenCount\":3,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":100,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,100],\"degree\":2,\"leftLeafCount\":0,\"treeDepth\":3,\"treeNodeCount\":7,\"treeMaxDegree\":3,\"treeLeafCount\":4,\"childrenCount\":2,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":200,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,200],\"degree\":2,\"leftLeafCount\":4,\"treeDepth\":3,\"treeNodeCount\":4,\"treeMaxDegree\":2,\"treeLeafCount\":2,\"childrenCount\":2,\"siblingOrdinal\":2,\"leaf\":false},{\"nid\":300,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,300],\"degree\":0,\"leftLeafCount\":6,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":3,\"leaf\":true},{\"nid\":110,\"pid\":100,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,100,110],\"degree\":3,\"leftLeafCount\":0,\"treeDepth\":2,\"treeNodeCount\":4,\"treeMaxDegree\":3,\"treeLeafCount\":3,\"childrenCount\":3,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":120,\"pid\":100,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,100,120],\"degree\":1,\"leftLeafCount\":3,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":2,\"leaf\":false},{\"nid\":111,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,111],\"degree\":0,\"leftLeafCount\":0,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true},{\"nid\":112,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,112],\"degree\":0,\"leftLeafCount\":1,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":2,\"leaf\":true},{\"nid\":113,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,113],\"degree\":0,\"leftLeafCount\":2,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":3,\"leaf\":true},{\"nid\":121,\"pid\":120,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,120,121],\"degree\":0,\"leftLeafCount\":3,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true},{\"nid\":210,\"pid\":200,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,200,210],\"degree\":0,\"leftLeafCount\":4,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true},{\"nid\":220,\"pid\":200,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,200,220],\"degree\":1,\"leftLeafCount\":5,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":2,\"leaf\":false},{\"nid\":221,\"pid\":220,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,200,220,221],\"degree\":0,\"leftLeafCount\":5,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true}]");
        Assertions.assertThat(Jsons.toJson(root1.flatBFS())).isEqualTo("[{\"enabled\":true,\"available\":true,\"level\":1,\"path\":[null],\"degree\":3,\"leftLeafCount\":0,\"treeDepth\":4,\"treeNodeCount\":13,\"treeMaxDegree\":3,\"treeLeafCount\":7,\"childrenCount\":3,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":100,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,100],\"degree\":2,\"leftLeafCount\":0,\"treeDepth\":3,\"treeNodeCount\":7,\"treeMaxDegree\":3,\"treeLeafCount\":4,\"childrenCount\":2,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":200,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,200],\"degree\":2,\"leftLeafCount\":4,\"treeDepth\":3,\"treeNodeCount\":4,\"treeMaxDegree\":2,\"treeLeafCount\":2,\"childrenCount\":2,\"siblingOrdinal\":2,\"leaf\":false},{\"nid\":300,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[null,300],\"degree\":0,\"leftLeafCount\":6,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":3,\"leaf\":true},{\"nid\":110,\"pid\":100,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,100,110],\"degree\":3,\"leftLeafCount\":0,\"treeDepth\":2,\"treeNodeCount\":4,\"treeMaxDegree\":3,\"treeLeafCount\":3,\"childrenCount\":3,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":120,\"pid\":100,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,100,120],\"degree\":1,\"leftLeafCount\":3,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":2,\"leaf\":false},{\"nid\":210,\"pid\":200,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,200,210],\"degree\":0,\"leftLeafCount\":4,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true},{\"nid\":220,\"pid\":200,\"enabled\":true,\"available\":true,\"level\":3,\"path\":[null,200,220],\"degree\":1,\"leftLeafCount\":5,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":2,\"leaf\":false},{\"nid\":111,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,111],\"degree\":0,\"leftLeafCount\":0,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true},{\"nid\":112,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,112],\"degree\":0,\"leftLeafCount\":1,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":2,\"leaf\":true},{\"nid\":113,\"pid\":110,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,110,113],\"degree\":0,\"leftLeafCount\":2,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":3,\"leaf\":true},{\"nid\":121,\"pid\":120,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,100,120,121],\"degree\":0,\"leftLeafCount\":3,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true},{\"nid\":221,\"pid\":220,\"enabled\":true,\"available\":true,\"level\":4,\"path\":[null,200,220,221],\"degree\":0,\"leftLeafCount\":5,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true}]");
        System.out.println("root1 rot: " + Jsons.toJson(root1));
        System.out.println("root1 dfs: " + Jsons.toJson(root1.flatDFS()));
        System.out.println("root1 cfs: " + Jsons.toJson(root1.flatCFS()));
        System.out.println("root1 bfs: " + Jsons.toJson(root1.flatBFS()));


        TreeNode<Integer, Object> root2 = new TreeNode<>(1, 0);
        root2.mount(Arrays.asList(
            new PlainNode<>(2, 1),
            new PlainNode<>(3, 4)
        ), true, true, Comparator.comparing(TreeNode::getNid));
        Assertions.assertThat(Jsons.toJson(root2)).isEqualTo("{\"nid\":1,\"pid\":0,\"enabled\":true,\"available\":true,\"level\":1,\"path\":[1],\"degree\":1,\"leftLeafCount\":0,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":1,\"children\":[{\"nid\":2,\"pid\":1,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[1,2],\"degree\":0,\"leftLeafCount\":0,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"children\":[]}]}");
        Assertions.assertThat(Jsons.toJson(root2.flatDFS())).isEqualTo("[{\"nid\":1,\"pid\":0,\"enabled\":true,\"available\":true,\"level\":1,\"path\":[1],\"degree\":1,\"leftLeafCount\":0,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":2,\"pid\":1,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[1,2],\"degree\":0,\"leftLeafCount\":0,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true}]");
        Assertions.assertThat(Jsons.toJson(root2.flatCFS())).isEqualTo("[{\"nid\":1,\"pid\":0,\"enabled\":true,\"available\":true,\"level\":1,\"path\":[1],\"degree\":1,\"leftLeafCount\":0,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":2,\"pid\":1,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[1,2],\"degree\":0,\"leftLeafCount\":0,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true}]");
        Assertions.assertThat(Jsons.toJson(root2.flatBFS())).isEqualTo("[{\"nid\":1,\"pid\":0,\"enabled\":true,\"available\":true,\"level\":1,\"path\":[1],\"degree\":1,\"leftLeafCount\":0,\"treeDepth\":2,\"treeNodeCount\":2,\"treeMaxDegree\":1,\"treeLeafCount\":1,\"childrenCount\":1,\"siblingOrdinal\":1,\"leaf\":false},{\"nid\":2,\"pid\":1,\"enabled\":true,\"available\":true,\"level\":2,\"path\":[1,2],\"degree\":0,\"leftLeafCount\":0,\"treeDepth\":1,\"treeNodeCount\":1,\"treeMaxDegree\":0,\"treeLeafCount\":1,\"childrenCount\":0,\"siblingOrdinal\":1,\"leaf\":true}]");
        System.out.println("root2 rot: " + Jsons.toJson(root2));
        System.out.println("root2 dfs: " + Jsons.toJson(root2.flatDFS()));
        System.out.println("root2 cfs: " + Jsons.toJson(root2.flatCFS()));
        System.out.println("root2 bfs: " + Jsons.toJson(root2.flatBFS()));
    }

}
