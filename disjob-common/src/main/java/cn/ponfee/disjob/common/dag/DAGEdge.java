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

package cn.ponfee.disjob.common.dag;

import cn.ponfee.disjob.common.base.ToJsonString;
import com.google.common.graph.EndpointPair;

import java.io.Serializable;
import java.util.Objects;

/**
 * DAG Edge
 *
 * @author Ponfee
 */
public final class DAGEdge extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 2292231888365728538L;

    private final DAGNode source;
    private final DAGNode target;

    private DAGEdge(DAGNode source, DAGNode target) {
        this.source = Objects.requireNonNull(source, "DAG source node cannot be null.");
        this.target = Objects.requireNonNull(target, "DAG target node cannot be null.");
    }

    public static DAGEdge of(DAGNode source, DAGNode target) {
        return new DAGEdge(source, target);
    }

    public static DAGEdge of(String source, String target) {
        Objects.requireNonNull(source, "DAG source text cannot be blank.");
        Objects.requireNonNull(target, "DAG target text cannot be blank.");
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
