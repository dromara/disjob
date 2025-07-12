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
import org.apache.commons.lang3.ArrayUtils;
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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

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

    // --------------------------------------------------------------character convert

    public static char toChar(Object obj) {
        return toChar(obj, Char.ZERO);
    }

    public static char toChar(Object obj, char defaultVal) {
        Character value = toWrapChar(obj);
        return value != null ? value : defaultVal;
    }

    public static Character toWrapChar(Object obj) {
        if (obj == null || obj instanceof Character) {
            return (Character) obj;
        }
        if (obj instanceof CharSequence && ((CharSequence) obj).length() == 1) {
            return ((CharSequence) obj).charAt(0);
        }
        return null;
    }

    // -----------------------------------------------------------------boolean convert

    public static boolean toBoolean(Object obj) {
        return toBoolean(obj, false);
    }

    public static boolean toBoolean(Object obj, boolean defaultVal) {
        Boolean value = toWrapBoolean(obj);
        return value != null ? value : defaultVal;
    }

    public static Boolean toWrapBoolean(Object obj) {
        return (obj instanceof Boolean) ? (Boolean) obj : parse(obj, Boolean::parseBoolean);
    }

    // -----------------------------------------------------------------byte convert

    public static byte toByte(Object obj) {
        return toByte(obj, (byte) 0);
    }

    public static byte toByte(Object obj, byte defaultVal) {
        Byte value = toWrapByte(obj);
        return value != null ? value : defaultVal;
    }

    public static Byte toWrapByte(Object obj) {
        return (obj instanceof Byte) ? (Byte) obj : parse(obj, Byte::parseByte);
    }

    // -----------------------------------------------------------------short convert

    public static short toShort(Object obj) {
        return toShort(obj, (short) 0);
    }

    public static short toShort(Object obj, short defaultVal) {
        Short value = toWrapShort(obj);
        return value != null ? value : defaultVal;
    }

    public static Short toWrapShort(Object obj) {
        return (obj instanceof Short) ? (Short) obj : parse(obj, Short::parseShort);
    }

    // -----------------------------------------------------------------int convert

    public static int toInt(Object obj) {
        return toInt(obj, 0);
    }

    public static int toInt(Object obj, int defaultVal) {
        Integer value = toWrapInt(obj);
        return value != null ? value : defaultVal;
    }

    public static Integer toWrapInt(Object obj) {
        return (obj instanceof Integer) ? (Integer) obj : parse(obj, Integer::parseInt);
    }

    // -----------------------------------------------------------------long convert

    public static long toLong(Object obj) {
        return toLong(obj, 0L);
    }

    public static long toLong(Object obj, long defaultVal) {
        Long value = toWrapLong(obj);
        return value != null ? value : defaultVal;
    }

    public static Long toWrapLong(Object obj) {
        return (obj instanceof Long) ? (Long) obj : parse(obj, Long::parseLong);
    }

    // -----------------------------------------------------------------float convert

    public static float toFloat(Object obj) {
        return toFloat(obj, 0.0F);
    }

    public static float toFloat(Object obj, float defaultVal) {
        Float value = toWrapFloat(obj);
        return value != null ? value : defaultVal;
    }

    public static Float toWrapFloat(Object obj) {
        return (obj instanceof Float) ? (Float) obj : parse(obj, Float::parseFloat);
    }

    // -----------------------------------------------------------------double convert

    public static double toDouble(Object obj) {
        return toDouble(obj, 0.0D);
    }

    public static double toDouble(Object obj, double defaultVal) {
        Double value = toWrapDouble(obj);
        return value != null ? value : defaultVal;
    }

    public static Double toWrapDouble(Object obj) {
        return (obj instanceof Double) ? (Double) obj : parse(obj, Double::parseDouble);
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

        return BigDecimal.valueOf(val).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 向下转单位
     *
     * @param value the number value
     * @param pow   the pow
     * @return double value
     */
    public static double lower(double value, int pow) {
        return BigDecimal.valueOf(value / Math.pow(10, pow)).doubleValue();
    }

    public static double lower(double value, int pow, int scale) {
        return BigDecimal.valueOf(value / Math.pow(10, pow)).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 向上转单位
     *
     * @param value the number value
     * @param pow   the pow
     * @return double value
     */
    public static double upper(double value, int pow) {
        return BigDecimal.valueOf(value * Math.pow(10, pow)).doubleValue();
    }

    public static double upper(double value, int pow, int scale) {
        return BigDecimal.valueOf(value * Math.pow(10, pow)).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public static long zeroIfNull(Long value) {
        return value == null ? 0 : value;
    }

    public static Long nullIfZero(long value) {
        return value == 0 ? null : value;
    }

    public static int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    public static Integer nullIfZero(int value) {
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
     * @param dividend the dividend
     * @param divisor  the divisor
     * @param scale    the scale
     * @return percent string value
     */
    public static String percent(double dividend, double divisor, int scale) {
        double value = dividend / divisor;
        return (Double.isInfinite(value) || Double.isNaN(value)) ? "-" : percent(value, scale);
    }

    /**
     * 百分比
     *
     * @param value the value
     * @param scale the scale
     * @return percent string value
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
     * @param obj the object
     * @return formatted string value
     */
    public static String format(Object obj) {
        return format(obj, "###,###.###");
    }

    /**
     * 数字格式化
     *
     * @param obj    the object
     * @param format the format
     * @return formatted string value
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
     * @param segment  the segment
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
            result.add(Tuple2.of(last += 1, last += (value - 1)));
        }

        return result;
    }

    /**
     * <pre>
     * Prorate the value for array
     *  prorate(new long[]{249, 249, 249, 3}, 748) = [249, 249, 248, 2]
     *  prorate(new long[]{43, 1, 47}       , 61 ) = [29, 1, 31]
     * </pre>
     *
     * @param array the array
     * @param value the value
     * @return prorate result
     */
    public static long[] prorate(long[] array, long value) {
        // 校验数组不能为空
        if (ArrayUtils.isEmpty(array)) {
            throw new IllegalArgumentException("Prorate array cannot be empty.");
        }
        // 校验所有数值的正负符号必须一致，不能存在`a>0 && b<0`的情况
        if (LongStream.of(array).filter(e -> e != 0).mapToObj(e -> e > 0).distinct().count() > 1) {
            throw new IllegalArgumentException("Prorate array has diff signum: " + Arrays.toString(array) + ", " + value);
        }
        // 校验value不能超出区间：[total, 0] or [0, total]
        long total = LongStream.of(array).sum();
        if (value < Math.min(total, 0) || value > Math.max(0, total)) {
            throw new IllegalArgumentException("Prorate array less than value: " + Arrays.toString(array) + ", " + value);
        }

        long[] result = new long[array.length];
        long remainingValue = value;
        long remainingTotal = total;
        for (int i = 0; i < array.length; i++) {
            result[i] = upDiv(remainingValue * array[i], remainingTotal);
            remainingValue -= result[i];
            remainingTotal -= array[i];
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
        String hex = Bytes.encodeHex(value.toByteArray(), false);
        if (ALL_ZERO_PATTERN.matcher(hex).matches()) {
            return "0";
        }
        return hex.replaceFirst("^0*", "");
    }

    // -------------------------------------------------------private methods

    private static <R> R parse(Object obj, Function<String, R> mapper) {
        if (obj == null) {
            return null;
        }
        try {
            return mapper.apply(obj.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static long upDiv(long dividend, long divisor) {
        return dividend == 0 ? 0 : Maths.upDiv(dividend, divisor);
    }

}
