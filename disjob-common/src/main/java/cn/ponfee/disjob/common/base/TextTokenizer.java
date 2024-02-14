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
