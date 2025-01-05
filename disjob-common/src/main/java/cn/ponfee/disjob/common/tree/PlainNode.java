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

/**
 * Representing plain node
 *
 * @param <T> the node id type
 * @param <A> the attachment biz object type
 * @author Ponfee
 */
public final class PlainNode<T extends Serializable & Comparable<T>, A> extends BaseNode<T, A> {
    private static final long serialVersionUID = -2189191471047483877L;

    public PlainNode(T nid, T pid) {
        super(nid, pid);
    }

    public PlainNode(T nid, T pid, boolean enabled, boolean available, A attach) {
        super(nid, pid, enabled, available, attach);
    }

}
