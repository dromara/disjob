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
     * Block id
     */
    public final int a;

    /**
     * name id
     */
    public final int b;

    /**
     * name
     */
    public final String c;

    private GraphNodeId(int a, int b, String c) {
        Assert.isTrue(a >= 0, "Invalid graph node block id: " + a);
        Assert.isTrue(b >= 0, "Invalid graph node name id: " + a);
        Assert.hasText(c, "Invalid graph node name: " + a);
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public static GraphNodeId of(int a, int b, String c) {
        return new GraphNodeId(a, b, c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GraphNodeId)) {
            return false;
        }

        GraphNodeId other = (GraphNodeId) obj;
        return this.a == other.a
            && this.b == other.b
            && this.c.equals(other.c);
    }

    @Override
    public String toString() {
        return a + DAGParser.SEP_NAMING + b + DAGParser.SEP_NAMING + c;
    }

}
