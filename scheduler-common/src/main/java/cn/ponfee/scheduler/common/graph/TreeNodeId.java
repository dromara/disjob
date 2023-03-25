/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.graph;

import java.io.Serializable;
import java.util.Objects;

/**
 * Tree node id
 *
 * @author Ponfee
 */
public class TreeNodeId implements Serializable, Comparable<TreeNodeId> {
    private static final long serialVersionUID = 5761050473836675590L;

    public static final TreeNodeId ROOT_ID = TreeNodeId.of(-1, -1);

    /**
     * position of "("
     */
    public final int a;

    /**
     * position of ")"
     */
    public final int b;

    private TreeNodeId(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public static TreeNodeId of(int a, int b) {
        return new TreeNodeId(a, b);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TreeNodeId)) {
            return false;
        }
        TreeNodeId other = (TreeNodeId) obj;
        return this.a == other.a && this.b == other.b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public int compareTo(TreeNodeId other) {
        int n = this.a - other.a;
        return n != 0 ? n : this.b - other.b;
    }

    @Override
    public String toString() {
        return "(" + a + "," + b + ")";
    }

}
