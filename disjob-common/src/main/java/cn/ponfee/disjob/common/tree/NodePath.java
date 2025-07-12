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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Representing tree node path
 *
 * @param <T> the node id type
 * @author Ponfee
 */
public final class NodePath<T extends Serializable & Comparable<T>>
    extends ArrayList<T> implements Comparable<NodePath<T>> {
    private static final long serialVersionUID = 9090552044337950223L;

    public NodePath() {
        // Note: For help deserialization(jackson)
    }

    public NodePath(List<T> path) {
        super(path.size());
        super.addAll(path);
    }

    public NodePath(NodePath<T> parentPath, T child) {
        super(parentPath.size() + 1);
        super.addAll(parentPath);
        super.add(child);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof NodePath) && super.equals(obj);
    }

    @Override
    public int compareTo(NodePath<T> that) {
        int compared;
        for (Iterator<T> a = this.iterator(), b = that.iterator(); a.hasNext() && b.hasNext(); ) {
            if ((compared = a.next().compareTo(b.next())) != 0) {
                return compared;
            }
        }
        return this.size() - that.size();
    }

    @Override
    public NodePath<T> clone() {
        return new NodePath<>(this);
    }

}
