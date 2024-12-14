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
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The map for Tree node
 *
 * @param <T> the node id type
 * @param <A> the attachment biz object type
 * @author Ponfee
 */
public class MapTreeTrait<T extends Serializable & Comparable<T>, A>
    extends LinkedHashMap<String, Object> implements TreeTrait<T, A, MapTreeTrait<T, A>> {

    private static final long serialVersionUID = -5799393887664198242L;
    public static final String DEFAULT_CHILDREN_KEY = "children";

    private final String childrenKey;

    public MapTreeTrait() {
        this(DEFAULT_CHILDREN_KEY);
    }

    public MapTreeTrait(String childrenKey) {
        this.childrenKey = childrenKey;
    }

    @Override
    public void setChildren(List<MapTreeTrait<T, A>> children) {
        super.put(childrenKey, children);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MapTreeTrait<T, A>> getChildren() {
        return (List<MapTreeTrait<T, A>>) super.get(childrenKey);
    }

}
