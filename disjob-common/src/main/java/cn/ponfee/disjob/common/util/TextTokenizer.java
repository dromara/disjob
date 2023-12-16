/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

/**
 * Text tokenizer
 *
 * @author Ponfee
 */
public class TextTokenizer {

    private final String text;
    private final String delimiter;

    private int position;

    public TextTokenizer(String text, String delimiter) {
        this.text = text;
        this.delimiter = delimiter;
        this.position = -1;
    }

    public boolean hasNext() {
        return text.indexOf(delimiter, position) != -1;
    }

    public String next() {
        int begin = (++position);
        int end   = position = text.indexOf(delimiter, position);
        if (position == -1) {
            position = text.length();
        } else {
            position = position + delimiter.length() - 1;
        }

        return end == -1 ? text.substring(begin) : text.substring(begin, end);
    }

    public String tail() {
        return text.substring(position + 1);
    }

}
