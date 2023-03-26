/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.graph;

import org.springframework.util.Assert;

import java.util.Objects;

/**
 * Graph node id
 *
 * @author Ponfee
 */
public class GraphNodeId {

    public static final GraphNodeId HEAD = new GraphNodeId(0, 0, "HEAD");
    public static final GraphNodeId TAIL = new GraphNodeId(0, 0, "TAIL");

    /**
     * Flow id
     */
    public final int flowId;

    /**
     * Name id
     */
    public final int nameId;

    /**
     * Name content
     */
    public final String name;

    private GraphNodeId(int flowId, int nameId, String name) {
        this.flowId = flowId;
        this.nameId = nameId;
        this.name = name;
    }

    public static GraphNodeId of(int flowId, int nameId, String name) {
        Assert.isTrue(flowId > 0, "Invalid graph node flow id: " + flowId);
        Assert.isTrue(nameId > 0, "Invalid graph node name id: " + nameId);
        Assert.hasText(name, "Invalid graph node name content: " + name);
        return new GraphNodeId(flowId, nameId, name);
    }

    public boolean isHead() {
        return this.equals(HEAD);
    }

    public boolean isTail() {
        return this.equals(TAIL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowId, nameId, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GraphNodeId)) {
            return false;
        }

        GraphNodeId other = (GraphNodeId) obj;
        return this.flowId == other.flowId
            && this.nameId == other.nameId
            && this.name.equals(other.name);
    }

    @Override
    public String toString() {
        return flowId + DAGParser.SEP_NAMING + nameId + DAGParser.SEP_NAMING + name;
    }

}
