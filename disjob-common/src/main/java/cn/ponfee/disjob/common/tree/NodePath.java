/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
