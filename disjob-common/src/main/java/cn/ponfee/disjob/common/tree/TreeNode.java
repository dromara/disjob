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

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.tree.print.MultiwayTreePrinter;
import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 * Tree node structure
 *
 * ┌───────────────────────────┐
 * │              0            │
 * │        ┌─────┴─────┐      │
 * │        1           2      │
 * │    ┌───┴───┐   ┌───┴───┐  │
 * │    3       4   5       6  │
 * │  ┌─┴─┐   ┌─┘              │
 * │  7   8   9                │
 * └───────────────────────────┘
 *
 * 上面这棵二叉树中的遍历方式：
 *   DFS前序遍历：0137849256
 *   DFS中序遍历：7381940526
 *   DFS后序遍历：7839415620
 *   BFS广度优先：0123456789
 *   CFS孩子优先：0123478956         (备注：教科书上没有CFS一说，是我为方便说明描述而取名的)
 * </pre>
 *
 * @param <T> the node id type
 * @param <A> the attachment biz object type
 * @author Ponfee
 */
@Getter
public final class TreeNode<T extends Serializable & Comparable<T>, A> extends BaseTree<T, A> {
    private static final long serialVersionUID = -9081626363752680404L;

    /**
     * 子节点列表（empty则表示此节点为叶子节点）
     */
    private final List<TreeNode<T, A>> children = new ArrayList<>();

    public TreeNode(T id, T parentId) {
        super(id, parentId);
    }

    public TreeNode(T id, T parentId, boolean enabled, boolean available, A attach) {
        super(id, parentId, enabled, available, attach);
    }

    // -----------------------------------------------------------public static methods

    /**
     * Creates single root tree node with id and parentId is null.
     *
     * @param id  the tree node id
     * @param <T> the node id type
     * @param <A> the attachment biz object type
     * @return single root tree node
     */
    public static <T extends Serializable & Comparable<T>, A> TreeNode<T, A> root(T id) {
        return new TreeNode<>(id, null);
    }

    public static <T extends Serializable & Comparable<T>, A, E extends PlainNode<T, A>> TreeNode<T, A> build(List<E> list) {
        return build(list, false, Comparator.comparing(TreeNode::getId));
    }

    /**
     * Builds root tree node from node list
     *
     * @param list                   the node list
     * @param buildPath              is whether build path
     * @param siblingNodesComparator the sibling nodes comparator
     * @param <T>                    the node id type
     * @param <A>                    the attachment biz object type
     * @param <E>                    the type of list node
     * @return root tree node
     */
    public static <T extends Serializable & Comparable<T>, A, E extends PlainNode<T, A>> TreeNode<T, A> build(
        List<E> list, boolean buildPath, Comparator<? super TreeNode<T, A>> siblingNodesComparator) {
        Assert.notEmpty(list, "Build list cannot be empty.");
        // find root node
        @SuppressWarnings("unchecked")
        PlainNode<T, A> rn = findRootNode((List<PlainNode<T, A>>) list);

        // if found the root node, should remove it
        List<E> nodes = list.stream().filter(e -> !e.equals(rn)).collect(Collectors.toList());

        // do mount
        TreeNode<T, A> root = (rn instanceof TreeNode) ?
            (TreeNode<T, A>) rn : new TreeNode<>(rn.id, rn.parentId, rn.enabled, rn.available, rn.attach);
        root.mount(nodes, false, buildPath, siblingNodesComparator);
        return root;
    }

    // -----------------------------------------------------------mount children nodes

    public <E extends PlainNode<T, A>> void mount(List<E> list) {
        mount(list, false, false, Comparator.comparing(TreeNode::getId));
    }

    /**
     * Mount these nodes to this tree node
     *
     * @param list                   节点列表
     * @param ignoreIsolated         是否忽略孤立的节点
     * @param buildPath              是否构建节点路径（比较耗资源）
     * @param siblingNodesComparator 兄弟节点间的排序比较器
     */
    public <E extends PlainNode<T, A>> void mount(List<E> list, boolean ignoreIsolated, boolean buildPath,
                                                  Comparator<? super TreeNode<T, A>> siblingNodesComparator) {
        Objects.requireNonNull(siblingNodesComparator, "Sibling nodes comparator cannot be null.");

        // 1、预处理
        @SuppressWarnings("unchecked")
        List<PlainNode<T, A>> nodes = prepare((List<PlainNode<T, A>>) list);

        // 2、检查是否存在：重复节点、循环依赖
        check(nodes);

        // 3、以此节点为根构建节点树
        mount(nodes, siblingNodesComparator);

        // 4、检查是否存在孤立的节点
        if (!ignoreIsolated && CollectionUtils.isNotEmpty(nodes)) {
            throw new IllegalStateException("Isolated node ids: " + Collects.convert(nodes, PlainNode::getId));
        }

        // 5、统计
        count(buildPath);
    }

