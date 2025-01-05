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

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

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
public final class TreeNode<T extends Serializable & Comparable<T>, A> extends BaseNode<T, A> {
    private static final long serialVersionUID = -9081626363752680404L;

    /**
     * 子节点列表（空列表则表示为叶子节点）
     */
    private final LinkedList<TreeNode<T, A>> children = new LinkedList<>();

    public TreeNode(T nid, T pid) {
        super(nid, pid);
    }

    /**
     * Constructs a tree node
     *
     * @param nid       the node id
     * @param pid       the parent node id(withhold this pid field value,when use if the other root node mount this node as child)
     * @param enabled   the node is enabled
     * @param available the current node is available(parent.available & this.enabled)
     * @param attach    the attachment for biz object
     */
    public TreeNode(T nid, T pid, boolean enabled, boolean available, A attach) {
        super(nid, pid, enabled, available, attach);
    }

    public static <T extends Serializable & Comparable<T>, A> TreeNode<T, A> root() {
        return new TreeNode<>(null, null);
    }

    // ------------------------------------------------------mount children nodes

    public <E extends BaseNode<T, A>> void mount(List<E> list) {
        mount(list, false, true, Comparator.comparing(TreeNode::getNid));
    }

    /**
     * Mount as a tree
     *
     * @param list           子节点列表
     * @param ignoreUnlinked 是否忽略未连接的节点
     */
    public <E extends BaseNode<T, A>> void mount(List<E> list, boolean ignoreUnlinked, boolean buildPath,
                                                 Comparator<? super TreeNode<T, A>> siblingNodesComparator) {
        Objects.requireNonNull(siblingNodesComparator, "Sibling nodes comparator cannot be null.");

        // 1、检查是否存在重复节点
        check(list);

        // 2、预处理
        List<BaseNode<T, A>> nodes = prepare(list);

        // 3、以此节点为根构建节点树
        super.level = 1;         // root node level is 1
        super.path = null;       // reset with null
        super.leftLeafCount = 0; // root node left leaf count is 1
        super.siblingOrdinal = 1;
        mount(null, nodes, buildPath, siblingNodesComparator);

        // 4、检查是否有未连接的节点
        if (!ignoreUnlinked && CollectionUtils.isNotEmpty(nodes)) {
            throw new IllegalStateException("Unlinked node ids: " + Collects.convert(nodes, BaseNode::getNid));
        }

        // 5、统计
        count();
    }

    // -------------------------------------------------------------DFS

    /**
     * 深度优先搜索DFS(Depth-First Search)：使用前序遍历
     * <p>Should be invoking after {@link #mount(List, boolean, boolean, Comparator)}
     *
     * @return a list nodes for DFS tree node
     */
    public List<FlatNode<T, A>> flatDFS() {
        List<FlatNode<T, A>> list = Lists.newLinkedList();
        Deque<TreeNode<T, A>> stack = Collects.newLinkedList(this);
        while (!stack.isEmpty()) {
            TreeNode<T, A> node = stack.pop();
            list.add(new FlatNode<>(node));
            node.ifChildrenPresent(cs -> {
                // 反向遍历子节点
                for (Iterator<TreeNode<T, A>> iter = cs.descendingIterator(); iter.hasNext(); ) {
                    stack.push(iter.next());
                }
            });
        }
        return list;
    }

    /*
    // 递归方式DFS
    public List<FlatNode<T, A>> flatDFS() {
        List<FlatNode<T, A>> list = Lists.newLinkedList();
        dfs(list);
        return list;
    }
    private void dfs(List<FlatNode<T, A>> list) {
        list.add(new FlatNode<>(this));
        forEachChild(child -> child.dfs(list));
    }
    */

    // -------------------------------------------------------------CFS

    /**
     * <pre>
     * 按层级方式展开节点：兄弟节点相邻
     * 子节点优先搜索CFS(Children-First Search)
     * Should be invoking after {@link #mount(List, boolean, boolean, Comparator)}
     * Note：为了构建复杂表头，保证左侧的叶子节点必须排在右侧叶子节点前面，此处不能用广度优先搜索策略
     * </pre>
     *
     * @return a list nodes for CFS tree node
     */
    public List<FlatNode<T, A>> flatCFS() {
        List<FlatNode<T, A>> list = Collects.newLinkedList(new FlatNode<>(this));
        Deque<TreeNode<T, A>> stack = Collects.newLinkedList(this);
        while (!stack.isEmpty()) {
            TreeNode<T, A> node = stack.pop();
            node.ifChildrenPresent(cs -> {
                cs.forEach(child -> list.add(new FlatNode<>(child)));

                // 反向遍历子节点
                for (Iterator<TreeNode<T, A>> iter = cs.descendingIterator(); iter.hasNext(); ) {
                    stack.push(iter.next());
                }
            });
        }
        return list;
    }

