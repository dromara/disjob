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

import lombok.Getter;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Objects;

/**
 * Representing plain node
 *
 * @param <T> the node id type
 * @param <A> the attachment biz object type
 * @author Ponfee
 */
@Getter
public class PlainNode<T extends Serializable & Comparable<T>, A> implements Serializable, Cloneable {
    private static final long serialVersionUID = -4116799955526185765L;

    // -------------------------------------------------------------------basic information

    protected final T id;              // current node id
    protected final T parentId;        // parent node id
    protected final boolean enabled;   // 状态（业务相关）：false无效；true有效；
    protected final boolean available; // 是否可用（parent.available && this.available && this.enabled）
    protected final A attach;          // 附加信息（与业务相关）

    // -------------------------------------------------------------------Constructor

    public PlainNode(T id, T parentId) {
        this(id, parentId, true, true, null);
    }

    /**
     * Constructs plain node
     *
     * @param id        the current node id
     * @param parentId  the parent node id
     * @param enabled   the node is enabled
     * @param available the current node is available(parent.available && this.available && this.enabled)
     * @param attach    the attachment of biz object
     */
    public PlainNode(T id, T parentId, boolean enabled, boolean available, A attach) {
        checkNodeId(id, parentId);
        this.id = id;
        this.parentId = parentId;
        this.enabled = enabled;
        this.available = enabled && available;
        this.attach = attach;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.id, ((PlainNode<T, A>) obj).id);
    }

    /**
     * Deep copy
     *
     * @return a copies of node
     */
    @Override
    public PlainNode<T, A> clone() {
        return SerializationUtils.clone(this);
    }

    public boolean equalsId(T id) {
        return Objects.equals(this.id, id);
    }

    private static <T extends Serializable & Comparable<T>> void checkNodeId(T id, T parentId) {
        if (id == null) {
            Assert.isNull(parentId, "Parent id must be null when node id is null.");
        } else {
            Assert.isTrue(!id.equals(parentId), "Node id cannot equals parent id.");
        }
        if (parentId != null) {
            Assert.notNull(id, "Node id cannot be null when parent id is not null.");
        }
    }

}
