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

import lombok.Getter;

import java.io.Serializable;

/**
 * 基于树形结构节点的基类
 *
 * @param <T> the node id type
 * @param <A> the attachment biz object type
 * @author Ponfee
 */
@Getter
abstract class BaseTree<T extends Serializable & Comparable<T>, A> extends PlainNode<T, A> {
    private static final long serialVersionUID = -4116799955526185765L;

    // -------------------------------------------------------------------整棵树的视角

    protected int level;          // 节点层级：根节点为0开始往下逐级加1，与`depth(从根到该节点路径上边的个数)`的含义有些区别但值相同
    protected NodePath<T> path;   // 节点路径`NodePath<id>`：[rootId, ..., parentId, nodeId]
    protected int leftLeafCount;  // 左叶子节点数量：在其左边的所有叶子节点数量，相邻左兄弟节点的左叶子节点个数+该兄弟节点的子节点个数

    // -------------------------------------------------------------------当前节点视角

    protected int nodeDegree;     // 当前节点的度数：即直系子节点个数，叶子节点的度为0，树中所有节点的个数=所有节点度数之和+1
    protected int treeDegree;     // 以当前节点为根的树中度数的最大值
    protected int treeHeight;     // 以当前节点为根的树高度：从该节点到其最远叶子节点路径上的边数，叶子节点的树高度为0
    protected int treeNodeCount;  // 以当前节点为根的树中的节点数量
    protected int treeLeafCount;  // 以当前节点为根的树中的叶子节点数量，当前是叶子节点时此值为1
    protected int siblingOrdinal; // 兄弟节点按顺序排行，从0开始

    // -------------------------------------------------------------------Constructor

    BaseTree(T id, T parentId) {
        super(id, parentId);
    }

    BaseTree(T id, T parentId, boolean enabled, boolean available, A attach) {
        super(id, parentId, enabled, available, attach);
    }

}
