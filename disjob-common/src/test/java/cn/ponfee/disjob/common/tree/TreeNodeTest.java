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
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

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
        TreeNode<Integer, Object> root1 = TreeNode.root(null);
        Assertions.assertThatThrownBy(() -> root1.mount(Collections.singletonList(new PlainNode<>(null, null)), false, true, Comparator.comparing(TreeNode::getId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Duplicated node id: null");


        TreeNode<Integer, Object> root2 = new TreeNode<>(1, 2);
        Assertions.assertThatThrownBy(() -> root2.mount(Collections.singletonList(new PlainNode<>(2, 1)), false, true, Comparator.comparing(TreeNode::getId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cycled node id: 1");


        TreeNode<Integer, Object> root3 = TreeNode.root(null);
        Assertions.assertThatThrownBy(() -> root3.mount(Arrays.asList(new PlainNode<>(1, null), new PlainNode<>(2, null), new PlainNode<>(2, null)), false, true, Comparator.comparing(TreeNode::getId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Duplicated node id: 2");


        TreeNode<Integer, Object> root4 = new TreeNode<>(1, null);
        Assertions.assertThatThrownBy(() -> root4.mount(Arrays.asList(new PlainNode<>(null, 2), new PlainNode<>(2, 1)), false, true, Comparator.comparing(TreeNode::getId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Parent id must be null when node id is null.");


        TreeNode<Integer, Object> root5 = new TreeNode<>(1, 0);
        Assertions.assertThatThrownBy(() -> root5.mount(Arrays.asList(new PlainNode<>(0, 2), new PlainNode<>(2, 1)), false, true, Comparator.comparing(TreeNode::getId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cycled node id: 0");


        TreeNode<Integer, Object> root6 = new TreeNode<>(1, 0);
        Assertions.assertThatThrownBy(() -> root6.mount(Arrays.asList(new PlainNode<>(2, 1), new PlainNode<>(3, 4), new PlainNode<>(4, 3)), false, true, Comparator.comparing(TreeNode::getId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cycled node id: 3");


        TreeNode<Integer, Object> root7 = new TreeNode<>(1, 0);
        Assertions.assertThatThrownBy(() -> root7.mount(Arrays.asList(new PlainNode<>(2, 1), new PlainNode<>(3, 4), new PlainNode<>(5, 6)), false, true, Comparator.comparing(TreeNode::getId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Isolated node ids: [3, 5]");
    }

    @Test
    public void testMount1() throws JSONException {
        TreeNode<Integer, Object> root = TreeNode.root(null);
        root.mount(Arrays.asList(
            new PlainNode<>(100, null),
            new PlainNode<>(200, null),
            new PlainNode<>(300, null),
            new PlainNode<>(110, 100),
            new PlainNode<>(120, 100),
            new PlainNode<>(111, 110),
            new PlainNode<>(112, 110),
            new PlainNode<>(113, 110),
            new PlainNode<>(114, 110),
            new PlainNode<>(121, 120),
            new PlainNode<>(210, 200),
            new PlainNode<>(220, 200),
            new PlainNode<>(221, 220)
        ), false, true, Comparator.comparing(TreeNode::getId));
        System.out.println("print tree: \n" + root);
        JSONAssert.assertEquals("{\"enabled\":true,\"available\":true,\"path\":[null],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":3,\"treeDegree\":4,\"treeHeight\":3,\"treeNodeCount\":14,\"treeLeafCount\":8,\"children\":[{\"id\":100,\"enabled\":true,\"available\":true,\"path\":[null,100],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":4,\"treeHeight\":2,\"treeNodeCount\":8,\"treeLeafCount\":5,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,110],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":4,\"treeDegree\":4,\"treeHeight\":1,\"treeNodeCount\":5,\"treeLeafCount\":4,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,111],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,112],\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":113,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,113],\"level\":3,\"siblingOrdinal\":2,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":114,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,114],\"level\":3,\"siblingOrdinal\":3,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,120],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":4,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"path\":[null,100,120,121],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":200,\"enabled\":true,\"available\":true,\"path\":[null,200],\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":5,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,210],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,220],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":6,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"path\":[null,200,220,221],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":300,\"enabled\":true,\"available\":true,\"path\":[null,300],\"level\":1,\"siblingOrdinal\":2,\"leftLeafCount\":7,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(root), JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals("[{\"enabled\":true,\"available\":true,\"path\":[null],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":3,\"treeDegree\":4,\"treeHeight\":3,\"treeNodeCount\":14,\"treeLeafCount\":8,\"leaf\":false},{\"id\":100,\"enabled\":true,\"available\":true,\"path\":[null,100],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":4,\"treeHeight\":2,\"treeNodeCount\":8,\"treeLeafCount\":5,\"leaf\":false},{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,110],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":4,\"treeDegree\":4,\"treeHeight\":1,\"treeNodeCount\":5,\"treeLeafCount\":4,\"leaf\":false},{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,111],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,112],\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":113,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,113],\"level\":3,\"siblingOrdinal\":2,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":114,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,114],\"level\":3,\"siblingOrdinal\":3,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,120],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":4,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"leaf\":false},{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"path\":[null,100,120,121],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":200,\"enabled\":true,\"available\":true,\"path\":[null,200],\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":5,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"leaf\":false},{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,210],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,220],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":6,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"leaf\":false},{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"path\":[null,200,220,221],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":300,\"enabled\":true,\"available\":true,\"path\":[null,300],\"level\":1,\"siblingOrdinal\":2,\"leftLeafCount\":7,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true}]", Jsons.toJson(root.flatDFS()), JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals("[{\"enabled\":true,\"available\":true,\"path\":[null],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":3,\"treeDegree\":4,\"treeHeight\":3,\"treeNodeCount\":14,\"treeLeafCount\":8,\"leaf\":false},{\"id\":100,\"enabled\":true,\"available\":true,\"path\":[null,100],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":4,\"treeHeight\":2,\"treeNodeCount\":8,\"treeLeafCount\":5,\"leaf\":false},{\"id\":200,\"enabled\":true,\"available\":true,\"path\":[null,200],\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":5,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"leaf\":false},{\"id\":300,\"enabled\":true,\"available\":true,\"path\":[null,300],\"level\":1,\"siblingOrdinal\":2,\"leftLeafCount\":7,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,110],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":4,\"treeDegree\":4,\"treeHeight\":1,\"treeNodeCount\":5,\"treeLeafCount\":4,\"leaf\":false},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,120],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":4,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"leaf\":false},{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,111],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,112],\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":113,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,113],\"level\":3,\"siblingOrdinal\":2,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":114,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,114],\"level\":3,\"siblingOrdinal\":3,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"path\":[null,100,120,121],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,210],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,220],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":6,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"leaf\":false},{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"path\":[null,200,220,221],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true}]", Jsons.toJson(root.flatCFS()), JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals("[{\"enabled\":true,\"available\":true,\"path\":[null],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":3,\"treeDegree\":4,\"treeHeight\":3,\"treeNodeCount\":14,\"treeLeafCount\":8,\"leaf\":false},{\"id\":100,\"enabled\":true,\"available\":true,\"path\":[null,100],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":4,\"treeHeight\":2,\"treeNodeCount\":8,\"treeLeafCount\":5,\"leaf\":false},{\"id\":200,\"enabled\":true,\"available\":true,\"path\":[null,200],\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":5,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"leaf\":false},{\"id\":300,\"enabled\":true,\"available\":true,\"path\":[null,300],\"level\":1,\"siblingOrdinal\":2,\"leftLeafCount\":7,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,110],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":4,\"treeDegree\":4,\"treeHeight\":1,\"treeNodeCount\":5,\"treeLeafCount\":4,\"leaf\":false},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,120],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":4,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"leaf\":false},{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,210],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,220],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":6,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"leaf\":false},{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,111],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,112],\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":113,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,113],\"level\":3,\"siblingOrdinal\":2,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":114,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,114],\"level\":3,\"siblingOrdinal\":3,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"path\":[null,100,120,121],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true},{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"path\":[null,200,220,221],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true}]", Jsons.toJson(root.flatBFS()), JSONCompareMode.NON_EXTENSIBLE);

        TreeNode<Integer, Object> cloneRoot1 = (TreeNode<Integer, Object>) root.clone();
        Assertions.assertThat(root).isNotSameAs(cloneRoot1);
        Assertions.assertThat(root).isEqualTo(cloneRoot1);
        Assertions.assertThat(Jsons.toJson(root)).isEqualTo(Jsons.toJson(cloneRoot1));
        System.out.println("rot: " + Jsons.toJson(root));
        System.out.println("dfs: " + Jsons.toJson(root.flatDFS()));
        System.out.println("cfs: " + Jsons.toJson(root.flatCFS()));
        System.out.println("bfs: " + Jsons.toJson(root.flatBFS()));

        root.mount(Arrays.asList(new PlainNode<>(310, 300), new PlainNode<>(320, 300), new PlainNode<>(311, 310)), false, true, Comparator.comparing(TreeNode::getId));
        JSONAssert.assertEquals("{\"enabled\":true,\"available\":true,\"path\":[null],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":3,\"treeDegree\":4,\"treeHeight\":3,\"treeNodeCount\":17,\"treeLeafCount\":9,\"children\":[{\"id\":100,\"enabled\":true,\"available\":true,\"path\":[null,100],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":4,\"treeHeight\":2,\"treeNodeCount\":8,\"treeLeafCount\":5,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,110],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":4,\"treeDegree\":4,\"treeHeight\":1,\"treeNodeCount\":5,\"treeLeafCount\":4,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,111],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,112],\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":113,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,113],\"level\":3,\"siblingOrdinal\":2,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":114,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,114],\"level\":3,\"siblingOrdinal\":3,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,120],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":4,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"path\":[null,100,120,121],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":200,\"enabled\":true,\"available\":true,\"path\":[null,200],\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":5,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,210],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,220],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":6,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"path\":[null,200,220,221],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":300,\"enabled\":true,\"available\":true,\"path\":[null,300],\"level\":1,\"siblingOrdinal\":2,\"leftLeafCount\":7,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":310,\"parentId\":300,\"enabled\":true,\"available\":true,\"path\":[null,300,310],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":7,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":311,\"parentId\":310,\"enabled\":true,\"available\":true,\"path\":[null,300,310,311],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":7,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":320,\"parentId\":300,\"enabled\":true,\"available\":true,\"path\":[null,300,320],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":8,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]}", Jsons.toJson(root), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println("print tree: \n" + root);

        TreeNode<Integer, Object> node220 = root.getNode(220);
        System.out.println("print node220: \n" + node220);
        JSONAssert.assertEquals("{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,220],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":6,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"path\":[null,200,220,221],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(node220), JSONCompareMode.NON_EXTENSIBLE);

        TreeNode<Integer, Object> clone220 = (TreeNode<Integer, Object>) node220.clone();
        System.out.println("print clone220: \n" + clone220);

        TreeNode<Integer, Object> removed221 = clone220.removeNode(221);
        System.out.println("print remove221: \n" + removed221);

        JSONAssert.assertEquals("{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[220],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}", Jsons.toJson(clone220), JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals("{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"path\":[221],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}", Jsons.toJson(removed221), JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals("{\"enabled\":true,\"available\":true,\"path\":[null],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":3,\"treeDegree\":4,\"treeHeight\":3,\"treeNodeCount\":17,\"treeLeafCount\":9,\"children\":[{\"id\":100,\"enabled\":true,\"available\":true,\"path\":[null,100],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":4,\"treeHeight\":2,\"treeNodeCount\":8,\"treeLeafCount\":5,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,110],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":4,\"treeDegree\":4,\"treeHeight\":1,\"treeNodeCount\":5,\"treeLeafCount\":4,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,111],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,112],\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":113,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,113],\"level\":3,\"siblingOrdinal\":2,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":114,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,114],\"level\":3,\"siblingOrdinal\":3,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,120],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":4,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"path\":[null,100,120,121],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":200,\"enabled\":true,\"available\":true,\"path\":[null,200],\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":5,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,210],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[null,200,220],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":6,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"path\":[null,200,220,221],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":300,\"enabled\":true,\"available\":true,\"path\":[null,300],\"level\":1,\"siblingOrdinal\":2,\"leftLeafCount\":7,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":310,\"parentId\":300,\"enabled\":true,\"available\":true,\"path\":[null,300,310],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":7,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":311,\"parentId\":310,\"enabled\":true,\"available\":true,\"path\":[null,300,310,311],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":7,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":320,\"parentId\":300,\"enabled\":true,\"available\":true,\"path\":[null,300,320],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":8,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]}", Jsons.toJson(root), JSONCompareMode.NON_EXTENSIBLE);

        TreeNode<Integer, Object> removed200 = root.removeNode(200);
        System.out.println("print root: \n" + root);
        System.out.println("print remove200: \n" + removed200);
        JSONAssert.assertEquals("{\"id\":200,\"enabled\":true,\"available\":true,\"path\":[200],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[200,210],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[200,220],\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"path\":[200,220,221],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]}", Jsons.toJson(removed200), JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals("{\"enabled\":true,\"available\":true,\"path\":[null],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":4,\"treeHeight\":3,\"treeNodeCount\":13,\"treeLeafCount\":7,\"children\":[{\"id\":100,\"enabled\":true,\"available\":true,\"path\":[null,100],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":4,\"treeHeight\":2,\"treeNodeCount\":8,\"treeLeafCount\":5,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,110],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":4,\"treeDegree\":4,\"treeHeight\":1,\"treeNodeCount\":5,\"treeLeafCount\":4,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,111],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,112],\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":113,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,113],\"level\":3,\"siblingOrdinal\":2,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":114,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[null,100,110,114],\"level\":3,\"siblingOrdinal\":3,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[null,100,120],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":4,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"path\":[null,100,120,121],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":300,\"enabled\":true,\"available\":true,\"path\":[null,300],\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":5,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":310,\"parentId\":300,\"enabled\":true,\"available\":true,\"path\":[null,300,310],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":311,\"parentId\":310,\"enabled\":true,\"available\":true,\"path\":[null,300,310,311],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":320,\"parentId\":300,\"enabled\":true,\"available\":true,\"path\":[null,300,320],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]}", Jsons.toJson(root), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testMount2() throws JSONException {
        TreeNode<Integer, Object> root = new TreeNode<>(0, -1);
        root.mount(Arrays.asList(
            new PlainNode<>(100, 0),
            new PlainNode<>(200, 0),
            new PlainNode<>(300, 0),
            new PlainNode<>(110, 100),
            new PlainNode<>(120, 100),
            new PlainNode<>(111, 110),
            new PlainNode<>(112, 110),
            new PlainNode<>(113, 110),
            new PlainNode<>(114, 110),
            new PlainNode<>(121, 120),
            new PlainNode<>(210, 200),
            new PlainNode<>(220, 200),
            new PlainNode<>(221, 220)
        ), false, true, Comparator.comparing(TreeNode::getId));
        root.mount(Arrays.asList(new PlainNode<>(310, 300), new PlainNode<>(320, 300), new PlainNode<>(311, 310)), false, true, Comparator.comparing(TreeNode::getId));
        System.out.println("print tree: \n" + root);

        TreeTraitMap<Integer, Object> treeTraitMap = root.convert(e -> {
            TreeTraitMap<Integer, Object> treeTrait = new TreeTraitMap<>();
            treeTrait.put("parentId", e.getParentId());
            treeTrait.put("id", e.getId());
            return treeTrait;
        }, true);
        JSONAssert.assertEquals("{\"parentId\":-1,\"id\":0,\"children\":[{\"parentId\":0,\"id\":100,\"children\":[{\"parentId\":100,\"id\":110,\"children\":[{\"parentId\":110,\"id\":111,\"children\":[]},{\"parentId\":110,\"id\":112,\"children\":[]},{\"parentId\":110,\"id\":113,\"children\":[]},{\"parentId\":110,\"id\":114,\"children\":[]}]},{\"parentId\":100,\"id\":120,\"children\":[{\"parentId\":120,\"id\":121,\"children\":[]}]}]},{\"parentId\":0,\"id\":200,\"children\":[{\"parentId\":200,\"id\":210,\"children\":[]},{\"parentId\":200,\"id\":220,\"children\":[{\"parentId\":220,\"id\":221,\"children\":[]}]}]},{\"parentId\":0,\"id\":300,\"children\":[{\"parentId\":300,\"id\":310,\"children\":[{\"parentId\":310,\"id\":311,\"children\":[]}]},{\"parentId\":300,\"id\":320,\"children\":[]}]}]}", Jsons.toJson(treeTraitMap), JSONCompareMode.NON_EXTENSIBLE);

        TreeNode<Integer, Object> removed200 = root.removeNode(200);
        System.out.println("print root: \n" + root);
        System.out.println("print remove200: \n" + removed200);
        JSONAssert.assertEquals("{\"id\":200,\"parentId\":0,\"enabled\":true,\"available\":true,\"path\":[200],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[200,210],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"path\":[200,220],\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"path\":[200,220,221],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]}", Jsons.toJson(removed200), JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals("{\"id\":0,\"parentId\":-1,\"enabled\":true,\"available\":true,\"path\":[0],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":4,\"treeHeight\":3,\"treeNodeCount\":13,\"treeLeafCount\":7,\"children\":[{\"id\":100,\"parentId\":0,\"enabled\":true,\"available\":true,\"path\":[0,100],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":4,\"treeHeight\":2,\"treeNodeCount\":8,\"treeLeafCount\":5,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[0,100,110],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":4,\"treeDegree\":4,\"treeHeight\":1,\"treeNodeCount\":5,\"treeLeafCount\":4,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[0,100,110,111],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[0,100,110,112],\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":113,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[0,100,110,113],\"level\":3,\"siblingOrdinal\":2,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":114,\"parentId\":110,\"enabled\":true,\"available\":true,\"path\":[0,100,110,114],\"level\":3,\"siblingOrdinal\":3,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"path\":[0,100,120],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":4,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"path\":[0,100,120,121],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":300,\"parentId\":0,\"enabled\":true,\"available\":true,\"path\":[0,300],\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":5,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":310,\"parentId\":300,\"enabled\":true,\"available\":true,\"path\":[0,300,310],\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":311,\"parentId\":310,\"enabled\":true,\"available\":true,\"path\":[0,300,310,311],\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":320,\"parentId\":300,\"enabled\":true,\"available\":true,\"path\":[0,300,320],\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]}", Jsons.toJson(root), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testMount3() throws JSONException {
        TreeNode<Integer, Object> root = new TreeNode<>(1, 0);
        root.mount(Arrays.asList(new PlainNode<>(2, 1), new PlainNode<>(3, 4)), true, true, Comparator.comparing(TreeNode::getId));
        JSONAssert.assertEquals("{\"id\":1,\"parentId\":0,\"enabled\":true,\"available\":true,\"path\":[1],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":2,\"parentId\":1,\"enabled\":true,\"available\":true,\"path\":[1,2],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(root), JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals("[{\"id\":1,\"parentId\":0,\"enabled\":true,\"available\":true,\"path\":[1],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"leaf\":false},{\"id\":2,\"parentId\":1,\"enabled\":true,\"available\":true,\"path\":[1,2],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true}]", Jsons.toJson(root.flatDFS()), JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals("[{\"id\":1,\"parentId\":0,\"enabled\":true,\"available\":true,\"path\":[1],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"leaf\":false},{\"id\":2,\"parentId\":1,\"enabled\":true,\"available\":true,\"path\":[1,2],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true}]", Jsons.toJson(root.flatCFS()), JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals("[{\"id\":1,\"parentId\":0,\"enabled\":true,\"available\":true,\"path\":[1],\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"leaf\":false},{\"id\":2,\"parentId\":1,\"enabled\":true,\"available\":true,\"path\":[1,2],\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"leaf\":true}]", Jsons.toJson(root.flatBFS()), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println("rot: " + Jsons.toJson(root));
        System.out.println("dfs: " + Jsons.toJson(root.flatDFS()));
        System.out.println("cfs: " + Jsons.toJson(root.flatCFS()));
        System.out.println("bfs: " + Jsons.toJson(root.flatBFS()));
    }

    @Test
    public void testMount4() {
        List<PlainNode<Integer, Object>> plainNodes1 = Arrays.asList(
            new PlainNode<>(100, null),
            new PlainNode<>(110, 100),
            new PlainNode<>(120, 100),
            new PlainNode<>(111, 110)
        );
        TreeNode<Integer, Object> root1 = TreeNode.build(plainNodes1);
        System.out.println(root1);


        List<PlainNode<Integer, Object>> plainNodes2 = Arrays.asList(
            new PlainNode<>(200, null),
            new PlainNode<>(210, 200),
            new PlainNode<>(220, 200),
            new PlainNode<>(211, 210),
            new PlainNode<>(212, 210)
        );
        TreeNode<Integer, Object> root2 = TreeNode.build(plainNodes2);
        System.out.println(root2);


        Assertions.assertThatThrownBy(() -> root1.mount(Arrays.asList(new PlainNode<>(112, 110), new PlainNode<>(121, 120), new PlainNode<>(122, 120), root2)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Isolated node ids: [200, 210, 220, 211, 212]");
    }

    @Test
    public void testMount5() throws JSONException {
        List<PlainNode<Integer, Object>> plainNodes1 = Arrays.asList(
            new PlainNode<>(100, null),
            new PlainNode<>(300, null),
            new PlainNode<>(110, 100),
            new PlainNode<>(120, 100),
            new PlainNode<>(111, 110)
        );
        TreeNode<Integer, Object> root1 = TreeNode.build(plainNodes1);
        System.out.println("root1:\n" + root1);
        JSONAssert.assertEquals("{\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":3,\"treeNodeCount\":6,\"treeLeafCount\":3,\"children\":[{\"id\":100,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":300,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(root1), JSONCompareMode.NON_EXTENSIBLE);


        List<PlainNode<Integer, Object>> plainNodes2 = Arrays.asList(
            new PlainNode<>(200, null),
            new PlainNode<>(210, 200),
            new PlainNode<>(220, 200),
            new PlainNode<>(211, 210),
            new PlainNode<>(212, 210)
        );
        TreeNode<Integer, Object> root2 = TreeNode.build(plainNodes2);
        System.out.println("root2:\n" + root2);
        JSONAssert.assertEquals("{\"id\":200,\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":5,\"treeLeafCount\":3,\"children\":[{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":1,\"treeNodeCount\":3,\"treeLeafCount\":2,\"children\":[{\"id\":211,\"parentId\":210,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":212,\"parentId\":210,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(root2), JSONCompareMode.NON_EXTENSIBLE);


        root1.mount(Arrays.asList(new PlainNode<>(112, 110), new PlainNode<>(121, 120), new PlainNode<>(122, 120), root2));
        System.out.println("root1:\n" + root1);
        JSONAssert.assertEquals("{\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":3,\"treeDegree\":3,\"treeHeight\":3,\"treeNodeCount\":14,\"treeLeafCount\":8,\"children\":[{\"id\":100,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":7,\"treeLeafCount\":4,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":1,\"treeNodeCount\":3,\"treeLeafCount\":2,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":2,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":1,\"treeNodeCount\":3,\"treeLeafCount\":2,\"children\":[{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":122,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":200,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":4,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":5,\"treeLeafCount\":3,\"children\":[{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":1,\"treeNodeCount\":3,\"treeLeafCount\":2,\"children\":[{\"id\":211,\"parentId\":210,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":212,\"parentId\":210,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":5,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":300,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":2,\"leftLeafCount\":7,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(root1), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testBuild1() throws JSONException {
        List<PlainNode<Integer, Object>> plainNodes = Arrays.asList(
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
        );
        TreeNode<Integer, Object> root = TreeNode.build(plainNodes);
        JSONAssert.assertEquals("{\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":3,\"treeDegree\":3,\"treeHeight\":3,\"treeNodeCount\":13,\"treeLeafCount\":7,\"children\":[{\"id\":100,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":3,\"treeHeight\":2,\"treeNodeCount\":7,\"treeLeafCount\":4,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":3,\"treeDegree\":3,\"treeHeight\":1,\"treeNodeCount\":4,\"treeLeafCount\":3,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":113,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":2,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":3,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":200,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":4,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":210,\"parentId\":200,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":220,\"parentId\":200,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":5,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":221,\"parentId\":220,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":5,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":300,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":2,\"leftLeafCount\":6,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(root), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println(root);
    }

    @Test
    public void testBuild2() throws JSONException {
        List<PlainNode<Integer, Object>> plainNodes = Arrays.asList(
            new PlainNode<>(100, null),
            new PlainNode<>(110, 100),
            new PlainNode<>(120, 100),
            new PlainNode<>(111, 110)
        );
        TreeNode<Integer, Object> root1 = TreeNode.build(plainNodes);
        JSONAssert.assertEquals("{\"id\":100,\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(root1), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println("root1:\n" + root1);


        List<PlainNode<Integer, Object>> plainNodes2 = Arrays.asList(
            new PlainNode<>(121, 120),
            new PlainNode<>(131, 121),
            new PlainNode<>(132, 121)
        );
        TreeNode<Integer, Object> root2 = TreeNode.build(plainNodes2);
        JSONAssert.assertEquals("{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":1,\"treeNodeCount\":3,\"treeLeafCount\":2,\"children\":[{\"id\":131,\"parentId\":121,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":132,\"parentId\":121,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(root2), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println("root2:\n" + root2);


        List<PlainNode<Integer, Object>> plainNodes3 = Arrays.asList(
            root1,
            root2,
            new PlainNode<>(112, 110)
        );
        TreeNode<Integer, Object> root3 = TreeNode.build(plainNodes3);
        JSONAssert.assertEquals("{\"id\":100,\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":3,\"treeNodeCount\":8,\"treeLeafCount\":4,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":1,\"treeNodeCount\":3,\"treeLeafCount\":2,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":2,\"nodeDegree\":1,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":2,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":1,\"treeNodeCount\":3,\"treeLeafCount\":2,\"children\":[{\"id\":131,\"parentId\":121,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":132,\"parentId\":121,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]}]}", Jsons.toJson(root3), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println("root3:\n" + root3);
    }

    @Test
    public void testBuild3() throws JSONException {
        List<PlainNode<Integer, Object>> plainNodes = Arrays.asList(
            new PlainNode<>(100, 0),
            new PlainNode<>(110, 100),
            new PlainNode<>(120, 100),
            new PlainNode<>(111, 110),
            new PlainNode<>(200, 0)
        );
        TreeNode<Integer, Object> root = TreeNode.build(plainNodes);
        JSONAssert.assertEquals("{\"available\":true,\"id\":0,\"enabled\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":3,\"treeNodeCount\":6,\"treeLeafCount\":3,\"children\":[{\"id\":100,\"parentId\":0,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":200,\"parentId\":0,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(root), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println(root);
    }

    @Test
    public void testBuild4() throws JSONException {
        List<PlainNode<Integer, Object>> plainNodes = Arrays.asList(
            new PlainNode<>(null, null),
            new PlainNode<>(100, null),
            new PlainNode<>(110, 100),
            new PlainNode<>(120, 100),
            new PlainNode<>(111, 110)
        );
        TreeNode<Integer, Object> root = TreeNode.build(plainNodes);
        JSONAssert.assertEquals("{\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":2,\"treeHeight\":3,\"treeNodeCount\":5,\"treeLeafCount\":2,\"children\":[{\"id\":100,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]},{\"id\":120,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]}", Jsons.toJson(root), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println(root);
    }

    @Test
    public void testBuild5() {
        List<PlainNode<Integer, Object>> plainNodes = Arrays.asList(
            new PlainNode<>(100, null),
            new PlainNode<>(110, 100),
            new PlainNode<>(120, 100),
            new PlainNode<>(111, 110),
            new PlainNode<>(210, 200)
        );
        Assertions.assertThatThrownBy(() -> TreeNode.build(plainNodes))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Found many root node id: [200, null]");
    }

    @Test
    public void testBuild6() throws JSONException {
        List<PlainNode<Integer, Object>> plainNodes = Arrays.asList(
            new PlainNode<>(100, null),
            new PlainNode<>(110, 100),
            new PlainNode<>(111, 110)
        );
        TreeNode<Integer, Object> root1 = TreeNode.build(plainNodes);
        JSONAssert.assertEquals("{\"id\":100,\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":2,\"treeNodeCount\":3,\"treeLeafCount\":1,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":1,\"treeHeight\":1,\"treeNodeCount\":2,\"treeLeafCount\":1,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]}", Jsons.toJson(root1), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println("root1:\n" + root1);


        List<PlainNode<Integer, Object>> plainNodes2 = Arrays.asList(
            new PlainNode<>(121, 120),
            new PlainNode<>(122, 120),
            new PlainNode<>(123, 120)
        );
        TreeNode<Integer, Object> root2 = TreeNode.build(plainNodes2);
        JSONAssert.assertEquals("{\"id\":120,\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":3,\"treeDegree\":3,\"treeHeight\":1,\"treeNodeCount\":4,\"treeLeafCount\":3,\"children\":[{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":122,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":123,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":2,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}", Jsons.toJson(root2), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println("root2:\n" + root2);


        List<PlainNode<Integer, Object>> plainNodes3 = Arrays.asList(
            root1,
            root2,
            new PlainNode<>(112, 110)
        );
        TreeNode<Integer, Object> root3 = TreeNode.build(plainNodes3);
        JSONAssert.assertEquals("{\"enabled\":true,\"available\":true,\"level\":0,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":3,\"treeHeight\":3,\"treeNodeCount\":9,\"treeLeafCount\":5,\"children\":[{\"id\":100,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":1,\"treeDegree\":2,\"treeHeight\":2,\"treeNodeCount\":4,\"treeLeafCount\":2,\"children\":[{\"id\":110,\"parentId\":100,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":2,\"treeDegree\":2,\"treeHeight\":1,\"treeNodeCount\":3,\"treeLeafCount\":2,\"children\":[{\"id\":111,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":0,\"leftLeafCount\":0,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":112,\"parentId\":110,\"enabled\":true,\"available\":true,\"level\":3,\"siblingOrdinal\":1,\"leftLeafCount\":1,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]},{\"id\":120,\"enabled\":true,\"available\":true,\"level\":1,\"siblingOrdinal\":1,\"leftLeafCount\":2,\"nodeDegree\":3,\"treeDegree\":3,\"treeHeight\":1,\"treeNodeCount\":4,\"treeLeafCount\":3,\"children\":[{\"id\":121,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":0,\"leftLeafCount\":2,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":122,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":1,\"leftLeafCount\":3,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]},{\"id\":123,\"parentId\":120,\"enabled\":true,\"available\":true,\"level\":2,\"siblingOrdinal\":2,\"leftLeafCount\":4,\"nodeDegree\":0,\"treeDegree\":0,\"treeHeight\":0,\"treeNodeCount\":1,\"treeLeafCount\":1,\"children\":[]}]}]}", Jsons.toJson(root3), JSONCompareMode.NON_EXTENSIBLE);
        System.out.println("root3:\n" + root3);
    }

    @Test
    public void testTraverse() {
        TreeNode<Integer, Object> root1 = new TreeNode<>(0, null);
        root1.mount(Arrays.asList(
            new PlainNode<>(1, 0),
            new PlainNode<>(2, 0),
            new PlainNode<>(3, 1),
            new PlainNode<>(4, 1),
            new PlainNode<>(5, 2),
            new PlainNode<>(6, 2),
            new PlainNode<>(7, 3),
            new PlainNode<>(8, 3),
            new PlainNode<>(9, 4)
        ));
        System.out.println(root1);
        StringBuilder builder = new StringBuilder();
        root1.traverse(node -> builder.append(node.id));
        Assertions.assertThat(builder.toString()).isEqualTo("0123456789");
    }

}
