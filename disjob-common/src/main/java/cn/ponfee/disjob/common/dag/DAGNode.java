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

import cn.ponfee.disjob.common.base.TextTokenizer;
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
public final class DAGNode implements Serializable {
    private static final long serialVersionUID = 7413110685194391605L;

    public static final DAGNode START = new DAGNode(0, 0, "Start");
    public static final DAGNode END   = new DAGNode(0, 0, "End");

    /**
     * <pre>
     *  任务链的编号，用来区分不同的任务链
     *  如[A -> B; C -> D]，表达式用“;”分隔成两个不同的任务链
     *  section=1： A -> B
     *  section=2： C -> D
     * </pre>
     */
    private final int section;

    /**
     * 名称相同时通过顺序来区分，如[A -> B -> A]，两个A是不同的
     * <p>实际结果为 [1:1:A -> 1:1:B -> 1:2:A]
     */
    private final int ordinal;

    /**
     * 名称
     */
    private final String name;

    private DAGNode(int section, int ordinal, String name) {
        this.section = section;
        this.ordinal = ordinal;
        this.name = name;
    }

    public static DAGNode of(int section, int ordinal, String name) {
        Assert.isTrue(section > 0, () -> "Graph node section must be greater than 0: " + section);
        Assert.isTrue(ordinal > 0, () -> "Graph node ordinal must be greater than 0: " + ordinal);
        Assert.hasText(name, () -> "Graph node name cannot be blank: " + name);
        return new DAGNode(section, ordinal, name);
    }

    public int getSection() {
        return section;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public String getName() {
        return name;
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
        return Objects.hash(section, ordinal, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DAGNode)) {
            return false;
        }
        DAGNode other = (DAGNode) obj;
        return this.section == other.section
            && this.ordinal == other.ordinal
            && this.name.equals(other.name);
    }

    public boolean equals(int section, int ordinal, String name) {
        return this.section == section
            && this.ordinal == ordinal
            && this.name.equals(name);
    }

    @Override
    public String toString() {
        return section + COLON + ordinal + COLON + name;
    }

    public static DAGNode fromString(String str) {
        TextTokenizer tokenizer = new TextTokenizer(str, COLON);
        int section = Integer.parseInt(tokenizer.next());
        int ordinal = Integer.parseInt(tokenizer.next());
        String name = tokenizer.tail();
        if (START.equals(section, ordinal, name)) {
            return START;
        }
        if (END.equals(section, ordinal, name)) {
            return END;
        }
        return DAGNode.of(section, ordinal, name);
    }

}
