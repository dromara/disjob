/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.graph;

import org.springframework.util.Assert;

import java.beans.Transient;
import java.util.Objects;

import static cn.ponfee.scheduler.common.base.Symbol.Str.COLON;

/**
 * Graph node id
 *
 * @author Ponfee
 */
public final class GraphNodeId {

    public static final GraphNodeId START = new GraphNodeId(0, 0, "Start");
    public static final GraphNodeId END = new GraphNodeId(0, 0, "End");

    /**
     * Section
     */
    private final int section;

    /**
     * Ordinal
     */
    private final int ordinal;

    /**
     * Name
     */
    private final String name;

    private GraphNodeId(int section, int ordinal, String name) {
        this.section = section;
        this.ordinal = ordinal;
        this.name = name;
    }

    public static GraphNodeId of(int section, int ordinal, String name) {
        Assert.isTrue(section > 0, () -> "Graph node section must be greater than 0: " + section);
        Assert.isTrue(ordinal > 0, () -> "Graph node ordinal must be greater than 0: " + ordinal);
        Assert.hasText(name, () -> "Graph node name cannot be blank: " + name);
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
    public boolean isStart() {
        return this.equals(START);
    }

    @Transient
    public boolean isEnd() {
        return this.equals(END);
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
        if (!(obj instanceof GraphNodeId)) {
            return false;
        }
        GraphNodeId other = (GraphNodeId) obj;
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

    public static GraphNodeId fromString(String str) {
        int pos = -1;
        int section = Integer.parseInt(str.substring(pos += 1, pos = str.indexOf(COLON, pos)));
        int ordinal = Integer.parseInt(str.substring(pos += 1, pos = str.indexOf(COLON, pos)));
        String name = str.substring(pos + 1);
        if (START.equals(section, ordinal, name)) {
            return START;
        }
        if (END.equals(section, ordinal, name)) {
            return END;
        }
        return GraphNodeId.of(section, ordinal, name);
    }

}
