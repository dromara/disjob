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
import cn.ponfee.disjob.common.util.Fields;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

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
public final class TreeNode<T extends Serializable & Comparable<T>, A> extends BaseNode<T, A> {
    private static final long serialVersionUID = -9081626363752680404L;

    /**
     * 用于比较兄弟节点次序
     */
    private final Comparator<? super TreeNode<T, A>> siblingNodesComparator;

    /**
     * 子节点列表（空列表则表示为叶子节点）
     */
    private final LinkedList<TreeNode<T, A>> children = new LinkedList<>();

    /**
     * 是否构建path
     */
    private final boolean buildPath;

    /**
     * Constructs a tree node
     *
     * @param nid                    the node id
     * @param pid                    the parent node id(withhold this pid field value,when use if the other root node mount this node as child)
     * @param enabled                the node is enabled
     * @param available              the current node is available(parent.available & this.enabled)
     * @param attach                 the attachment for biz object
     * @param siblingNodesComparator the comparator for sort sibling nodes(has the same parent node)
     * @param buildPath              the if whether build path
     * @param doMount                the if whether do mount, if is inner new TreeNode then false else true
     */
    TreeNode(T nid,
             T pid,
             boolean enabled,
             boolean available,
             A attach,
             Comparator<? super TreeNode<T, A>> siblingNodesComparator,
             boolean buildPath,
             boolean doMount) {
        super(nid, pid, enabled, available, attach);

        this.siblingNodesComparator = Objects.requireNonNull(siblingNodesComparator);

        this.buildPath = buildPath;

        if (doMount) {
            // as root node if new instance at external(TreeNodeBuilder) or of(TreeNode)
            mount(null);
        }
    }

    public static <T extends Serializable & Comparable<T>, A> TreeNodeBuilder<T, A> builder(T nid) {
        return new TreeNodeBuilder<>(nid);
    }

    // ------------------------------------------------------mount children nodes

    public <E extends BaseNode<T, A>> TreeNode<T, A> mount(List<E> nodes) {
        mount(nodes, false);
        return this;
    }

    /**
     * Mount a tree
     *
     * @param list         子节点列表
     * @param ignoreOrphan {@code true}忽略孤儿节点，{@code false}如果有孤儿节点则会抛异常
     */
    public <E extends BaseNode<T, A>> TreeNode<T, A> mount(List<E> list, boolean ignoreOrphan) {
        if (list == null) {
            list = Collections.emptyList();
        }

        // 1、预处理
        List<BaseNode<T, A>> nodes = prepare(list);

        // 2、检查是否存在重复节点
        List<T> checkDuplicateList = Collects.newLinkedList(super.nid);
        nodes.forEach(n -> checkDuplicateList.add(n.nid));
        List<T> duplicated = Collects.duplicate(checkDuplicateList);
        if (CollectionUtils.isNotEmpty(duplicated)) {
            throw new IllegalStateException("Duplicated nodes: " + duplicated);
        }

        // 3、以此节点为根构建节点树
        super.level = 1;         // root node level is 1
        super.path = null;       // reset with null
        super.leftLeafCount = 0; // root node left leaf count is 1
        super.siblingOrdinal = 1;
        mount0(null, nodes, ignoreOrphan, super.nid);

        // 4、检查是否存在孤儿节点
        if (!ignoreOrphan && CollectionUtils.isNotEmpty(nodes)) {
            String nids = nodes.stream().map(e -> String.valueOf(e.getNid())).collect(Collectors.joining(","));
            throw new IllegalStateException("Invalid orphan nodes: [" + nids + "]");
        }

        // 5、统计
        count();

        return this;
    }

    // -------------------------------------------------------------DFS

    /**
     * 深度优先搜索DFS(Depth-First Search)：使用前序遍历
     * <p>Should be invoking after {@link #mount(List)}
     *
     * @return a list nodes for DFS tree node
     */
    public List<FlatNode<T, A>> flatDFS() {
        List<FlatNode<T, A>> collect = Lists.newLinkedList();
        Deque<TreeNode<T, A>> stack = Collects.newLinkedList(this);
        while (!stack.isEmpty()) {
            TreeNode<T, A> node = stack.pop();
            collect.add(new FlatNode<>(node));
            node.ifChildrenPresent(cs -> {
                // 反向遍历子节点
                for (Iterator<TreeNode<T, A>> iter = cs.descendingIterator(); iter.hasNext(); ) {
                    stack.push(iter.next());
                }
            });
        }
        return collect;
    }

    /*
    // 递归方式DFS
    public List<FlatNode<T, A>> flatDFS() {
        List<FlatNode<T, A>> collect = Lists.newLinkedList();
        dfs(collect);
        return collect;
    }
    private void dfs(List<FlatNode<T, A>> collect) {
        collect.add(new FlatNode<>(this));
        forEachChild(child -> child.dfs(collect));
    }
    */

    // -------------------------------------------------------------CFS

    /**
     * 按层级方式展开节点：兄弟节点相邻
     * <p>子节点优先搜索CFS(Children-First Search)
     * <p>Should be invoking after {@link #mount(List)}
     * <p>Note：为了构建复杂表头，保证左侧的叶子节点必须排在右侧叶子节点前面，此处不能用广度优先搜索策略
     *
     * @return a list nodes for CFS tree node
     */
    public List<FlatNode<T, A>> flatCFS() {
        List<FlatNode<T, A>> collect = Collects.newLinkedList(new FlatNode<>(this));
        Deque<TreeNode<T, A>> stack = Collects.newLinkedList(this);
        while (!stack.isEmpty()) {
            TreeNode<T, A> node = stack.pop();
            node.ifChildrenPresent(cs -> {
                cs.forEach(child -> collect.add(new FlatNode<>(child)));

                // 反向遍历子节点
                for (Iterator<TreeNode<T, A>> iter = cs.descendingIterator(); iter.hasNext(); ) {
                    stack.push(iter.next());
                }
            });
        }
        return collect;
    }

