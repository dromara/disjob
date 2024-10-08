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

package cn.ponfee.disjob.common.base;

/**
 * Represents value enum type structure.
 *
 * @param <V> value type
 * @param <T> enum type
 * @author Ponfee
 */
public interface ValueEnum<V, T extends Enum<T> & ValueEnum<V, T>> {

    /**
     * Returns enum value
     *
     * @return enum value
     */
    V value();

    /**
     * Returns enum description
     *
     * @return enum description
     */
    String desc();

    /**
     * Return ValueEnum object of value
     * <p>Enum.values()和EnumClass.getEnumConstants()都会clone数组，效率不高
     *
     * @param type  the ValueEnum class
     * @param value the value
     * @param <V>   value type
     * @param <T>   ValueEnum type
     * @return ValueEnum object
     */
    static <V, T extends Enum<T> & ValueEnum<V, T>> T of(Class<T> type, V value) {
        for (T e : type.getEnumConstants()) {
            if (value.equals(e.value())) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid enum value: " + value);
    }

}
