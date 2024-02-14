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

package cn.ponfee.disjob.common.tuple;

import java.util.*;

/**
 * Tuple0 consisting of empty element.
 *
 * @author Ponfee
 */
public final class Tuple0 extends Tuple {
    private static final long serialVersionUID = -3627925720098458172L;
    private static final Tuple0 INSTANCE = new Tuple0();

    public Tuple0() {
    }

    public static Tuple0 of() {
        return INSTANCE;
    }

    @Override
    public <T> T get(int index) {
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Override
    public <T> void set(T value, int index) {
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public Tuple0 copy() {
        return INSTANCE;
    }

    @Override
    public List<Object> toList() {
        return Collections.emptyList();
    }

    @Override
    public Iterator<Object> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public Spliterator<Object> spliterator() {
        return Spliterators.emptySpliterator();
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
