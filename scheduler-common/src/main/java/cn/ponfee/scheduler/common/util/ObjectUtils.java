/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.util;

import cn.ponfee.scheduler.common.date.JavaUtilDateFormat;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

/**
 * Object utility class
 *
 * @author Ponfee
 */
public final class ObjectUtils {

    /**
     * Returns object toString
     *
     * @param obj the target object
     * @return the string of object
     */
    public static String toString(Object obj) {
        return toString(obj, "null");
    }

    public static String toString(Object obj, String defaultStr) {
        return (obj == null)
             ? defaultStr
             : reflectionToString(obj, ToStringStyle.JSON_STYLE);
    }

    public static <T> T defaultIfNull(T object, Supplier<T> defaultValue) {
        return object != null ? object : defaultValue.get();
    }

    /**
     * Compare two object numerically
     *
     * @param a the object a
     * @param b the object b
     * @return 0(a==b), 1(a>b), -1(a<b)
     */
    public static int compare(Object a, Object b) {
        if (a == b) {
            return Comparators.EQ;
        }
        if (a == null) {
            // null last
            return Comparators.GT;
        }
        if (b == null) {
            // null last
            return Comparators.LT;
        }

        if ((a instanceof Comparable) && (b instanceof Comparable)) {
            if (a.getClass().isInstance(b)) {
                return ((Comparable) a).compareTo(b);
            } else if (b.getClass().isInstance(a)) {
                return ((Comparable) b).compareTo(a);
            }
        }

        // Fields.addressOf
        int res = Integer.compare(System.identityHashCode(a.getClass()), System.identityHashCode(b.getClass()));
        return res != Comparators.EQ ? res : Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
    }

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
        if (o instanceof CharSequence) {
            return ((CharSequence) o).length() == 0;
        }
        if (o instanceof Collection) {
            return ((Collection<?>) o).isEmpty();
        }
        if (o.getClass().isArray()) {
            return Array.getLength(o) == 0;
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
        } else if (obj instanceof Map) {
            return ((Map<?, ?>) obj).get(name);
        } else if (obj instanceof Dictionary) {
            return ((Dictionary<?, ?>) obj).get(name);
        } else {
            return Fields.get(obj, name);
        }
    }

    /**
     * Returns target type value from origin value cast
     *
     * @param value source object
     * @param type  target object type
     * @return target type object
     * @see com.alibaba.fastjson.util.TypeUtils#castToInt(Object)
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

        if (type.isEnum()) {
            return (value instanceof Number)
                 ? type.getEnumConstants()[((Number) value).intValue()]
                 : (T) EnumUtils.getEnumIgnoreCase((Class<Enum>) type, value.toString());
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
                throw new RuntimeException(e);
            }
        }

        return ClassUtils.newInstance(type, new Object[]{value.toString()});
    }

    /**
     * uuid byte array
     *
     * @return
     */
    public static byte[] uuid() {
        UUID uuid = UUID.randomUUID();
        long most = uuid.getMostSignificantBits(),
            least = uuid.getLeastSignificantBits();
        return new byte[]{
            (byte) (most >>> 56), (byte) (most >>> 48),
            (byte) (most >>> 40), (byte) (most >>> 32),
            (byte) (most >>> 24), (byte) (most >>> 16),
            (byte) (most >>>  8), (byte) (most       ),

            (byte) (least >>> 56), (byte) (least >>> 48),
            (byte) (least >>> 40), (byte) (least >>> 32),
            (byte) (least >>> 24), (byte) (least >>> 16),
            (byte) (least >>>  8), (byte) (least       )
        };
    }

    /**
     * uuid 32 string
     *
     * @return 32 length uuid
     */
    public static String uuid32() {
        UUID uuid = UUID.randomUUID();
        return Bytes.toHex(uuid.getMostSignificantBits(), true)
             + Bytes.toHex(uuid.getLeastSignificantBits(), true);
    }

    /**
     * 22位uuid
     *
     * @return 22 length uuid
     */
    public static String uuid22() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uuid());
    }

    /**
     * 获取堆栈信息
     *
     * @param deepPath
     * @return
     */
    public static String getStackTrace(int deepPath) {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        if (traces.length <= deepPath) {
            return "warning: out of stack trace.";
        }
        return traces[deepPath].toString();
    }

    public static String getStackTrace() {
        return buildStackTrace(Thread.currentThread().getStackTrace());
    }

    public static String getStackTrace(Thread thread) {
        return buildStackTrace(thread.getStackTrace());
    }

    private static String buildStackTrace(StackTraceElement[] traces) {
        StringBuilder builder = new StringBuilder();
        for (int i = 2, n = traces.length; i < n; i++) {
            builder.append("--\t").append(traces[i].toString()).append("\n");
        }
        return builder.toString();
    }

    /**
     * Returns a new instance of type
     *
     * @param type the type class
     * @return a new instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> type) {
        if (Map.class == type) { // interface
            return (T) new HashMap<>(8);
        } else if (Set.class == type) { // interface
            return (T) new HashSet<>();
        } else if (Collection.class == type || List.class == type) { // interface
            return (T) new ArrayList<>();
        } else if (Dictionary.class == type) { // abstract
            return (T) new Hashtable<>();
        } else if (type.isPrimitive() || PrimitiveTypes.isWrapperType(type)) {
            Class<?> wrapper = PrimitiveTypes.ofPrimitiveOrWrapper(type).wrapper();
            // new Boolean("0") -> false
            return (T) ClassUtils.newInstance(wrapper, new Class<?>[]{String.class}, new Object[]{"0"});
        } else {
            return ClassUtils.newInstance(type);
        }
    }

    /**
     * yyyyMMdd(HHmmss(SSS))
     */
    private static final Pattern PATTERN_DATE = Pattern.compile(
        "^([1-9]\\d{3}((0[1-9]|1[012])(0[1-9]|1\\d|2[0-8])|(0[13456789]|1[012])(29|30)|(0[13578]|1[02])31)|(([2-9]\\d)(0[48]|[2468][048]|[13579][26])|(([2468][048]|[3579][26])00))0229)(([0-1][0-9]|2[0-3])([0-5][0-9])([0-5][0-9])(\\d{3})?)?$"
    );
    /**
     * Validates the text whether date pattern
     *
     * @param text the string
     * @return if returns {@code true} then is a valid date pattern
     */
    private static boolean isDatePattern(String text) {
        return text != null && PATTERN_DATE.matcher(text).matches();
    }

    // -------------------------------------------------------------------------- private class
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

        private static final Map<Class<?>, PrimitiveOrWrapperConvertors> MAPPING =
            Enums.toMap(PrimitiveOrWrapperConvertors.class, PrimitiveOrWrapperConvertors::type);

        private final Class<?> type;

        PrimitiveOrWrapperConvertors(Class<?> type) {
            this.type = type;
        }

        abstract <T> T to(Object value);

        public Class<?> type() {
            return this.type;
        }

        static PrimitiveOrWrapperConvertors of(Class<?> targetType) {
            return MAPPING.get(targetType);
        }
    }

}
