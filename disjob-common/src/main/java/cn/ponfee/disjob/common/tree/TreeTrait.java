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
import java.util.List;

/**
 * The trait for Tree node
 *
 * @param <T> the node id type
 * @param <A> the attachment biz object type
 * @param <E> the TreeTrait type
 * @author Ponfee
 */
public interface TreeTrait<T extends Serializable & Comparable<T>, A, E extends TreeTrait<T, A, E>> {

    /**
     * Sets node list as children
     *
     * @param children the children node list
     */
    void setChildren(List<E> children);

    /**
     * Gets children node list
     *
     * @return children node list
     */
    List<E> getChildren();
}
