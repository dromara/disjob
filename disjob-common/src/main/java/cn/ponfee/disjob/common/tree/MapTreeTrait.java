/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
public class MapTreeTrait<T extends Serializable & Comparable<? super T>, A>
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

    @Override
    public List<MapTreeTrait<T, A>> getChildren() {
        return (List<MapTreeTrait<T, A>>) super.get(childrenKey);
    }

}
