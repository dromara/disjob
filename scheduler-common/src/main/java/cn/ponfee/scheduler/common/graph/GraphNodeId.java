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
     * Serial
     */
    public final int serial;

    /**
     * Name
     */
    public final String name;

    private GraphNodeId(int section, int serial, String name) {
        this.section = section;
        this.serial = serial;
        this.name = name;
    }

    public static GraphNodeId of(int section, int serial, String name) {
        Assert.isTrue(section > 0, "Invalid graph node section: " + section);
        Assert.isTrue(serial > 0, "Invalid graph node serial: " + serial);
        Assert.hasText(name, "Invalid graph node name: " + name);
        return new GraphNodeId(section, serial, name);
    }

    public int getSection() {
        return section;
    }

    public int getSerial() {
        return serial;
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
        return Objects.hash(section, serial, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GraphNodeId)) {
            return false;
        }

        GraphNodeId other = (GraphNodeId) obj;
        return this.section == other.section
            && this.serial == other.serial
            && this.name.equals(other.name);
    }

    @Override
    public String toString() {
        return section + Str.COLON + serial + Str.COLON + name;
    }

}
