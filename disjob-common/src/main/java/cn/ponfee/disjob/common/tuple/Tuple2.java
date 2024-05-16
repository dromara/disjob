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

import java.util.Map;

/**
 * Tuple2 consisting of two elements.
 *
 * @author Ponfee
 */
public final class Tuple2<A, B> extends Tuple implements Map.Entry<A, B> {
    private static final long serialVersionUID = -3627925720098458172L;

    public A a;
    public B b;

    public Tuple2(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public static <A, B> Tuple2<A, B> of(A a, B b) {
        return new Tuple2<>(a, b);
    }

    @Override
    public <T> T get(int index) {
        switch (index) {
            case  0: return (T) a;
            case  1: return (T) b;
            default: throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public <T> void set(T value, int index) {
        switch (index) {
            case  0: a = (A) value; break;
            case  1: b = (B) value; break;
            default: throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public int length() {
        return 2;
    }

    @Override
    public Tuple2<A, B> copy() {
        return new Tuple2<>(a, b);
    }

    /**
     * Returns a Tuple2 Object of this instance swapped values.
     *
     * @return a Tuple2 Object of this instance swapped values
     */
    public Tuple2<B, A> swap() {
        return new Tuple2<>(b, a);
    }

    @Override
    public A getKey() {
        return a;
    }

    @Override
    public B getValue() {
        return b;
    }

    @Override
    public B setValue(B value) {
        B result = b;
        this.b = value;
        return result;
    }

}
