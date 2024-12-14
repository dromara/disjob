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

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Function;

/**
 * Representing tree node id
 *
 * @param <T> the NodeId implementation subclass
 * @author Ponfee
 */
public abstract class NodeId<T extends NodeId<T>>
    extends ToJsonString implements Comparable<T>, Serializable {
    private static final long serialVersionUID = -9004940918491918780L;

    /**
     * Parent node id, null value if root node
     */
    protected final T parent;

    protected NodeId(T parent) {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !this.getClass().equals(obj.getClass())) {
            return false;
        }
        return Objects.equals(this.parent, ((T) obj).parent);
    }

    @Override
    public int compareTo(@Nonnull T that) {
        if (this.parent == null) {
            // null parent first (root node should be first)
            return that.parent == null ? 0 : -1;
        }
        return that.parent == null ? 1 : this.parent.compareTo(that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parent);
    }

    public final T getParent() {
        return parent;
    }

    @SuppressWarnings("unchecked")
    public <E extends Serializable & Comparable<E>> NodePath<E> toNodePath(Function<T, E> mapper) {
        LinkedList<E> path = new LinkedList<>();
        for (T node = (T) this; node != null; node = node.parent) {
            // [root, parent, child]
            path.addFirst(mapper.apply(node));
        }
        return new NodePath<>(path);
    }

}
