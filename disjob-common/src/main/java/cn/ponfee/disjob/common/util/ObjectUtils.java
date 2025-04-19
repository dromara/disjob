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
import cn.ponfee.disjob.common.collect.TypedDictionary;
import cn.ponfee.disjob.common.date.JavaUtilDateFormat;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Object utility class
 *
 * @author Ponfee
 */
public final class ObjectUtils {

    /**
     * 判断对象是否为空
     *
     * @param o the object
     * @return {@code true} is empty
     */
    public static boolean isEmpty(Object o) {
        if (o == null) {
            return true;
        }
        if (o.getClass().isArray()) {
            return Array.getLength(o) == 0;
        }
        if (o instanceof CharSequence) {
            return ((CharSequence) o).length() == 0;
        }
        if (o instanceof Collection) {
            return ((Collection<?>) o).isEmpty();
        }
        if (o instanceof Map) {
            return ((Map<?, ?>) o).isEmpty();
        }
        if (o instanceof Dictionary) {
            return ((Dictionary<?, ?>) o).isEmpty();
        }
        return false;
    }

    /**
     * Gets the target's name value
     *
     * @param obj  the object
     * @param name the field name
     * @return a value
     */
    public static Object getValue(Object obj, String name) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).get(name);
        }
        if (obj instanceof Dictionary) {
            return ((Dictionary<?, ?>) obj).get(name);
        }
        if (obj instanceof TypedDictionary) {
            return ((TypedDictionary<?, ?>) obj).get(name);
        }
        try {
            return FieldUtils.readField(obj, name, true);
        } catch (IllegalAccessException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Returns target type value from origin value cast
     * <p> See com.alibaba.fastjson.util.TypeUtils#castToInt(Object)
     *
     * @param value source object
     * @param type  target object type
     * @return target type object
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T cast(Object value, Class<T> type) {
        if (type.isInstance(value)) {
            return (T) value;
        }

        PrimitiveOrWrapperConvertors convertor = PrimitiveOrWrapperConvertors.of(type);
        if (convertor != null) {
            return convertor.to(value);
        }

        if (value == null) {
            return null;
        }

        if (type.isArray()) {
            Object[] array = Collects.newArray(type, 1);
            array[0] = cast(value, type.getComponentType());
            return (T) array;
        }

        if (type.isEnum()) {
            return (value instanceof Number) ?
                type.getEnumConstants()[((Number) value).intValue()] :
                (T) EnumUtils.getEnumIgnoreCase((Class<Enum>) type, value.toString());
        }

        if (Date.class == type) {
            if (value instanceof Number) {
                return (T) new Date(((Number) value).longValue());
            }
            String text = value.toString();
            if (StringUtils.isNumeric(text) && !isDatePattern(text)) {
                return (T) new Date(Numbers.toLong(text));
            }
            try {
                return (T) JavaUtilDateFormat.DEFAULT.parse(text);
            } catch (ParseException e) {
                return ExceptionUtils.rethrow(e);
            }
        }

        return ClassUtils.newInstance(type, value);
    }

    public static <T> void applyIfNotNull(T obj, Consumer<T> consumer) {
        if (obj != null) {
            consumer.accept(obj);
        }
    }

    // -------------------------------------------------------------------------- private class

    /**
     * yyyyMMdd(HHmmss(SSS))
     */
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "^([1-9]\\d{3}((0[1-9]|1[012])(0[1-9]|1\\d|2[0-8])|(0[13456789]|1[012])(29|30)|(0[13578]|1[02])31)|(([2-9]\\d)(0[48]|[2468][048]|[13579][26])|(([2468][048]|[3579][26])00))0229)(([0-1][0-9]|2[0-3])([0-5][0-9])([0-5][0-9])(\\d{3})?)?$"
    );

    /**
     * Validates the text whether date pattern
     *
     * @param text the string
     * @return if returns {@code true} then is a valid date pattern
     */
    private static boolean isDatePattern(String text) {
        return text != null && DATE_PATTERN.matcher(text).matches();
    }

    @SuppressWarnings("all")
    private enum PrimitiveOrWrapperConvertors {

        BOOLEAN(boolean.class) {
            @Override
            public Boolean to(Object value) {
                return Numbers.toBoolean(value);
            }
        },

        WRAP_BOOLEAN(Boolean.class) {
            @Override
            public Boolean to(Object value) {
                return Numbers.toWrapBoolean(value);
            }
        },

        BYTE(byte.class) {
            @Override
            public Byte to(Object value) {
                return Numbers.toByte(value);
            }
        },

        WRAP_BYTE(Byte.class) {
            @Override
            public Byte to(Object value) {
                return Numbers.toWrapByte(value);
            }
        },

        SHORT(short.class) {
            @Override
            public Short to(Object value) {
                return Numbers.toShort(value);
            }
        },

        WRAP_SHORT(Short.class) {
            @Override
            public Short to(Object value) {
                return Numbers.toWrapShort(value);
            }
        },

        CHAR(char.class) {
            @Override
            public Character to(Object value) {
                return Numbers.toChar(value);
            }
        },

        WRAP_CHAR(Character.class) {
            @Override
            public Character to(Object value) {
                return Numbers.toWrapChar(value);
            }
        },

        INT(int.class) {
            @Override
            public Integer to(Object value) {
                return Numbers.toInt(value);
            }
        },

        WRAP_INT(Integer.class) {
            @Override
            public Integer to(Object value) {
                return Numbers.toWrapInt(value);
            }
        },

        LONG(long.class) {
            @Override
            public Long to(Object value) {
                return Numbers.toLong(value);
            }
        },

        WRAP_LONG(Long.class) {
            @Override
            public Long to(Object value) {
                return Numbers.toWrapLong(value);
            }
        },

        FLOAT(float.class) {
            @Override
            public Float to(Object value) {
                return Numbers.toFloat(value);
            }
        },

        WRAP_FLOAT(Float.class) {
            @Override
            public Float to(Object value) {
                return Numbers.toWrapFloat(value);
            }
        },

        DOUBLE(double.class) {
            @Override
            public Double to(Object value) {
                return Numbers.toDouble(value);
            }
        },

        WRAP_DOUBLE(Double.class) {
            @Override
            public Double to(Object value) {
                return Numbers.toWrapDouble(value);
            }
        };

        static final Map<Class<?>, PrimitiveOrWrapperConvertors> MAPPING =
            Enums.toMap(PrimitiveOrWrapperConvertors.class, PrimitiveOrWrapperConvertors::type);

        final Class<?> type;

        PrimitiveOrWrapperConvertors(Class<?> type) {
            this.type = type;
        }

        abstract <T> T to(Object value);

        Class<?> type() {
            return this.type;
        }

        static PrimitiveOrWrapperConvertors of(Class<?> targetType) {
            return MAPPING.get(targetType);
        }
    }

}
