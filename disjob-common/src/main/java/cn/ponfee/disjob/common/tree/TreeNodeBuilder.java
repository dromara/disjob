/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
public final class TreeNodeBuilder<T extends Serializable & Comparable<? super T>, A> {

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