    /**
     * Gets node by node id
     *
     * @param id the node id
     * @return node
     */
    public TreeNode<T, A> getNode(T id) {
        Deque<TreeNode<T, A>> stack = Collects.newArrayDeque(this);
        while (!stack.isEmpty()) {
            TreeNode<T, A> node = stack.pop();
            if (node.equalsId(id)) {
                return node;
            }
            node.forEachChild(stack::push);
        }
        return null;
    }

    /**
     * Remove node form tree
     *
     * @param id the node id
     * @return removed node
     */
    public TreeNode<T, A> removeNode(T id) {
        TreeNode<T, A> removed = removeNode0(id);
        if (removed != null && removed != this) {
            boolean buildPath = (super.path != null);
            // re-count root node tree
            this.count(buildPath);
            // re-count removed node tree
            removed.count(buildPath);
        }
        return removed;
    }

    // -----------------------------------------------------------DFS

    /**
     * 深度优先搜索DFS(Depth-First Search)：使用前序遍历
     * <p>Should be invoking after {@link #mount(List, boolean, boolean, Comparator)}
     *
     * @return flat nodes with DFS
     */
    public List<FlatNode<T, A>> flatDFS() {
        List<FlatNode<T, A>> list = new ArrayList<>(treeNodeCount);
        Deque<TreeNode<T, A>> stack = Collects.newArrayDeque(this);
        while (!stack.isEmpty()) {
            TreeNode<T, A> node = stack.pop();
            list.add(new FlatNode<>(node));
            // 反向遍历子节点
            Lists.reverse(node.children).forEach(stack::push);
        }
        return list;
    }

    /*
    // 递归方式DFS
    public List<FlatNode<T, A>> flatDFS() {
        List<FlatNode<T, A>> list = new ArrayList<>(treeNodeCount);
        dfs(list);
        return list;
    }
    private void dfs(List<FlatNode<T, A>> list) {
        list.add(new FlatNode<>(this));
        forEachChild(child -> child.dfs(list));
    }
    */

    // -----------------------------------------------------------CFS

    /**
     * <pre>
     * 按层级方式展开节点：兄弟节点相邻
     * 子节点优先搜索CFS(Children-First Search)
     * Should be invoking after {@link #mount(List, boolean, boolean, Comparator)}
     * Note：为了构建复杂表头，保证左侧的叶子节点必须排在右侧叶子节点前面，此处不能用广度优先搜索策略
     * </pre>
     *
     * @return flat nodes with CFS
     */
    public List<FlatNode<T, A>> flatCFS() {
        List<FlatNode<T, A>> list = Collects.newArrayList(treeNodeCount, new FlatNode<>(this));
        Deque<TreeNode<T, A>> stack = Collects.newArrayDeque(this);
        while (!stack.isEmpty()) {
            TreeNode<T, A> node = stack.pop();
            node.forEachChild(child -> list.add(new FlatNode<>(child)));
            Lists.reverse(node.children).forEach(stack::push);
        }
        return list;
    }

    /*
    // 递归方式CFS
    public List<FlatNode<T, A>> flatCFS() {
        List<FlatNode<T, A>> list = Collects.newArrayList(treeNodeCount, new FlatNode<>(this));
        cfs(list);
        return list;
    }
    private void cfs(List<FlatNode<T, A>> list) {
        forEachChild(child -> list.add(new FlatNode<>(child)));
        forEachChild(child -> child.cfs(list));
    }
    */

    // -----------------------------------------------------------BFS

    /**
     * 广度优先遍历BFS(Breath-First Search)
     *
     * @return flat nodes with BFS
     */
    public List<FlatNode<T, A>> flatBFS() {
        List<FlatNode<T, A>> list = new ArrayList<>(treeNodeCount);
        traverse(node -> list.add(new FlatNode<>(node)));
        return list;
    }

    /*
    // 递归方式BFS
    public List<FlatNode<T, A>> flatBFS() {
        List<FlatNode<T, A>> list = new ArrayList<>(treeNodeCount);
        Queue<TreeNode<T, A>> queue = Collects.newArrayDeque(this);
        bfs(queue, list);
        return list;
    }
    private void bfs(Queue<TreeNode<T, A>> queue, List<FlatNode<T, A>> list) {
        int size = queue.size();
        if (size == 0) {
            return;
        }
        while (size-- > 0) {
            TreeNode<T, A> node = Objects.requireNonNull(queue.poll());
            list.add(new FlatNode<>(node));
            node.forEachChild(queue::offer);
        }
        bfs(queue, list);
    }
    */

    // -----------------------------------------------------------other public methods

    public void forEachChild(Consumer<TreeNode<T, A>> childProcessor) {
        if (!children.isEmpty()) {
            children.forEach(childProcessor);
        }
    }

