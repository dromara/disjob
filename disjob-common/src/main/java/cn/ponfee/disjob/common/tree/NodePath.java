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

import cn.ponfee.disjob.common.collect.ImmutableArrayList;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;


/**
 * Representing immutable node path array
 *
 * @param <T> the node id type
 * @author Ponfee
 */
@JsonDeserialize(using = NodePath.JacksonDeserializer.class)
public final class NodePath<T extends Serializable & Comparable<T>>
    extends ImmutableArrayList<T> implements Comparable<NodePath<T>> {

    private static final long serialVersionUID = 9090552044337950223L;

    public NodePath() {
        // Note: For help deserialization(jackson)
    }

    @SafeVarargs
    public NodePath(T... path) {
        super(path);
    }

    @SuppressWarnings("unchecked")
    public NodePath(T[] parent, T child) {
        super(ArrayUtils.addAll(Objects.requireNonNull(parent), child));
    }

    public NodePath(List<T> path) {
        super(path.toArray());
    }

    public NodePath(List<T> path, T child) {
        super(ArrayUtils.addAll(path.toArray(), child));
    }

    public NodePath(NodePath<T> parent) {
        super(parent.toArray());
    }

    public NodePath(NodePath<T> parent, T child) {
        super(parent.join(child));
    }

    @Override
    public int compareTo(NodePath<T> o) {
        int c;
        for (Iterator<T> a = this.iterator(), b = o.iterator(); a.hasNext() && b.hasNext(); ) {
            if ((c = a.next().compareTo(b.next())) != 0) {
                return c;
            }
        }
        return super.size() - o.size();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof NodePath) && super.equals(obj);
    }

    @Override
    public NodePath<T> clone() {
        return new NodePath<>(this);
    }

    // --------------------------------------------------------custom jackson deserialize

    public static class JacksonDeserializer<T extends Serializable & Comparable<T>> extends JsonDeserializer<NodePath<T>> {
        @Override
        @SuppressWarnings("unchecked")
        public NodePath<T> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            List<T> list = p.readValueAs(List.class);
            return CollectionUtils.isEmpty(list) ? null : new NodePath<>(list);
        }
    }

}
