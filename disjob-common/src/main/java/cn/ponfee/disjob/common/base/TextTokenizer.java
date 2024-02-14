/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Text tokenizer
 *
 * @author Ponfee
 */
public class TextTokenizer implements Iterator<String> {

    private final String text;
    private final String delimiter;

    private int position;

    public TextTokenizer(String text, String delimiter) {
        if (StringUtils.isEmpty(text)) {
            throw new IllegalArgumentException("Text token value cannot be empty.");
        }
        if (StringUtils.isEmpty(delimiter)) {
            throw new IllegalArgumentException("Text token delimiter cannot be empty.");
        }
        this.text = text;
        this.delimiter = delimiter;
        this.position = -1;
    }

    @Override
    public boolean hasNext() {
        return position < text.length();
    }

    @Override
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
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