    /**
     * Traverses the tree, use BFS
     * <pre>{@code
     *   Traverser.<File>forTree(f -> f.isDirectory() ? Arrays.asList(f.listFiles()) : Collections.emptyList())
     *       .depthFirstPostOrder(new File("/xxx/path"))
     *       .forEach(f -> System.out.println(f.getName()));
     * }</pre>
     *
     * @param action the action function
     * @see com.google.common.graph.Traverser
     */
    public void traverse(Consumer<TreeNode<T, A>> action) {
        Queue<TreeNode<T, A>> queue = Collects.newArrayDeque(this);
        while (!queue.isEmpty()) {
            for (int i = queue.size(); i > 0; i--) {
                TreeNode<T, A> node = Objects.requireNonNull(queue.poll());
                action.accept(node);
                node.forEachChild(queue::offer);
            }
        }
    }

    public <E extends TreeTrait<T, A, E>> E convert(Function<TreeNode<T, A>, E> converter, boolean includeUnavailable) {
        if (super.available || includeUnavailable) {
            E root = converter.apply(this);
            convert(converter, includeUnavailable, root);
            return root;
        } else {
            return null;
        }
    }

    public void print(Appendable output, Function<TreeNode<T, A>, CharSequence> nodeLabel) throws IOException {
        new MultiwayTreePrinter<>(output, nodeLabel, TreeNode::getChildren).print(this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        try {
            print(builder, e -> String.valueOf(e.id));
            return builder.toString();
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    // -----------------------------------------------------------private methods

    private static <T extends Serializable & Comparable<T>, A> PlainNode<T, A> findRootNode(List<PlainNode<T, A>> list) {
        // 1、flatten
        List<PlainNode<T, A>> nodes = flatten(list, list.size());

        // 2、collect child node ids
        Set<T> childIds = new HashSet<>(nodes.size() * 2);
        for (PlainNode<T, A> node : nodes) {
            Assert.state(!childIds.contains(node.id), () -> "Found duplicated node id: " + node.id);
            childIds.add(node.id);
        }

        // 3、select root nodes
        Set<PlainNode<T, A>> roots = new HashSet<>();
        for (PlainNode<T, A> node : nodes) {
            // 如果`id为null`(由PlainNode构造函数中的校验可知parentId也必为null)，此时children包含null，这种特殊情况应加入roots中
            if (node.id == null || !childIds.contains(node.parentId)) {
                roots.add(node);
            }
        }

        // 4、check selected root id result
        if (roots.isEmpty()) {
            // id==parentId 且不为 null，但在`PlainNode`的构造函数中已校验`id不能等于parentId`，所以不会存在这种情况
            throw new IllegalArgumentException("Not found root node.");
        } else if (roots.size() == 1) {
            // found the root node
            return roots.iterator().next();
        } else {
            // root node node exists, must be dummy
            List<T> rootIds = roots.stream().map(e -> e.parentId).distinct().collect(Collectors.toList());
            Assert.state(rootIds.size() == 1, () -> "Found many root node id: " + rootIds);
            return new PlainNode<>(rootIds.get(0), null);
        }
    }

    private static <T extends Serializable & Comparable<T>, A> List<PlainNode<T, A>> flatten(List<PlainNode<T, A>> list, int size) {
        List<PlainNode<T, A>> nodes = new ArrayList<>(size);
        if (CollectionUtils.isNotEmpty(list)) {
            for (PlainNode<T, A> node : list) {
                if (node instanceof TreeNode) {
                    // flatten tree node
                    ((TreeNode<T, A>) node).traverse(nodes::add);
                } else {
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    private List<PlainNode<T, A>> prepare(List<PlainNode<T, A>> list) {
        List<PlainNode<T, A>> nodes = flatten(list, (list == null ? 0 : list.size()) + this.children.size());

        // flatten the root node children
        this.forEachChild(child -> child.traverse(nodes::add));
        // clear the root node children before mount
        this.children.clear();

        return nodes;
    }

    private void check(List<PlainNode<T, A>> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }

        // build graph
        Map<T, T> graph = new HashMap<>(nodes.size() * 2);
        graph.put(super.id, super.parentId);
        for (PlainNode<T, A> node : nodes) {
            // check duplicated node id
            Assert.state(!graph.containsKey(node.id), () -> "Duplicated node id: " + node.id);
            graph.put(node.id, node.parentId);
        }

        // check has cycled
        Set<T> set = new HashSet<>();
        for (T nodeId : graph.keySet()) {
            if (hasCycle(nodeId, set, graph)) {
                throw new IllegalStateException("Cycled node id: " + nodeId);
            }
        }
    }

    private static <T> boolean hasCycle(T id, Set<T> set, Map<T, T> graph) {
        if (!set.add(id)) {
            return true;
        }
        if (graph.containsKey(id)) {
            T parentId = graph.get(id);
            // 如果`id为null`(由PlainNode构造函数中的校验可知parentId也必为null)，则视为是特殊的根节点，无需校验
            if (id != null && hasCycle(parentId, set, graph)) {
                return true;
            }
        }
        set.remove(id);
        return false;
    }

    private <E extends PlainNode<T, A>> void mount(List<E> nodes, Comparator<? super TreeNode<T, A>> siblingNodesComparator) {
        // find child nodes for the current node
        for (Iterator<E> iter = nodes.iterator(); iter.hasNext(); ) {
            E node = iter.next();
            if (this.equalsId(node.parentId)) {
                // recompute the child node is available
                boolean childAvailable = node.enabled && node.available && super.available;
                this.children.add(new TreeNode<>(node.id, node.parentId, node.enabled, childAvailable, node.attach));
                // remove the found child node
                iter.remove();
            }
        }

        // recursion to mount child tree
        this.forEachChild(child -> child.mount(nodes, siblingNodesComparator));
        // sort the children list(sibling nodes sort)
        this.children.sort(siblingNodesComparator);
    }

    private void count(boolean buildPath) {
        super.level = 0;          // root node level is 0
        super.siblingOrdinal = 0; // root node sibling ordinal is 0
        super.leftLeafCount = 0;  // root node left leaf count is 0
        count(buildPath, new NodePath<>());
    }

    private void count(boolean buildPath, NodePath<T> parentPath) {
        super.path = buildPath ? new NodePath<>(parentPath, super.id) : null;
        super.nodeDegree = this.children.size();

        if (this.children.isEmpty()) {
            // 当前节点是叶子节点
            super.treeDegree    = 0;
            super.treeHeight    = 0;
            super.treeNodeCount = 1;
            super.treeLeafCount = 1;
        } else {
            // 当前节点非叶子节点
            int maxChildTreeDegree    = 0,
                maxChildTreeHeight    = 0,
                sumChildTreeNodeCount = 0,
                sumChildTreeLeafCount = 0;

            for (int i = 0, n = this.children.size(); i < n; i++) {
                TreeNode<T, A> child = this.children.get(i);
                child.level = super.level + 1;
                child.siblingOrdinal = i;

                // 1、统计当前子节点数据
                if (i == 0) {
                    // 为最左子节点：左叶子节点个数=父节点的左叶子节点个数
                    child.leftLeafCount = super.leftLeafCount;
                } else {
                    // 非最左子节点：左叶子节点个数=相邻左兄弟节点的左叶子节点个数+该兄弟节点的子节点个数
                    TreeNode<T, A> prevSibling = this.children.get(i - 1);
                    child.leftLeafCount = prevSibling.leftLeafCount + prevSibling.treeLeafCount;
                }

                // 2、递归
                child.count(buildPath, super.path);

                // 3、统计所有子节点数据
                maxChildTreeDegree     = Math.max(maxChildTreeDegree, child.treeDegree);
                maxChildTreeHeight     = Math.max(maxChildTreeHeight, child.treeHeight);
                sumChildTreeNodeCount += child.treeNodeCount;
                sumChildTreeLeafCount += child.treeLeafCount;
            }

            // 统计当前节点数据
            super.treeDegree    = Math.max(maxChildTreeDegree, super.nodeDegree); // 树中的最大度数
            super.treeHeight    = maxChildTreeHeight + 1;                         // 最大子节点高度+1
            super.treeNodeCount = sumChildTreeNodeCount + 1;                      // 所有子节点个数+节点本身
            super.treeLeafCount = sumChildTreeLeafCount;                          // 所有子节点的叶子节点之和
        }
    }

    private <E extends TreeTrait<T, A, E>> void convert(Function<TreeNode<T, A>, E> converter,
                                                        boolean includeUnavailable, E parent) {
        List<E> list = new ArrayList<>(this.children.size());
        for (TreeNode<T, A> child : this.children) {
            // filter unavailable
            if (child.available || includeUnavailable) {
                E node = converter.apply(child);
                child.convert(converter, includeUnavailable, node);
                list.add(node);
            }
        }
        parent.setChildren(list);
    }

    private TreeNode<T, A> removeNode0(T id) {
        if (this.equalsId(id)) {
            // is the root node id
            return this;
        }
        Deque<TreeNode<T, A>> stack = Collects.newArrayDeque(this);
        while (!stack.isEmpty()) {
            TreeNode<T, A> node = stack.pop();
            for (Iterator<TreeNode<T, A>> iter = node.children.iterator(); iter.hasNext(); ) {
                TreeNode<T, A> child = iter.next();
                if (child.equalsId(id)) {
                    iter.remove();
                    return child;
                }
                stack.push(child);
            }
        }
        // node id not exists
        return null;
    }

}
