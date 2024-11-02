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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * byte array utilities
 *
 * @author Ponfee
 */
public final class Bytes {

    private static final char[] HEX_LOWER_CODES = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    private static final char[] HEX_UPPER_CODES = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    public static final byte[] EMPTY = new byte[0];

    // -----------------------------------------------------------------hexEncode/hexDecode

    public static void encodeHex(char[] charArray, int i, byte b) {
        charArray[  i] = HEX_LOWER_CODES[(0xF0 & b) >>> 4];
        charArray[++i] = HEX_LOWER_CODES[ 0x0F & b       ];
    }

    public static String encodeHex(byte b, boolean lowercase) {
        char[] codes = lowercase ? HEX_LOWER_CODES : HEX_UPPER_CODES;
        return new String(new char[]{
            codes[(0xF0 & b) >>> 4], codes[0x0F & b]
        });
    }

    public static String encodeHex(byte[] bytes) {
        return encodeHex(bytes, true);
    }

    /**
     * encode the byte array the hex string
     *
     * @param bytes     the byte array
     * @param lowercase the boolean
     * @return string
     */
    public static String encodeHex(byte[] bytes, boolean lowercase) {
        //new BigInteger(1, bytes).toString(16);
        int len = bytes.length;
        char[] out = new char[len << 1];
        char[] codes = lowercase ? HEX_LOWER_CODES : HEX_UPPER_CODES;
        // one byte -> two char
        for (int i = 0, j = 0; i < len; i++) {
            out[j++] = codes[(0xF0 & bytes[i]) >>> 4];
            out[j++] = codes[ 0x0F & bytes[i]       ];
        }
        return new String(out);
    }

    /**
     * Decode hex string to byte array
     *
     * @param hex the hex string
     * @return byte array
     */
    public static byte[] decodeHex(String hex) {
        int len = hex.length();
        if ((len & 0x01) == 1) {
            throw new IllegalArgumentException("Hex string must be twice length.");
        }

        byte[] out = new byte[len >> 1];
        // two char -> one byte
        for (int i = 0, j = 0; j < len; i++, j += 2) {
            char c1 = hex.charAt(j), c2 = hex.charAt(j + 1);
            out[i] = (byte) (Character.digit(c1, 16) << 4 | Character.digit(c2, 16));
        }
        return out;
    }

    /**
     * <pre>
     * convert the byte array to binary string
     * byte:
     *    -1: 11111111
     *     0: 00000000
     *   127: 01111111
     *  -128: 10000000
     * </pre>
     *
     * @param array the byte array
     * @return binary string
     */
    public static String toBinary(byte... array) {
        if (array == null || array.length == 0) {
            return null;
        }

        StringBuilder builder = new StringBuilder(array.length << 3);
        String binary;
        for (byte b : array) {
            // byte & 0xFF ：byte转int保留bit位
            // byte | 0x100：对于正数保留八位，保证未尾8位为原byte的bit位，即1xxxxxxxx
            //               正数会有前缀0，如果不加，转binary string时前面的0会被舍去
            // 也可以用 “byte + 0x100”或者“leftPad(binaryString, 8, '0')”
            binary = Integer.toBinaryString((b & 0xFF) | 0x100);
            builder.append(binary, 1, binary.length());
        }
        return builder.toString();
    }

    // -----------------------------------------------------------------char array

    /**
     * Converts byte array to char array
     *
     * @param bytes the byte array
     * @return a char array
     */
    public static char[] toCharArray(byte[] bytes) {
        return toCharArray(bytes, StandardCharsets.US_ASCII);
    }

    /**
     * Converts byte array to char array
     *
     * @param bytes   the byte array
     * @param charset the charset
     * @return a char array
     */
    public static char[] toCharArray(byte[] bytes, Charset charset) {
        //return new String(bytes, charset).toCharArray();
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return charset.decode(buffer).array();
    }

    /**
     * Converts char array to byte array
     *
     * @param chars the char array
     * @return a byte array
     */
    public static byte[] toBytes(char[] chars) {
        return toBytes(chars, StandardCharsets.US_ASCII);
    }

    /**
     * Converts char array to byte array
     *
     * @param chars   the char array
     * @param charset the charset
     * @return a byte array
     */
    public static byte[] toBytes(char[] chars, Charset charset) {
        //return new String(chars).getBytes(charset);
        CharBuffer buffer = CharBuffer.allocate(chars.length);
        buffer.put(chars);
        buffer.flip();
        return charset.encode(buffer).array();
    }

    // -----------------------------------------------------------------char

    public static byte[] toBytes(char value) {
        return new byte[]{(byte) (value >>> 8), (byte) value};
    }

    public static char toChar(byte[] bytes) {
        return toChar(bytes, 0);
    }

    public static char toChar(byte[] bytes, int fromIdx) {
        return (char) (
            (bytes[  fromIdx]       ) << 8
          | (bytes[++fromIdx] & 0xFF)
      );
    }

    // -----------------------------------------------------------------int

    public static byte[] toBytes(int value) {
        byte[] bytes = new byte[4];
        put(value, bytes, 0);
        return bytes;
    }

    public static int toInt(byte[] bytes) {
        return toInt(bytes, 0);
    }

    public static int toInt(byte[] bytes, int fromIdx) {
        return (bytes[  fromIdx]       ) << 24 // 高8位转int后左移24位，刚好剩下原来的8位，故不用&0xFF
             | (bytes[++fromIdx] & 0xFF) << 16 // 其它转int：若为负数，则是其补码表示，故要&0xFF
             | (bytes[++fromIdx] & 0xFF) <<  8
             | (bytes[++fromIdx] & 0xFF);
    }

