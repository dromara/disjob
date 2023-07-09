/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.tree;

import java.io.Serializable;

/**
 * Representing plain node
 *
 * @param <T> the node id type
 * @param <A> the attachment biz object type
 * @author Ponfee
 */
public final class PlainNode<T extends Serializable & Comparable<T>, A> extends BaseNode<T, A> {
    private static final long serialVersionUID = -2189191471047483877L;

    public PlainNode(T nid, T pid, A attach) {
        super(nid, pid, attach);
    }

    public PlainNode(T nid, T pid, boolean enabled, A attach) {
        super(nid, pid, enabled, attach);
    }

    public PlainNode(T nid, T pid, boolean enabled, boolean available, A attach) {
        super(nid, pid, enabled, available, attach);
    }

}
