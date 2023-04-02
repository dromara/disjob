/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.graph;

import cn.ponfee.scheduler.common.base.Symbol.Str;
import org.springframework.util.Assert;

import java.beans.Transient;
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
     * Flow
     */
    public final int section;

    /**
     * Ordinal
     */
    public final int ordinal;

    /**
     * Name
     */
    public final String name;

    private GraphNodeId(int section, int ordinal, String name) {
        this.section = section;
        this.ordinal = ordinal;
        this.name = name;
    }

    public static GraphNodeId of(int section, int ordinal, String name) {
        Assert.isTrue(section > 0, "Graph node section must be greater than 0: " + section);
        Assert.isTrue(ordinal > 0, "Graph node ordinal must be greater than 0: " + ordinal);
        Assert.hasText(name, "Graph node name cannot be blank: " + name);
        return new GraphNodeId(section, ordinal, name);
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
    public boolean isHead() {
        return this.equals(HEAD);
    }

    @Transient
    public boolean isTail() {
        return this.equals(TAIL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(section, ordinal, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GraphNodeId)) {
            return false;
        }

        GraphNodeId other = (GraphNodeId) obj;
        return this.section == other.section
            && this.ordinal == other.ordinal
            && this.name.equals(other.name);
    }

    @Override
    public String toString() {
        return section + Str.COLON + ordinal + Str.COLON + name;
    }

}