    /*
    // 递归方式CFS
    public List<FlatNode<T, A>> flatCFS() {
        List<FlatNode<T, A>> list = Collects.newLinkedList(new FlatNode<>(this));
        cfs(list);
        return list;
    }
    private void cfs(List<FlatNode<T, A>> list) {
        forEachChild(child -> list.add(new FlatNode<>(child)));
        forEachChild(child -> child.cfs(list));
    }
    */

    // -------------------------------------------------------------BFS

    /**
     * 广度优先遍历BFS(Breath-First Search)
     *
     * @return a list nodes for BFS tree node
     */
    public List<FlatNode<T, A>> flatBFS() {
        List<FlatNode<T, A>> list = new LinkedList<>();
        Queue<TreeNode<T, A>> queue = Collects.newLinkedList(this);
        while (!queue.isEmpty()) {
            for (int i = queue.size(); i > 0; i--) {
                TreeNode<T, A> node = Objects.requireNonNull(queue.poll());
                list.add(new FlatNode<>(node));
                node.forEachChild(queue::offer);
            }
        }
        return list;
    }

    /*
    // 递归方式BFS
    public List<FlatNode<T, A>> flatBFS() {
        List<FlatNode<T, A>> list = new LinkedList<>();
        Queue<TreeNode<T, A>> queue = Collects.newLinkedList(this);
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

    // -----------------------------------------------------------children

    public void ifChildrenPresent(Consumer<LinkedList<TreeNode<T, A>>> childrenProcessor) {
        if (!children.isEmpty()) {
            childrenProcessor.accept(children);
        }
    }

    public void forEachChild(Consumer<TreeNode<T, A>> childProcessor) {
        if (!children.isEmpty()) {
            children.forEach(childProcessor);
        }
    }

    // -----------------------------------------------------------tree traverse

    /**
     * Traverses the tree
     *
     * @param action the action function
     * @see com.google.common.graph.Traverser
     */
    public void traverse(Consumer<TreeNode<T, A>> action) {
        Deque<TreeNode<T, A>> stack = Collects.newLinkedList(this);
        while (!stack.isEmpty()) {
            TreeNode<T, A> node = stack.pop();
            action.accept(node);
            node.forEachChild(stack::push);
        }
    }

    // -----------------------------------------------------------convert to TreeTrait

    public <E extends TreeTrait<T, A, E>> E convert(Function<TreeNode<T, A>, E> convert) {
        return convert(convert, true);
    }

    public <E extends TreeTrait<T, A, E>> E convert(Function<TreeNode<T, A>, E> convert,
                                                    boolean containsUnavailable) {
        if (!available && !containsUnavailable) {
            // not contains unavailable node
            return null;
        }

        E root = convert.apply(this);
        convert(convert, root, containsUnavailable);
        return root;
    }

    public String print(Function<TreeNode<T, A>, CharSequence> nodeLabel) throws IOException {
        StringBuilder builder = new StringBuilder();
        new MultiwayTreePrinter<>(builder, nodeLabel, TreeNode::getChildren).print(this);
        return builder.toString();
    }

