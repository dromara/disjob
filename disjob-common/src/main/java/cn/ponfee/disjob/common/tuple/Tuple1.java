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

/**
 * Tuple1 consisting of one element.
 *
 * @author Ponfee
 */
public final class Tuple1<A> extends Tuple {
    private static final long serialVersionUID = -3627925720098458172L;

    public A a;

    public Tuple1(A a) {
        this.a = a;
    }

    public static <A> Tuple1<A> of(A a) {
        return new Tuple1<>(a);
    }

    @Override
    public <T> T get(int index) {
        if (index == 0) {
            return (T) a;
        } else {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public <T> void set(T value, int index) {
        if (index == 0) {
            a = (A) value;
        } else {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public Tuple1<A> copy() {
        return new Tuple1<>(a);
    }

}
