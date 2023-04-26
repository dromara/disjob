/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.tree;

import cn.ponfee.scheduler.common.base.ImmutableArrayList;
import cn.ponfee.scheduler.common.util.GenericUtils;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Representing immutable node path array
 *
 * @param <T> the node id type
 * @author Ponfee
 */
// NodePath is extends ArrayList, so must be use mappingTo in fastjson
// if not do it then deserialized json as a collection type(java.util.ArrayList)
// hashCode()/equals() extends ImmutableArrayList
@JSONType(mappingTo = NodePath.FastjsonDeserializeMarker.class) // fastjson
@JsonDeserialize(using = NodePath.JacksonDeserializer.class)    // jackson
public final class NodePath<T extends Serializable & Comparable<? super T>>
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

    // -----------------------------------------------------custom fastjson deserialize

    @JSONType(deserializer = FastjsonDeserializer.class)
    public static class FastjsonDeserializeMarker { }

    /**
     * <pre> {@code
     *   public static class IntegerNodePath {
     *     // 当定义的NodePath字段其泛型参数为具体类型时，必须用JSONField注解，否则报错
     *     @JSONField(deserializeUsing = FastjsonDeserializer.class)
     *     private NodePath<Integer> path; // ** NodePath<Integer> **
     *   }
     * }</pre>
     *
     * @param <T>
     */
    public static class FastjsonDeserializer<T extends Serializable & Comparable<? super T>> implements ObjectDeserializer {
        @Override
        @SuppressWarnings("unchecked")
        public NodePath<T> deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
            if (GenericUtils.getRawType(type) != NodePath.class) {
                throw new UnsupportedOperationException("Only supported deserialize NodePath, cannot supported: " + type);
            }
            List<T> list = parser.parseArray(GenericUtils.getActualTypeArgument(type, 0));
            return list.isEmpty() ? null : new NodePath<>(list);
        }

        @Override
        public int getFastMatchToken() {
            return 0 /*JSONToken.RBRACKET*/;
        }
    }

    // -----------------------------------------------------custom jackson deserialize

    public static class JacksonDeserializer<T extends Serializable & Comparable<? super T>> extends JsonDeserializer<NodePath<T>> {
        @Override
        @SuppressWarnings("unchecked")
        public NodePath<T> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            List<T> list = p.readValueAs(List.class);
            return CollectionUtils.isEmpty(list) ? null : new NodePath<>(list);
        }
    }

}
