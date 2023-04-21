/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * byte array utilities
 *
 * @author Ponfee
 */
public final class Bytes {

    private static final char[] HEX_LOWER_CODES = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    private static final char[] HEX_UPPER_CODES = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    // -----------------------------------------------------------------hexEncode/hexDecode
    public static void hexEncode(char[] charArray, int i, byte b) {
        charArray[  i] = HEX_LOWER_CODES[(0xF0 & b) >>> 4];
        charArray[++i] = HEX_LOWER_CODES[ 0x0F & b       ];
    }

    public static String hexEncode(byte b, boolean lowercase) {
        char[] codes = lowercase ? HEX_LOWER_CODES : HEX_UPPER_CODES;
        return new String(new char[] {
            codes[(0xF0 & b) >>> 4], codes[0x0F & b]
        });
    }

    public static String hexEncode(byte[] bytes) {
        return hexEncode(bytes, true);
    }

    /**
     * encode the byte array the hex string
     * @param bytes
     * @param lowercase
     * @return
     */
    public static String hexEncode(byte[] bytes, boolean lowercase) {
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
     * decode the hex string to byte array
     * @param hex
     * @return
     */
    public static byte[] hexDecode(String hex) {
        char[] data = hex.toCharArray();
        int len = data.length;
        if ((len & 0x01) == 1) {
            throw new IllegalArgumentException("Invalid hex string.");
        }

        byte[] out = new byte[len >> 1];

        // two char -> one byte
        for (int i = 0, j = 0; j < len; i++, j += 2) {
            out[i] = (byte) ( Character.digit(data[j], 16) << 4 | Character.digit(data[j + 1], 16) );
        }
        return out;
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
     * @param bytes the byte array
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
     * @param chars the char array
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
        return new byte[] {(byte) (value >>> 8), (byte) value};
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
        return new byte[] {
            (byte) (value >>> 24), (byte) (value >>> 16),
            (byte) (value >>>  8), (byte) (value       )
        };
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
     * @param value the long number
     * @return byte array
     */
    public static byte[] toBytes(long value) {
        return new byte[] {
            (byte) (value >>> 56), (byte) (value >>> 48),
            (byte) (value >>> 40), (byte) (value >>> 32),
            (byte) (value >>> 24), (byte) (value >>> 16),
            (byte) (value >>>  8), (byte) (value       )
        };
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
     * @param bytes  the byte array
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
     * @param bytes the byte array
     * @return
     */
    public static long toLong(byte[] bytes) {
        return toLong(bytes, 0);
    }

    // ---------------------------------------------------------BigDecimal
    /**
     * Convert a BigDecimal value to a byte array
     *
     * @param val
     * @return the byte array
     */
    public static byte[] toBytes(BigDecimal val) {
        byte[] valueBytes = val.unscaledValue().toByteArray();
        byte[] result = new byte[valueBytes.length + Integer.BYTES];
        int offset = putInt(val.scale(), result, 0);
        System.arraycopy(valueBytes, 0, result, offset, valueBytes.length);
        return result;
    }

    /**
     * Puts int number to byte array
     *
     * @param val    the int value
     * @param bytes  the byte array
     * @param offset the byte array start offset
     * @return int of next offset
     */
    public static int putInt(int val, byte[] bytes, int offset) {
        bytes[  offset] = (byte) (val >>> 24);
        bytes[++offset] = (byte) (val >>> 16);
        bytes[++offset] = (byte) (val >>>  8);
        bytes[++offset] = (byte) (val       );
        return ++offset;
    }

    /**
     * Converts a byte array to a BigDecimal
     *
     * @param bytes
     * @return the char value
     */
    public static BigDecimal toBigDecimal(byte[] bytes) {
        return toBigDecimal(bytes, 0, bytes.length);
    }

    /**
     * Converts a byte array to a BigDecimal value
     *
     * @param bytes
     * @param offset
     * @param length
     * @return the char value
     */
    public static BigDecimal toBigDecimal(byte[] bytes, int offset, final int length) {
        if (bytes == null || length < Integer.BYTES + 1 ||
            (offset + length > bytes.length)) {
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

    // ----------------------------------------------------------others
    /**
     * merge byte arrays
     * @param first  first byte array of args
     * @param rest   others byte array
     * @return a new byte array of them
     */
    public static byte[] concat(byte[] first, byte[]... rest) {
        Objects.requireNonNull(first, "the first array arg cannot be null");
        if (rest == null || rest.length == 0) {
            return first;
        }

        int totalLength = first.length;
        for (byte[] array : rest) {
            if (array != null) {
                totalLength += array.length;
            }
        }

        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            if (array != null) {
                System.arraycopy(array, 0, result, offset, array.length);
                offset += array.length;
            }
        }
        return result;
    }

    public static byte[] remaining(ByteBuffer buf) {
        int count = buf.limit() - buf.position();
        if (count <= 0) {
            return null;
        }
        byte[] bytes = new byte[count];
        buf.get(bytes);
        return bytes;
    }

}
