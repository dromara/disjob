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

import cn.ponfee.disjob.common.base.Symbol.Char;
import cn.ponfee.disjob.common.tuple.Tuple2;
import com.google.common.base.Strings;
import com.google.common.primitives.Chars;
import org.apache.commons.codec.binary.Hex;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * <pre>
 * Number utility
 *
 * 十进制：10
 * 二进制：0B10
 * 八进制：010
 * 十六进制：0X10
 * 小数点：1e-9
 * </pre>
 *
 * @author Ponfee
 */
public final class Numbers {

    private static final Pattern ALL_ZERO_PATTERN = Pattern.compile("^0+$");

    public static final int     ZERO_INT     = 0;
    public static final Integer ZERO_INTEGER = 0;
    public static final byte    ZERO_BYTE    = 0x00;

    // --------------------------------------------------------------character convert

    public static char toChar(Object obj) {
        return toChar(obj, Char.ZERO);
    }

    public static char toChar(Object obj, char defaultVal) {
        Character value = toWrapChar(obj);
        return value == null ? defaultVal : value;
    }

    public static Character toWrapChar(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Character) {
            return (Character) obj;
        } else if (obj instanceof Number) {
            return (char) ((Number) obj).intValue();
        } else if (obj instanceof byte[]) {
            return Chars.fromByteArray((byte[]) obj);
        } else if (obj instanceof Boolean) {
            return (char) (((boolean) obj) ? 0xFF : 0x00);
        } else {
            String str = obj.toString();
            return str.length() == 1 ? str.charAt(0) : null;
        }
    }

    // -----------------------------------------------------------------boolean convert

    public static boolean toBoolean(Object obj) {
        return toBoolean(obj, false);
    }

    public static boolean toBoolean(Object obj, boolean defaultVal) {
        Boolean value = toWrapBoolean(obj);
        return value == null ? defaultVal : value;
    }

    public static Boolean toWrapBoolean(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).byteValue() != ZERO_BYTE;
        } else {
            return Boolean.parseBoolean(obj.toString());
        }
    }

    // -----------------------------------------------------------------byte convert

    public static byte toByte(Object obj) {
        return toByte(obj, (byte) 0);
    }

    public static byte toByte(Object obj, byte defaultVal) {
        if (obj instanceof Number) {
            return ((Number) obj).byteValue();
        }
        Long value = parseLong(obj);
        return value == null ? defaultVal : value.byteValue();
    }

    public static Byte toWrapByte(Object obj) {
        if (obj instanceof Byte) {
            return (Byte) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).byteValue();
        }
        Long value = parseLong(obj);
        return value == null ? null : value.byteValue();
    }

    // -----------------------------------------------------------------short convert

    public static short toShort(Object obj) {
        return toShort(obj, (short) 0);
    }

    public static short toShort(Object obj, short defaultVal) {
        if (obj instanceof Number) {
            return ((Number) obj).shortValue();
        }
        Long value = parseLong(obj);
        return value == null ? defaultVal : value.shortValue();
    }

    public static Short toWrapShort(Object obj) {
        if (obj instanceof Short) {
            return (Short) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).shortValue();
        }
        Long value = parseLong(obj);
        return value == null ? null : value.shortValue();
    }

    // -----------------------------------------------------------------int convert

    public static int toInt(Object obj) {
        return toInt(obj, 0);
    }

    public static int toInt(Object obj, int defaultVal) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        Long value = parseLong(obj);
        return value == null ? defaultVal : value.intValue();
    }

    public static Integer toWrapInt(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        Long value = parseLong(obj);
        return value == null ? null : value.intValue();
    }

    // -----------------------------------------------------------------long convert

    public static long toLong(Object obj) {
        return toLong(obj, 0L);
    }

    public static long toLong(Object obj, long defaultVal) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        Long value = parseLong(obj);
        return value == null ? defaultVal : value;
    }

    public static Long toWrapLong(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return parseLong(obj);
    }

    // -----------------------------------------------------------------float convert

    public static float toFloat(Object obj) {
        return toFloat(obj, 0.0F);
    }

    public static float toFloat(Object obj, float defaultVal) {
        if (obj instanceof Number) {
            return ((Number) obj).floatValue();
        }
        Double value = parseDouble(obj);
        return value == null ? defaultVal : value.floatValue();
    }

    public static Float toWrapFloat(Object obj) {
        if (obj instanceof Float) {
            return (Float) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).floatValue();
        }
        Double value = parseDouble(obj);
        return value == null ? null : value.floatValue();
    }

    // -----------------------------------------------------------------double convert

    public static double toDouble(Object obj) {
        return toDouble(obj, 0.0D);
    }

    public static double toDouble(Object obj, double defaultVal) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        Double value = parseDouble(obj);
        return value == null ? defaultVal : value;
    }

    public static Double toWrapDouble(Object obj) {
        if (obj instanceof Double) {
            return (Double) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return parseDouble(obj);
    }

    // ---------------------------------------------------------------------number format

    /**
     * 数字精度
     *
     * @param value the number value
     * @param scale the scale
     * @return double value
     */
    public static double scale(Object value, int scale) {
        double val = toDouble(value);

        if (scale < 0) {
            return val;
        }

        return BigDecimal.valueOf(val)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 向下转单位
     *
     * @param value the number value
     * @param pow the pow
     * @return double value
     */
    public static double lower(double value, int pow) {
        return BigDecimal.valueOf(value / Math.pow(10, pow)).doubleValue();
    }

    public static double lower(double value, int pow, int scale) {
        return BigDecimal.valueOf(value / Math.pow(10, pow))
                         .setScale(scale, RoundingMode.HALF_UP)
                         .doubleValue();
    }

    /**
     * 向上转单位
     *
     * @param value
     * @param pow
     * @return
     */
    public static double upper(double value, int pow) {
        return BigDecimal.valueOf(value * Math.pow(10, pow)).doubleValue();
    }

    public static double upper(double value, int pow, int scale) {
        return BigDecimal.valueOf(value * Math.pow(10, pow))
                         .setScale(scale, RoundingMode.HALF_UP)
                         .doubleValue();
    }

    public static long nullZero(Long value) {
        return value == null ? 0 : value;
    }

    public static Long zeroNull(long value) {
        return value == 0 ? null : value;
    }

    public static int nullZero(Integer value) {
        return value == null ? 0 : value;
    }

    public static Integer zeroNull(int value) {
        return value == 0 ? null : value;
    }

    public static boolean isNullOrZero(Long value) {
        return value == null || value == 0L;
    }

    public static boolean isNullOrZero(Integer value) {
        return value == null || value == 0;
    }

    /**
     * 百分比
     *
     * @param numerator
     * @param denominator
     * @param scale
     * @return
     */
    public static String percent(double numerator, double denominator, int scale) {
        if (denominator == 0.0D) {
            return "--";
        }

        return percent(numerator / denominator, scale);
    }

    /**
     * 百分比
     *
     * @param value
     * @param scale
     * @return
     */
    public static String percent(double value, int scale) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "--";
        }

        String format = "#,##0";
        if (scale > 0) {
            // StringUtils.leftPad("", scale, '0'); String.format("%0" + scale + "d", 0);
            format += "." + Strings.repeat("0", scale);
        }
        return new DecimalFormat(format + "%").format(value);
    }

    /**
     * 数字格式化
     *
     * @param obj
     * @return
     */
    public static String format(Object obj) {
        return format(obj, "###,###.###");
    }

    /**
     * 数字格式化
     *
     * @param obj
     * @param format
     * @return
     */
    public static String format(Object obj, String format) {
        NumberFormat fmt = new DecimalFormat(format);
        if (obj instanceof CharSequence) {
            String str = obj.toString().replace(",", "");
            if (str.endsWith("%")) {
                str = str.substring(0, str.length() - 1);
                return fmt.format(Double.parseDouble(str)) + "%";
            } else {
                return fmt.format(Double.parseDouble(str));
            }
        } else {
            return fmt.format(obj);
        }
    }

    /**
     * Returns a string value of double
     *
     * @param value the double value
     * @param scale the scale
     * @return a string
     */
    public static String format(double value, int scale) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(scale);
        nf.setGroupingUsed(false);
        return nf.format(value);
    }

    /**
     * 区间取值
     *
     * @param value
     * @param min
     * @param max
     * @return
     */
    public static int bound(Integer value, int min, int max) {
        if (value == null || value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

    public static int sum(Integer a, Integer b) {
        return defaultIfNull(a, 0) + defaultIfNull(b, 0);
    }

    public static long sum(Long a, Long b) {
        return defaultIfNull(a, 0L) + defaultIfNull(b, 0L);
    }

    public static double sum(Double a, Double b) {
        return defaultIfNull(a, 0.0D) + defaultIfNull(b, 0.0D);
    }

    /**
     * 分片
     *
     * <pre>
     *   slice(0 , 2)  ->  [0, 0]
     *   slice(2 , 3)  ->  [1, 1, 0]
     *   slice(3 , 1)  ->  [3]
     *   slice(9 , 3)  ->  [3, 3, 3]
     *   slice(10, 3)  ->  [4, 3, 3]
     *   slice(11, 3)  ->  [4, 4, 3]
     *   slice(12, 3)  ->  [4, 4, 4]
     * </pre>
     *
     * @param quantity the quantity
     * @param segment the segment
     * @return int array
     */
    public static int[] slice(int quantity, int segment) {
        int[] result = new int[segment];
        int quotient = quantity / segment;
        int remainder = quantity % segment;
        int moreValue = quotient + 1;
        Arrays.fill(result, 0, remainder, moreValue);
        Arrays.fill(result, remainder, segment, quotient);
        return result;
    }

    /**
     * Partition the number
     * <pre>
     *   partition( 0, 2)  ->  [(0, 0)]
     *   partition( 2, 3)  ->  [(0, 0), (1, 1)]
     *   partition( 3, 1)  ->  [(0, 2)]
     *   partition( 9, 3)  ->  [(0, 2), (3, 5), (6, 8)]
     *   partition(10, 3)  ->  [(0, 3), (4, 6), (7, 9)]
     *   partition(11, 3)  ->  [(0, 3), (4, 7), (8, 10)]
     *   partition(12, 3)  ->  [(0, 3), (4, 7), (8, 11)]
     * </pre>
     *
     * @param number the number
     * @param size   the size
     * @return array
     */
    public static List<Tuple2<Integer, Integer>> partition(int number, int size) {
        Assert.isTrue(number >= 0, "Number must be greater than 0.");
        Assert.isTrue(size > 0, "Size must be greater than 0.");
        if (number == 0) {
            return Collections.singletonList(Tuple2.of(0, 0));
        }

        List<Tuple2<Integer, Integer>> result = new ArrayList<>(size);
        int last = -1;
        for (int value : slice(number, size)) {
            if (value == 0) {
                break;
            }
            result.add(Tuple2.of(last += 1, last += value - 1));
        }

        return result;
    }

    /**
     * Prorate the value for array
     * <pre>
     *  prorate(new int[]{249, 249, 249, 3}, 748) = [249, 249, 248, 2]
     *  prorate(new int[]{43, 1, 47}       , 61 ) = [29, 1, 31]
     * </pre>
     *
     * @param array the array
     * @param value the value
     * @return prorate result
     */
    public static int[] prorate(int[] array, int value) {
        int[] result = new int[array.length];
        if (array.length == 0 || value == 0) {
            return result;
        }

        int total = IntStream.of(array).sum();
        double rate;
        int i = 0, n = array.length - 1;
        for (; i < n; i++) {
            // rate <= 1.0
            rate = value / (double) total;
            result[i] = Math.min((int) Math.ceil(array[i] * rate), value);
            value -= result[i];
            total -= array[i];

            if (value == 0) {
                break;
            }
        }

        if (i == n) {
            result[i] = value;
        }
        return result;
    }

    /**
     * Returns the Long object is equals the Integer object
     *
     * @param a the Long a
     * @param b the Integer b
     * @return if is equals then return {@code true}
     */
    public static boolean equals(Long a, Integer b) {
        if (a == null && b == null) {
            return true;
        }
        return a != null && b != null && a.longValue() == b.intValue();
    }

    /**
     * To upper hex string and remove prefix 0
     *
     * @param value the BigInteger value
     * @return upper hex string
     */
    public static String toHex(BigInteger value) {
        String hex = Hex.encodeHexString(value.toByteArray(), false);
        if (ALL_ZERO_PATTERN.matcher(hex).matches()) {
            return "0";
        }
        return hex.replaceFirst("^0*", "");
    }

    // -------------------------------------------------------private methods

    private static Long parseLong(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            String val = obj.toString();
            return val.indexOf('.') == -1
                    ? Long.parseLong(val)
                    : (long) Double.parseDouble(val);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double parseDouble(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