    // -----------------------------------------------------------------long

    /**
     * convert long value to byte array
     *
     * @param value the long number
     * @return byte array
     */
    public static byte[] toBytes(long value) {
        byte[] bytes = new byte[8];
        put(value, bytes, 0);
        return bytes;
    }

    public static String toHex(long value) {
        return toHex(value, true);
    }

    public static String toHex(long value, boolean lowercase) {
        char[] a = lowercase ? HEX_LOWER_CODES : HEX_UPPER_CODES;
        int mask = 0x0F;
        return new String(new char[]{
            a[       (int) (value >>> 60)], a[mask & (int) (value >>> 56)],
            a[mask & (int) (value >>> 52)], a[mask & (int) (value >>> 48)],
            a[mask & (int) (value >>> 44)], a[mask & (int) (value >>> 40)],
            a[mask & (int) (value >>> 36)], a[mask & (int) (value >>> 32)],
            a[mask & (int) (value >>> 28)], a[mask & (int) (value >>> 24)],
            a[mask & (int) (value >>> 20)], a[mask & (int) (value >>> 16)],
            a[mask & (int) (value >>> 12)], a[mask & (int) (value >>>  8)],
            a[mask & (int) (value >>>  4)], a[mask & (int) (value       )]
        });
    }

    /**
     * convert byte array to long number
     *
     * @param bytes   the byte array
     * @param fromIdx the byte array offset
     * @return long number
     */
    public static long toLong(byte[] bytes, int fromIdx) {
        return ((long) bytes[  fromIdx]       ) << 56
             | ((long) bytes[++fromIdx] & 0xFF) << 48
             | ((long) bytes[++fromIdx] & 0xFF) << 40
             | ((long) bytes[++fromIdx] & 0xFF) << 32
             | ((long) bytes[++fromIdx] & 0xFF) << 24
             | ((long) bytes[++fromIdx] & 0xFF) << 16
             | ((long) bytes[++fromIdx] & 0xFF) <<  8
             | ((long) bytes[++fromIdx] & 0xFF);
    }

    /**
     * convert byte array to long number
     *
     * @param bytes the byte array
     * @return long value
     */
    public static long toLong(byte[] bytes) {
        return toLong(bytes, 0);
    }

    // ---------------------------------------------------------BigDecimal

    /**
     * Convert a BigDecimal value to a byte array
     *
     * @param val the BigDecimal value
     * @return the byte array
     */
    public static byte[] toBytes(BigDecimal val) {
        byte[] valueBytes = val.unscaledValue().toByteArray();
        byte[] result = new byte[valueBytes.length + Integer.BYTES];
        put(val.scale(), result, 0);
        System.arraycopy(valueBytes, 0, result, 4, valueBytes.length);
        return result;
    }

    /**
     * Puts int number to byte array
     *
     * @param val    the int value
     * @param bytes  the byte array
     * @param offset the byte array start offset
     */
    public static void put(int val, byte[] bytes, int offset) {
        for (int i = 3; i >= 0; i--, offset++) {
            bytes[offset] = (byte) (val >>> (i << 3));
        }
    }

    public static void put(long val, byte[] bytes, int offset) {
        for (int i = 7; i >= 0; i--, offset++) {
            bytes[offset] = (byte) (val >>> (i << 3));
        }
    }

    /**
     * Converts a byte array to a BigDecimal
     *
     * @param bytes the byte array
     * @return BigDecimal
     */
    public static BigDecimal toBigDecimal(byte[] bytes) {
        return toBigDecimal(bytes, 0, bytes.length);
    }

    /**
     * Converts a byte array to a BigDecimal value
     *
     * @param bytes  the byte array
     * @param offset the offset
     * @param length the length
     * @return BigDecimal
     */
    public static BigDecimal toBigDecimal(byte[] bytes, int offset, int length) {
        if (bytes == null || length < (Integer.BYTES + 1) || (offset + length) > bytes.length) {
            return null;
        }

        int scale = toInt(bytes, offset);
        byte[] tcBytes = new byte[length - Integer.BYTES];
        System.arraycopy(bytes, offset + Integer.BYTES, tcBytes, 0, length - Integer.BYTES);
        return new BigDecimal(new BigInteger(tcBytes), scale);
    }

    // ---------------------------------------------------------BigInteger

    /**
     * Converts byte array to positive BigInteger
     *
     * @param bytes the byte array
     * @return a positive BigInteger number
     */
    public static BigInteger toBigInteger(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return BigInteger.ZERO;
        }
        return new BigInteger(1, bytes);
    }

    /**
     * Concat many byte arrays
     *
     * @param arrays the byte arrays
     * @return a new byte array of them
     */
    public static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }

    // ----------------------------------------------------------ByteBuffer

    public static void put(ByteBuffer buf, byte[] array) {
        if (array != null && array.length > 0) {
            buf.put(array);
        }
    }

    public static byte[] get(ByteBuffer buf, int length) {
        if (length == -1) {
            return null;
        }
        if (length == 0) {
            return EMPTY;
        }
        byte[] bytes = new byte[length];
        buf.get(bytes);
        return bytes;
    }

    public static byte[] remained(ByteBuffer buf) {
        int count = buf.limit() - buf.position();
        if (count < 0) {
            throw new IndexOutOfBoundsException("Index out of bound: " + count);
        }
        if (count == 0) {
            return EMPTY;
        }
        byte[] bytes = new byte[count];
        buf.get(bytes);
        return bytes;
    }

}
