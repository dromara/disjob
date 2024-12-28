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

import lombok.Getter;
import org.springframework.util.Assert;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Objects;

import static cn.ponfee.disjob.common.base.Symbol.Str.COLON;

/**
 * DAG Node
 *
 * @author Ponfee
 */
@Getter
public final class DAGNode implements Serializable {
    private static final long serialVersionUID = 7413110685194391605L;

    public static final DAGNode START = new DAGNode(0, 0, "Start");
    public static final DAGNode END   = new DAGNode(0, 0, "End");

    /**
     * <pre>
     *  拓扑图(任务)的编号，用来区分不同的任务，从1开始
     *  如[A -> B; C -> D]，表达式用“;”分隔成两个不同的任务
     *  topology-1: A -> B
     *  topology-2: C -> D
     * </pre>
     */
    private final int topology;

    /**
     * 名称相同时通过顺序来区分，从1开始
     * <p>如 [A -> B -> A] 中的两个A是不同的节点，实际为 [1:1:A -> 1:1:B -> 1:2:A]
     */
    private final int ordinal;

    /**
     * 名称
     */
    private final String name;

    private DAGNode(int topology, int ordinal, String name) {
        this.topology = topology;
        this.ordinal = ordinal;
        this.name = name;
    }

    public static DAGNode of(int topology, int ordinal, String name) {
        Assert.isTrue(topology > 0, () -> "Topology must be greater than 0: " + topology);
        Assert.isTrue(ordinal > 0, () -> "Ordinal must be greater than 0: " + ordinal);
        Assert.hasText(name, () -> "Name cannot be blank: " + name);
        return new DAGNode(topology, ordinal, name.trim());
    }

    @Transient
    public boolean isStart() {
        return this.equals(START);
    }

    @Transient
    public boolean isEnd() {
        return this.equals(END);
    }

    @Transient
    public boolean isStartOrEnd() {
        return isStart() || isEnd();
    }

    @Override
    public int hashCode() {
        return Objects.hash(topology, ordinal, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DAGNode)) {
            return false;
        }
        DAGNode that = (DAGNode) obj;
        return this.topology == that.topology
            && this.ordinal == that.ordinal
            && this.name.equals(that.name);
    }

    @Override
    public String toString() {
        return topology + COLON + ordinal + COLON + name;
    }

    public boolean equals(int topology, int ordinal, String name) {
        return this.topology == topology
            && this.ordinal == ordinal
            && this.name.equals(name);
    }

    public static DAGNode fromString(String str) {
        Assert.hasText(str, "DAG node text cannot be blank.");
        String[] array = str.trim().split(COLON, 3);
        int topology = Integer.parseInt(array[0]);
        int ordinal = Integer.parseInt(array[1]);
        String name = array[2].trim();
        if (START.equals(topology, ordinal, name)) {
            return START;
        }
        if (END.equals(topology, ordinal, name)) {
            return END;
        }
        return DAGNode.of(topology, ordinal, name);
    }

}
