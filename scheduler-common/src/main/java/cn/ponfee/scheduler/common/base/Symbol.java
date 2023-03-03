/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.base;

/**
 * Symbol definitions.
 *
 * @author Ponfee
 */
public final class Symbol {

    public interface Str {
        /**
         * Zero symbol
         */
        String ZERO = "\u0000";

        /**
         * Colon symbol
         */
        String COLON = ":";

        /**
         * Comma symbol
         */
        String COMMA = ",";

        /**
         * Dot symbol
         */
        String DOT = ".";

        /**
         * Hyphen symbol
         */
        String HYPHEN = "-";

        /**
         * Slash symbol
         */
        String SLASH = "/";

        /**
         * Space symbol
         */
        String SPACE = " ";

        /**
         * Tab symbol
         */
        String TAB = "	";

        /**
         * Backslash symbol
         */
        String BACKSLASH = "\\";

        /**
         * CR symbol
         */
        String CR = "\r";

        /**
         * LF symbol
         */
        String LF = "\n";

        /**
         * Underscore symbol
         */
        String UNDERSCORE = "_";

        /**
         * Asterisk symbol
         */
        String ASTERISK = "*";

        /**
         * Semicolon symbol
         */
        String SEMICOLON = ";";

        /**
         * Ampersand symbol
         */
        String AMPERSAND = "&";
    }

    public interface Char {
        /**
         * Zero char symbol, equals '\0'
         */
        char ZERO = '\u0000';

        /**
         * Colon symbol
         */
        char COLON = ':';

        /**
         * Comma symbol
         */
        char COMMA = ',';

        /**
         * Dot symbol
         */
        char DOT = '.';

        /**
         * Hyphen symbol
         */
        char HYPHEN = '-';

        /**
         * Slash symbol
         */
        char SLASH = '/';

        /**
         * Space symbol
         */
        char SPACE = ' ';

        /**
         * Tab symbol
         */
        char TAB = '	';

        /**
         * Backslash symbol
         */
        char BACKSLASH = '\\';

        /**
         * CR symbol
         */
        char CR = '\r';

        /**
         * LF symbol
         */
        char LF = '\n';

        /**
         * Underscore symbol
         */
        char UNDERSCORE = '_';

        /**
         * Asterisk symbol
         */
        char ASTERISK = '*';

        /**
         * Semicolon symbol
         */
        char SEMICOLON = ';';

        /**
         * Ampersand symbol
         */
        char AMPERSAND = '&';
    }

}
