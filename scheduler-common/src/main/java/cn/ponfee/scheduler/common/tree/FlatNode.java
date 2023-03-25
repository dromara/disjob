/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.tree;

import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 节点扁平结构
 *
 * @param <T> the node id type
 * @param <A> the attachment biz object type
 * @author Ponfee
 */
public final class FlatNode<T extends Serializable & Comparable<? super T>, A> extends BaseNode<T, A> {
    private static final long serialVersionUID = 5191371614061952661L;

    private final boolean leaf; // 是否叶子节点

    FlatNode(TreeNode<T, A> n) {
        super(n.nid, n.pid, n.enabled, n.available, n.attach);

        super.level  = n.level;
        super.degree = n.degree;
        super.path   = n.path;

        super.leftLeafCount = n.leftLeafCount;

        super.treeDepth      = n.treeDepth;
        super.treeNodeCount  = n.treeNodeCount;
        super.treeMaxDegree  = n.treeMaxDegree;
        super.treeLeafCount  = n.treeLeafCount;
        super.childrenCount  = n.childrenCount;
        super.siblingOrdinal = n.siblingOrdinal;

        this.leaf = CollectionUtils.isEmpty(n.getChildren());
    }

    public <R> R convert(Function<FlatNode<T, A>, R> convertor) {
        return convertor.apply(this);
    }

    // ----------------------------------------------getter/setter
    public boolean isLeaf() {
        return leaf;
    }

}
