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

package cn.ponfee.disjob.common.tree;

import cn.ponfee.disjob.common.base.ToJsonString;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Objects;

/**
 * Base node id
 *
 * @param <T> the NodeId implementation sub class
 * @author Ponfee
 */
public abstract class NodeId<T extends NodeId<T>> extends ToJsonString implements Comparable<T>, Serializable, Cloneable {

    private static final long serialVersionUID = -9004940918491918780L;

    protected final T parent;

    protected NodeId(T parent) {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    @Override
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
        return new HashCodeBuilder().append(parent).append(hash()).build();
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
