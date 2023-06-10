/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * URL encode/decode utility class.
 *
 * @author Ponfee
 */
public final class URLCodes {

    public static String encodeURI(String url) {
        return encodeURI(url, Files.UTF_8);
    }

    /**
     * <pre>
     * 相当于javascript中的encodeURI
     * 不会被此方法编码的字符：! @ # $& * ( ) = : / ; ? + '
     * encodeURI("http://www.oschina.net/search?scope=bbs&q=C语言", "UTF-8") -> http://www.oschina.net/search?scope=bbs&q=C%E8%AF%AD%E8%A8%80
     * </pre>
     *
     * @param url     the url string
     * @param charset the charset
     * @return encoded url string
     */
    public static String encodeURI(String url, String charset) {
        if (url == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(url.length() * 3 / 2);
        byte[] b;
        for (int n = url.length(), i = 0; i < n; i++) {
            char c = url.charAt(i);
            if (c >= 0 && c <= 255) {
                builder.append(c);
            } else {
                try {
                    b = Character.toString(c).getBytes(charset);
                } catch (Exception ignored) {
                    b = new byte[0];
                }
                for (int k, j = 0; j < b.length; j++) {
                    k = b[j];
                    if (k < 0) {
                        k += 256;
                    }
                    builder.append('%').append(Integer.toHexString(k).toUpperCase());
                }
            }
        }
        return builder.toString();
    }

    public static String decodeURI(String url) {
        return decodeURI(url, Files.UTF_8);
    }

    /**
     * 相当于javascript的decodeURI
     *
     * @param url     the url string
     * @param charset the charset
     * @return decoded url string
     */
    public static String decodeURI(String url, String charset) {
        if (url == null) {
            return null;
        }
        try {
            return URLDecoder.decode(url, charset);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // ------------------------------------------------------------------------------encode/decode uri component
    public static String encodeURIComponent(String url) {
        return encodeURIComponent(url, Files.UTF_8);
    }

    /**
     * <pre>
     * 相当于javascript中的encodeURIComponent
     * 不会被此方法编码的字符：! * ( )
     * encodeURIComponent("http://www.oschina.net/search?scope=bbs&q=C语言", "UTF-8") -> http%3A%2F%2Fwww.oschina.net%2Fsearch%3Fscope%3Dbbs%26q%3DC%E8%AF%AD%E8%A8%80
     * </pre>
     *
     * @param url     the uri string
     * @param charset the charset
     * @return encoded uri component string
     */
    public static String encodeURIComponent(String url, String charset) {
        if (url == null) {
            return null;
        }
        try {
            return URLEncoder.encode(url, charset);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String decodeURIComponent(String url) {
        return decodeURI(url, Files.UTF_8);
    }

    /**
     * 相当于javascript中的decodeURIComponent
     *
     * @param url     the url string
     * @param charset the charset
     * @return decoded uri component string
     */
    public static String decodeURIComponent(String url, String charset) {
        return decodeURI(url, charset);
    }

}
