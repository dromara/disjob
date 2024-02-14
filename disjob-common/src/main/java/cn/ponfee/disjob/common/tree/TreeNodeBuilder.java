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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * Builds tree node as root node
 *
 * @param <T> the node id type
 * @param <A> the attachment biz object type
 * @author Ponfee
 */
public final class TreeNodeBuilder<T extends Serializable & Comparable<T>, A> {

    private final T nid;

    private Comparator<? super TreeNode<T, A>> siblingNodesComparator = Comparator.comparing(TreeNode::getNid);
    private T       pid       = null;
    private boolean enabled   = true;
    private boolean available = true;
    private A       attach    = null;
    private boolean buildPath = true;

    TreeNodeBuilder(T nid) {
        this.nid = Objects.requireNonNull(nid);
    }

    public TreeNodeBuilder<T, A> pid(T pid) {
        this.pid = pid;
        return this;
    }

    public TreeNodeBuilder<T, A> siblingNodesComparator(Comparator<? super TreeNode<T, A>> comparator) {
        this.siblingNodesComparator = Objects.requireNonNull(comparator, "Sibling nodes comparator cannot be null.");
        return this;
    }

    public TreeNodeBuilder<T, A> enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public TreeNodeBuilder<T, A> available(boolean available) {
        this.available = available;
        return this;
    }

    public TreeNodeBuilder<T, A> attach(A attach) {
        this.attach = attach;
        return this;
    }

    public TreeNodeBuilder<T, A> buildPath(boolean buildPath) {
        this.buildPath = buildPath;
        return this;
    }

    public TreeNode<T, A> build() {
        return new TreeNode<>(nid, pid, enabled, available, attach, siblingNodesComparator, buildPath, true);
    }

}
