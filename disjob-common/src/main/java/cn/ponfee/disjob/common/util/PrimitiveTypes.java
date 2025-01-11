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

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.collect.Collects;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * 基本数据类型及其包装类型工具类(不包含 {@link Void})，以及这些数据类型间的转换规则
 *
 * +----------+-------+------+-----+-------+------+------+---------+
 * |  double  | float | long | int | short | char | byte | boolean |
 * +----------+-------+------+-----+-------+------+------+---------+
 * </pre>
 *
 * @author Ponfee
 */
@SuppressWarnings("all")
public enum PrimitiveTypes {

    DOUBLE (Double.class   , (byte) 0B1_0_0_0_0_0_0_0, (byte) 0B1_0_0_0_0_0_0_0   ),
    FLOAT  (Float.class    , (byte) 0B0_1_0_0_0_0_0_0, (byte) 0B1_1_0_0_0_0_0_0   ),
    LONG   (Long.class     , (byte) 0B0_0_1_0_0_0_0_0, (byte) 0B1_1_1_0_0_0_0_0   ),
    INT    (Integer.class  , (byte) 0B0_0_0_1_0_0_0_0, (byte) 0B1_1_1_1_0_0_0_0   ),
    SHORT  (Short.class    , (byte) 0B0_0_0_0_1_0_0_0, (byte) 0B1_1_1_1_1_0_0_0   ), // short与char不能互相自动转换
    CHAR   (Character.class, (byte) 0B0_0_0_0_0_1_0_0, (byte) 0B1_1_1_1_0_1_0_0   ), // short与char不能互相自动转换
    BYTE   (Byte.class     , (byte) 0B0_0_0_0_0_0_1_0, (byte) 0B1_1_1_1_1_0_1_0   ), // byte不能自动转为char
    BOOLEAN(Boolean.class  , (byte) 0B0_0_0_0_0_0_0_1, (byte) 0B0_0_0_0_0_0_0_1, 1), // boolean只能转boolean

    ;

    private final Class<?> wrapper;
    private final byte value;    // 类型值
    private final byte castable; // 支持转换到的目标类型
    private final Class<?> primitive;
    private final int size;

    PrimitiveTypes(Class<?> wrapper, byte value, byte castable) {
        this(wrapper, value, castable, (int) readStaticField(wrapper, "SIZE"));
    }

    PrimitiveTypes(Class<?> wrapper, byte value, byte castable, int size) {
        this.wrapper = wrapper;
        this.value = value;
        this.castable = castable;
        this.primitive = (Class<?>) readStaticField(wrapper, "TYPE");
        this.size = size;
    }

    public Class<?> primitive() {
        return primitive;
    }

    public Class<?> wrapper() {
        return wrapper;
    }

    public int size() {
        return size;
    }

    /**
     * 用于判断传入方法真实的参数类型(this)是否能转换到方法定义的参数类型(target)
     *
     * @param target 目标参数类型
     * @return {@code true}是，{@code false}否
     */
    public boolean isCastable(PrimitiveTypes target) {
        return (this.castable & target.value) == target.value;
    }

    public static PrimitiveTypes ofPrimitive(Class<?> primitive) {
        return Const.PRIMITIVE_MAPPING.get(primitive);
    }

    public static PrimitiveTypes ofWrapper(Class<?> wrapper) {
        return Const.WRAPPER_MAPPING.get(wrapper);
    }

    public static PrimitiveTypes ofPrimitiveOrWrapper(Class<?> primitive) {
        return Const.ALL_MAPPING.get(primitive);
    }

    public static Set<Class<?>> allPrimitiveTypes() {
        return Const.PRIMITIVE_MAPPING.keySet();
    }

    public static Set<Class<?>> allWrapperTypes() {
        return Const.WRAPPER_MAPPING.keySet();
    }

    public static boolean isWrapperType(Class<?> primitive) {
        return ofWrapper(primitive) != null;
    }

    public static <T> Class<T> wrap(Class<T> type) {
        PrimitiveTypes pt = ofPrimitiveOrWrapper(type);
        return pt == null ? type : (Class<T>) pt.wrapper;
    }

    public static <T> Class<T> unwrap(Class<T> type) {
        PrimitiveTypes pt = ofPrimitiveOrWrapper(type);
        return pt == null ? type : (Class<T>) pt.primitive;
    }

    // -------------------------------------------------------private methods

    private static <T> T readStaticField(Class<?> cls, String fieldName) {
        try {
            return (T) FieldUtils.readStaticField(cls, fieldName, true);
        } catch (IllegalAccessException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    private static final class Const {
        private static final Map<Class<?>, PrimitiveTypes> PRIMITIVE_MAPPING = Enums.toMap(PrimitiveTypes.class, PrimitiveTypes::primitive);
        private static final Map<Class<?>, PrimitiveTypes> WRAPPER_MAPPING = Enums.toMap(PrimitiveTypes.class, PrimitiveTypes::wrapper);
        private static final Map<Class<?>, PrimitiveTypes> ALL_MAPPING = Collects.concat(PRIMITIVE_MAPPING, WRAPPER_MAPPING);
    }

}
