/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.tree;

import cn.ponfee.disjob.common.base.ToJsonString;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Objects;

/**
 * Base node id
 *
 * @author Ponfee
 * @param <T> the NodeId implementation sub class
 */
public abstract class NodeId<T extends NodeId<T>> extends ToJsonString implements Comparable<T>, Serializable, Cloneable {

    private static final long serialVersionUID = -9004940918491918780L;

    protected final T parent;

    public NodeId(T parent) {
        this.parent = parent;
    }

    @Override @SuppressWarnings("unchecked")
    public final boolean equals(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        T o = (T) obj;
        return Objects.equals(this.parent, o.parent) && this.equals(o);
    }

    @Override
    public final int compareTo(T another) {
        if (this.parent == null) {
            return another.parent == null ? this.compare(another) : -1;
        }
        if (another.parent == null) {
            return 1;
        }

        int a = this.parent.compareTo(another.parent);
        return a != 0 ? a : this.compare(another);
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder()
            .append(this.parent)
            .append(this.hash())
            .build();
    }

    protected abstract boolean equals(T another);

    protected abstract int compare(T another);

    protected abstract int hash();

    @Override
    public abstract T clone();

    public final T getParent() {
        return parent;
    }

}