    @Override
    public String toString() {
        try {
            return print(e -> String.valueOf(e.getNid()));
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    // -----------------------------------------------------------private methods

    private <E extends BaseNode<T, A>> void check(List<E> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        // build graph
        Map<T, T> graph = new HashMap<>(list.size() * 2);
        graph.put(super.nid, super.pid);
        for (E node : list) {
            if (graph.containsKey(node.nid)) {
                throw new IllegalStateException("Duplicated node id: " + node.nid);
            }
            graph.put(node.nid, node.pid);
        }

        // check has cycled
        Set<T> set = new HashSet<>();
        for (T cid : graph.keySet()) {
            if (hasCycle(cid, set, graph)) {
                throw new IllegalStateException("Cycled node id: " + cid);
            }
        }
    }

    private static <T> boolean hasCycle(T nid, Set<T> set, Map<T, T> graph) {
        if (!set.add(nid)) {
            return true;
        }
        if (graph.containsKey(nid)) {
            T pid = graph.get(nid);
            // 如果`nid==null && pid==null`，视为根节点，无需校验
            if ((nid != null || pid != null) && hasCycle(pid, set, graph)) {
                return true;
            }
        }
        set.remove(nid);
        return false;
    }

    private <E extends BaseNode<T, A>> List<BaseNode<T, A>> prepare(List<E> nodes) {
        List<BaseNode<T, A>> list = new LinkedList<>();

        // nodes list
        if (CollectionUtils.isNotEmpty(nodes)) {
            for (BaseNode<T, A> node : nodes) {
                if (node instanceof TreeNode) {
                    // if tree node, then add all the tree nodes that includes the node's children(recursive)
                    ((TreeNode<T, A>) node).traverse(list::add);
                } else {
                    list.add(node);
                }
            }
        }

        // the root node children
        ifChildrenPresent(cs -> {
            cs.forEach(child -> child.traverse(list::add));
            // clear the root node children before mount
            cs.clear();
        });

        return list;
    }

    private <E extends BaseNode<T, A>> void mount(NodePath<T> parentPath, List<E> nodes, boolean buildPath,
                                                  Comparator<? super TreeNode<T, A>> siblingNodesComparator) {
        if (buildPath) {
            super.path = (parentPath == null) ? new NodePath<>(nid) : new NodePath<>(parentPath, nid);
        } else {
            super.path = null;
        }

        // find child nodes for the current node
        for (Iterator<E> iter = nodes.iterator(); iter.hasNext(); ) {
            BaseNode<T, A> node = iter.next();

            if (Objects.equals(super.nid, node.pid)) {
                // found a child node
                TreeNode<T, A> child = new TreeNode<>(
                    node.nid,
                    node.pid,
                    node.enabled,
                    // recompute the child node is available
                    super.available && node.available && node.enabled,
                    node.attach
                );
                child.level = super.level + 1;
                children.add(child);

                // remove the found child node
                iter.remove();
            }
        }

        ifChildrenPresent(cs -> {
            // recursion to mount child tree
            cs.forEach(child -> child.mount(path, nodes, buildPath, siblingNodesComparator));
            // sort the children list(sibling nodes sort)
            cs.sort(siblingNodesComparator);
        });

        super.degree = children.size();
    }

    private void count() {
        if (children.isEmpty()) {
            // 叶子节点
            super.treeDepth     = 1;
            super.treeMaxDegree = 0;
            super.treeLeafCount = 1;
            super.treeNodeCount = 1;
            super.childrenCount = 0;
            return;
        }

        // 非叶子节点
        int maxChildTreeDepth        = 0,
            maxChildTreeMaxDegree    = 0,
            sumChildrenTreeLeafCount = 0,
            sumChildrenTreeNodeCount = 0;
        TreeNode<T, A> child;
        for (int i = 0; i < children.size(); i++) {
            child = children.get(i);
            child.siblingOrdinal = i + 1;

            // 1、统计左叶子节点数量
            if (i == 0) {
                // 是最左子节点：左叶子节点个数=父节点的左叶子节点个数
                child.leftLeafCount = super.leftLeafCount;
            } else {
                // 非最左子节点：左叶子节点个数=相邻左兄弟节点的左叶子节点个数+该兄弟节点的子节点个数
                TreeNode<T, A> prevSibling = children.get(i - 1);
                child.leftLeafCount = prevSibling.leftLeafCount + prevSibling.treeLeafCount;
            }

            // 2、递归
            child.count();

            // 3、统计子叶子节点数量及整棵树节点的数量
            maxChildTreeDepth         = Math.max(maxChildTreeDepth, child.treeDepth);
            maxChildTreeMaxDegree     = Math.max(maxChildTreeMaxDegree, child.degree);
            sumChildrenTreeLeafCount += child.treeLeafCount;
            sumChildrenTreeNodeCount += child.treeNodeCount;
        }

        super.treeDepth     = maxChildTreeDepth + 1;                         // 加上自身节点的层级
        super.treeMaxDegree = Math.max(maxChildTreeMaxDegree, super.degree); // 树中的最大度数
        super.treeLeafCount = sumChildrenTreeLeafCount;                      // 子节点的叶子节点之和
        super.treeNodeCount = sumChildrenTreeNodeCount + 1;                  // 要包含节点本身
        super.childrenCount = children.size();                               // 子节点数量
    }

    private <E extends TreeTrait<T, A, E>> void convert(Function<TreeNode<T, A>, E> convert,
                                                        E parent, boolean containsUnavailable) {
        if (children.isEmpty()) {
            parent.setChildren(null);
            return;
        }

        List<E> list = new LinkedList<>();
        for (TreeNode<T, A> child : children) {
            // filter unavailable
            if (child.available || containsUnavailable) {
                E node = convert.apply(child);
                child.convert(convert, node, containsUnavailable);
                list.add(node);
            }
        }
        parent.setChildren(list);
    }

}
