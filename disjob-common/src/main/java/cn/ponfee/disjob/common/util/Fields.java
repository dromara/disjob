/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.tuple.Tuple2;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 高效的反射工具类（基于sun.misc.Unsafe）
 *
 * @author Ponfee
 */
@SuppressWarnings("restriction")
public final class Fields {

    // sun.misc.Unsafe.getUnsafe() will be throws "java.lang.SecurityException: Unsafe"
    // caller code must use in BootstrapClassLoader to load (JAVA_HOME/jre/lib)
    // but application code load by sun.misc.Launcher.AppClassLoader
    private static final Unsafe UNSAFE;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null); // If the underlying field is a static field,
                                           // the {@code obj} argument is ignored; it may be null.
                                           // Set static field's value {@code f.set(null, value)}

            // reset accessible value
            f.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("failed to get unsafe instance", e);
        }
    }

    private static final long BASE_OFFSET = UNSAFE.arrayBaseOffset(Object[].class); // 16
    private static final int  INDEX_SCALE = UNSAFE.arrayIndexScale(Object[].class); // 4
    private static final int ADDRESS_SIZE = UNSAFE.addressSize();                   // 8(64 bit cpu)

    public static long addressOf(Object obj) {
        return addressOf(new Object[]{obj}, 0);
    }

    /**
     * Returns the object reference pointer address of jvm
     *
     * @param array the obj array
     * @param index the array position
     * @return reference pointer address
     */
    public static long addressOf(Object[] array, int index) {
        switch (INDEX_SCALE) {
            case 4:
                return (UNSAFE.getInt(array, BASE_OFFSET + (long) index * INDEX_SCALE) & 0xFFFFFFFFL) * ADDRESS_SIZE;
            case 8:
                return UNSAFE.getLong(array, BASE_OFFSET + (long) index * INDEX_SCALE) * ADDRESS_SIZE;
            default:
                throw new Error("Unsupported address size: " + INDEX_SCALE);
        }
    }

    /**
     * put field to target object
     * @param target 目标对象
     * @param name 字段名
     * @param value 字段值
     */
    public static void put(Object target, String name, Object value) {
        try {
            Tuple2<?, Field> tuple = getField(target, name);
            put(tuple.a, tuple.b, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * put field to target object if value is null
     * @param target 目标对象
     * @param name 字段名
     * @param value 字段值
     */
    public static void putIfNull(Object target, String name, Object value) {
        try {
            Tuple2<?, Field> tuple = getField(target, name);
            putIfNull(tuple.a, tuple.b, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * put field to target object if value is null
     * @param target
     * @param field
     * @param value
     */
    public static void putIfNull(Object target, Field field, Object value) {
        if (get(target, field) == null) {
            put(target, field, value);
        }
    }

    /**
     * put field to target object
     * @param target target object
     * @param field object field
     * @param value field value
     */
    public static void put(Object target, Field field, Object value) {
        long fieldOffset = getFieldOffset(field);

        Class<?> type = GenericUtils.getFieldActualType(target.getClass(), field);
        if (Boolean.TYPE.equals(type)) {
            UNSAFE.putBoolean(target, fieldOffset, (boolean) value);
        } else if (Byte.TYPE.equals(type)) {
            UNSAFE.putByte(target, fieldOffset, (byte) value);
        } else if (Character.TYPE.equals(type)) {
            UNSAFE.putChar(target, fieldOffset, (char) value);
        } else if (Short.TYPE.equals(type)) {
            UNSAFE.putShort(target, fieldOffset, (short) value);
        } else if (Integer.TYPE.equals(type)) {
            UNSAFE.putInt(target, fieldOffset, (int) value);
        } else if (Long.TYPE.equals(type)) {
            UNSAFE.putLong(target, fieldOffset, (long) value);
        } else if (Double.TYPE.equals(type)) {
            UNSAFE.putDouble(target, fieldOffset, (double) value);
        } else if (Float.TYPE.equals(type)) {
            UNSAFE.putFloat(target, fieldOffset, (float) value);
        } else {
            UNSAFE.putObject(target, fieldOffset, value);
        }
    }

    /**
     * get field of target object
     * @param target 目标对象
     * @param name field name
     * @return the field value
     */
    public static Object get(Object target, String name) {
        try {
            Tuple2<?, Field> tuple = getField(target, name);
            return get(tuple.a, tuple.b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get field of target object
     * @param target 目标对象
     * @param field  字段
     * @return
     */
    public static Object get(Object target, Field field) {
        long fieldOffset = getFieldOffset(field);
        Class<?> type = GenericUtils.getFieldActualType(target.getClass(), field);
        if (Boolean.TYPE.equals(type)) {
            return UNSAFE.getBoolean(target, fieldOffset);
        } else if (Byte.TYPE.equals(type)) {
            return UNSAFE.getByte(target, fieldOffset);
        } else if (Character.TYPE.equals(type)) {
            return UNSAFE.getChar(target, fieldOffset);
        } else if (Short.TYPE.equals(type)) {
            return UNSAFE.getShort(target, fieldOffset);
        } else if (Integer.TYPE.equals(type)) {
            return UNSAFE.getInt(target, fieldOffset);
        } else if (Long.TYPE.equals(type)) {
            return UNSAFE.getLong(target, fieldOffset);
        } else if (Double.TYPE.equals(type)) {
            return UNSAFE.getDouble(target, fieldOffset);
        } else if (Float.TYPE.equals(type)) {
            return UNSAFE.getFloat(target, fieldOffset);
        } else {
            return UNSAFE.getObject(target, fieldOffset);
        }
    }

    /**
     * put of volatile
     * @param target
     * @param field
     * @param value
     */
    public static void putVolatile(Object target, Field field, Object value) {
        long fieldOffset = getFieldOffset(field);

        Class<?> type = GenericUtils.getFieldActualType(target.getClass(), field);
        if (Boolean.TYPE.equals(type)) {
            UNSAFE.putBooleanVolatile(target, fieldOffset, (boolean) value);
        } else if (Byte.TYPE.equals(type)) {
            UNSAFE.putByteVolatile(target, fieldOffset, (byte) value);
        } else if (Character.TYPE.equals(type)) {
            UNSAFE.putCharVolatile(target, fieldOffset, (char) value);
        } else if (Short.TYPE.equals(type)) {
            UNSAFE.putShortVolatile(target, fieldOffset, (short) value);
        } else if (Integer.TYPE.equals(type)) {
            UNSAFE.putIntVolatile(target, fieldOffset, (int) value);
        } else if (Long.TYPE.equals(type)) {
            UNSAFE.putLongVolatile(target, fieldOffset, (long) value);
        } else if (Double.TYPE.equals(type)) {
            UNSAFE.putDoubleVolatile(target, fieldOffset, (double) value);
        } else if (Float.TYPE.equals(type)) {
            UNSAFE.putFloatVolatile(target, fieldOffset, (float) value);
        } else {
            UNSAFE.putObjectVolatile(target, fieldOffset, value);
        }
    }

    /**
     * 支持volatile语义
     * @param target
     * @param name
     * @return
     */
    public static Object getVolatile(Object target, String name) {
        try {
            Tuple2<?, Field> tuple = getField(target, name);
            return getVolatile(tuple.a, tuple.b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 支持volatile语义
     * @param target
     * @param field
     * @return
     */
    public static Object getVolatile(Object target, Field field) {
        long fieldOffset = getFieldOffset(field);
        Class<?> type = GenericUtils.getFieldActualType(target.getClass(), field);
        if (Boolean.TYPE.equals(type)) {
            return UNSAFE.getBooleanVolatile(target, fieldOffset);
        } else if (Byte.TYPE.equals(type)) {
            return UNSAFE.getByteVolatile(target, fieldOffset);
        } else if (Character.TYPE.equals(type)) {
            return UNSAFE.getCharVolatile(target, fieldOffset);
        } else if (Short.TYPE.equals(type)) {
            return UNSAFE.getShortVolatile(target, fieldOffset);
        } else if (Integer.TYPE.equals(type)) {
            return UNSAFE.getIntVolatile(target, fieldOffset);
        } else if (Long.TYPE.equals(type)) {
            return UNSAFE.getLongVolatile(target, fieldOffset);
        } else if (Double.TYPE.equals(type)) {
            return UNSAFE.getDoubleVolatile(target, fieldOffset);
        } else if (Float.TYPE.equals(type)) {
            return UNSAFE.getFloatVolatile(target, fieldOffset);
        } else {
            return UNSAFE.getObjectVolatile(target, fieldOffset);
        }
    }

    // ----------------------------------------------------------------private methods
    private static Tuple2<?, Field> getField(Object obj, String name) {
        Tuple2<Class<?>, Predicates> tuple = ClassUtils.obtainClass(obj);
        if (tuple.b.state()) {
            // static
            // field.get(null);
            // field.set(null, value); 使用field设置final属性会报错，只能使用Unsafe
            return ClassUtils.getStaticFieldInClassChain(tuple.a, name);
            //return Tuple2.of(obj, ClassUtils.getStaticField(tuple.a, name));
        } else {
            // member
            return Tuple2.of(obj, ClassUtils.getField(tuple.a, name));
        }
    }

    private static long getFieldOffset(Field field) {
        return Modifier.isStatic(field.getModifiers())
             ? UNSAFE.staticFieldOffset(field)
             : UNSAFE.objectFieldOffset(field);
    }

}
