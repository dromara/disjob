/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.util;

/**
 * String utilities
 * 
 * @author Ponfee
 */
public final class Strings {

    public static final char BLANK_CHAR = ' ';

    /**
     * <pre>
     * '?' Matches any single character.
     * '*' Matches any sequence of characters (including the empty sequence).
     *
     * isMatch("aa","a")       = false
     * isMatch("aa","aa")      = true
     * isMatch("aaa","aa")     = false
     * isMatch("aa", "*")      = true
     * isMatch("aa", "a*")     = true
     * isMatch("ab", "?*")     = true
     * isMatch("aab", "c*a*b") = false
     * </pre>
     *
     * @param s characters
     * @param p pattern
     * @return {@code true} if the characters match pattern
     */
    public static boolean matches(String s, String p) {
        int idxs = 0, idxp = 0, idxstar = -1, idxmatch = 0;
        while (idxs < s.length()) {
            // 当两个指针指向完全相同的字符时，或者p中遇到的是?时
            if (idxp < p.length() && (s.charAt(idxs) == p.charAt(idxp) || p.charAt(idxp) == '?')) {
                idxp++;
                idxs++;
                // 如果字符不同也没有?，但在p中遇到是*时，我们记录下*的位置，但不改变s的指针
            } else if (idxp < p.length() && p.charAt(idxp) == '*') {
                idxstar = idxp;
                idxp++;
                //遇到*后，我们用idxmatch来记录*匹配到的s字符串的位置，和不用*匹配到的s字符串位置相区分
                idxmatch = idxs;
                // 如果字符不同也没有?，p指向的也不是*，但之前已经遇到*的话，我们可以从idxmatch继续匹配任意字符
            } else if (idxstar != -1) {
                // 用上一个*来匹配，那我们p的指针也应该退回至上一个*的后面
                idxp = idxstar + 1;
                // 用*匹配到的位置递增
                idxmatch++;
                // s的指针退回至用*匹配到位置
                idxs = idxmatch;
            } else {
                return false;
            }
        }
        // 因为1个*能匹配无限序列，如果p末尾有多个*，我们都要跳过
        while (idxp < p.length() && p.charAt(idxp) == '*') {
            idxp++;
        }
        // 如果p匹配完了，说明匹配成功
        return idxp == p.length();
    }

    /**
     * Count str occur on text.
     *
     * @param text the text
     * @param str  the string
     * @return number of occur count
     */
    public static int count(String text, String str) {
        int count = 0;
        for (int len = str.length(), index=-len; (index = text.indexOf(str, index + len)) != -1; ) {
            count++;
        }
        return count;
    }

}
