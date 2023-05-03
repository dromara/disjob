/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
public interface TreeTrait<T extends Serializable & Comparable<? super T>, A, E extends TreeTrait<T, A, E>> {

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
