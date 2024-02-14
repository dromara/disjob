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

package cn.ponfee.disjob.common.base;

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

        /**
         * Open symbol
         */
        String OPEN = "(";

        /**
         * Close symbol
         */
        String CLOSE = ")";

        /**
         * String of boolean true
         */
        String TRUE = "true";

        /**
         * String of boolean false
         */
        String FALSE = "false";
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
         * Tab symbol(the same as '\t')
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

        /**
         * Open symbol
         */
        char OPEN = '(';

        /**
         * Close symbol
         */
        char CLOSE = ')';
    }

}
