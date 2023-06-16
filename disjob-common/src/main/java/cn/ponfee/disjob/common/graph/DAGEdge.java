/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.graph;

import cn.ponfee.disjob.common.base.ToJsonString;
import com.google.common.graph.EndpointPair;

import java.io.Serializable;
import java.util.Objects;

/**
 * Graph edge
 *
 * @author Ponfee
 */
public final class DAGEdge extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 2292231888365728538L;

    private final DAGNode source;
    private final DAGNode target;

    private DAGEdge(DAGNode source, DAGNode target) {
        this.source = source;
        this.target = target;
    }

    public static DAGEdge of(DAGNode source, DAGNode target) {
        Objects.requireNonNull(source, "DAG source node cannot be null.");
        Objects.requireNonNull(target, "DAG target node cannot be null.");
        return new DAGEdge(source, target);
    }

    public static DAGEdge of(String source, String target) {
        Objects.requireNonNull(source, "DAG source node cannot be blank.");
        Objects.requireNonNull(target, "DAG target node cannot be blank.");
        return new DAGEdge(DAGNode.fromString(source), DAGNode.fromString(target));
    }

    public static DAGEdge of(EndpointPair<DAGNode> pair) {
        Objects.requireNonNull(pair, "DAG node pair cannot be blank.");
        return new DAGEdge(pair.source(), pair.target());
    }

    public DAGNode getSource() {
        return source;
    }

    public DAGNode getTarget() {
        return target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DAGEdge)) {
            return false;
        }

        DAGEdge other = (DAGEdge) obj;
        return this.source.equals(other.source)
            && this.target.equals(other.target);
    }

}
