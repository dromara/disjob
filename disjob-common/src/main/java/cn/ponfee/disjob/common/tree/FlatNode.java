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
public final class FlatNode<T extends Serializable & Comparable<T>, A> extends BaseNode<T, A> {
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