    /*
    // 递归方式CFS
    public List<FlatNode<T, A>> flatCFS() {
        List<FlatNode<T, A>> collect = Collects.newLinkedList(new FlatNode<>(this));
        cfs(collect);
        return collect;
    }
    private void cfs(List<FlatNode<T, A>> collect) {
        forEachChild(child -> collect.add(new FlatNode<>(child)));
        forEachChild(child -> child.cfs(collect));
    }
    */

    // -------------------------------------------------------------BFS

    /**
     * 广度优先遍历BFS(Breath-First Search)
     *
     * @return a list nodes for BFS tree node
     */
    public List<FlatNode<T, A>> flatBFS() {
        List<FlatNode<T, A>> collect = new LinkedList<>();
        Queue<TreeNode<T, A>> queue = Collects.newLinkedList(this);
        while (!queue.isEmpty()) {
            for (int i = queue.size(); i > 0; i--) {
                TreeNode<T, A> node = queue.poll();
                collect.add(new FlatNode<>(node));
                node.forEachChild(queue::offer);
            }
        }
        return collect;
    }

    /*
    // 递归方式BFS
    public List<FlatNode<T, A>> flatBFS() {
        List<FlatNode<T, A>> collect = new LinkedList<>();
        Queue<TreeNode<T, A>> queue = Collects.newLinkedList(this);
        bfs(queue, collect);
        return collect;
    }
    private void bfs(Queue<TreeNode<T, A>> queue, List<FlatNode<T, A>> collect) {
        int size = queue.size();
        if (size == 0) {
            return;
        }
        while (size-- > 0) {
            TreeNode<T, A> node = queue.poll();
            collect.add(new FlatNode<>(node));
            node.forEachChild(queue::offer);
        }
        bfs(queue, collect);
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

    public LinkedList<TreeNode<T, A>> getChildren() {
        return children;
    }

    // -----------------------------------------------------------private methods

    private <E extends BaseNode<T, A>> List<BaseNode<T, A>> prepare(List<E> nodes) {
        List<BaseNode<T, A>> list = Lists.newLinkedList();

        // nodes list
        for (BaseNode<T, A> node : nodes) {
            if (node instanceof TreeNode) {
                // if tree node, then add all the tree nodes that includes the node's children(recursive)
                ((TreeNode<T, A>) node).traverse(list::add);
            } else {
                list.add(node); // node.clone()
            }
        }

        // the root node children
        ifChildrenPresent(cs -> {
            cs.forEach(child -> child.traverse(list::add));
            cs.clear(); // clear the root node children before mount
        });
        return list;
    }

    private <E extends BaseNode<T, A>> void mount0(List<T> parentPath, List<E> nodes,
                                                   boolean ignoreOrphan, T mountPidIfNull) {
        // current "this" is parent: AbstractNode parent = this;

        // 当前节点路径=父节点路径+当前节点
        // the "super" means defined in super class BaseNode's field, is not parent node
        super.path = buildPath(parentPath, super.nid);

        // find child nodes for the current node
        for (Iterator<E> iter = nodes.iterator(); iter.hasNext(); ) {
            BaseNode<T, A> node = iter.next();

            if (!ignoreOrphan && ObjectUtils.isEmpty(node.pid)) { // effect condition that pid is null
                // 不忽略孤儿节点且节点的父节点为空，则其父节点视为根节点（将其挂载到根节点下）
                Fields.put(node, "pid", mountPidIfNull); // pid is final modify
            }

            if (super.nid.equals(node.pid)) {
                // found a child node
                TreeNode<T, A> child = new TreeNode<>(
                    node.nid,
                    node.pid,
                    node.enabled,
                    super.available && node.enabled, // recompute the child node is available
                    node.attach,
                    this.siblingNodesComparator,
                    this.buildPath,
                    false
                );

                child.level = super.level + 1;
                children.add(child); // 挂载子节点

                iter.remove(); // remove the found child node
            }
        }

        ifChildrenPresent(cs -> {
            // recursion to mount child tree
            cs.forEach(child -> child.mount0(path, nodes, ignoreOrphan, mountPidIfNull));

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

    /**
     * Returns a immutable list for current node path
     *
     * @param parentPath the parent node path
     * @param nid        the current node id
     * @return a immutable list appended current node id
     */
    private List<T> buildPath(List<T> parentPath, T nid) {
        if (!buildPath) {
            return null;
        }

        // already check duplicated, so cannot happen exists circular dependencies
        /*
        if (IterableUtils.matchesAny(parentPath, nid::equals)) {
            // 节点路径中已经包含了此节点，则视为环状
            throw new IllegalStateException("Node circular dependencies: " + parentPath + " -> " + nid);
        }
        */

        if (parentPath == null) {
            // root node un-contains null parent
            return Collections.singletonList(nid);
        }

        return ImmutableList.<T>builderWithExpectedSize(parentPath.size() + 1).addAll(parentPath).add(nid).build();
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
