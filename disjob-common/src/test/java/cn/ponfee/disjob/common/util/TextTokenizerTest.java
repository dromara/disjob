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

import cn.ponfee.disjob.common.base.TextTokenizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TextTokenizer test
 *
 * @author Ponfee
 */
public class TextTokenizerTest {

    @Test
    public void testNext() {
        TextTokenizer tokenizer = new TextTokenizer("abc:123::test", ":");
        assertThat(tokenizer.next()).isEqualTo("abc");
        assertThat(tokenizer.next()).isEqualTo("123");
        assertThat(tokenizer.next()).isEqualTo("");
        assertThat(tokenizer.next()).isEqualTo("test");
        assertThat(tokenizer.hasNext()).isFalse();

        // -----------------------------------------------------

        tokenizer = new TextTokenizer("abc:-123:-:-test", ":-");
        assertThat(tokenizer.next()).isEqualTo("abc");
        assertThat(tokenizer.next()).isEqualTo("123");
        assertThat(tokenizer.next()).isEqualTo("");
        assertThat(tokenizer.hasNext()).isTrue();
        assertThat(tokenizer.next()).isEqualTo("test");
        assertThat(tokenizer.hasNext()).isFalse();
    }

    @Test
    public void testEmpty() {
        assertThatThrownBy(() -> new TextTokenizer("", ","))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Text token value cannot be empty.");

        assertThatThrownBy(() -> new TextTokenizer("abc", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Text token delimiter cannot be empty.");

        TextTokenizer tokenizer = new TextTokenizer("a", ":");
        assertThat(tokenizer.hasNext()).isTrue();
        assertThat(tokenizer.next()).isEqualTo("a");
        assertThat(tokenizer.hasNext()).isFalse();

        tokenizer = new TextTokenizer("a:", ":");
        assertThat(tokenizer.next()).isEqualTo("a");
        assertThat(tokenizer.hasNext()).isTrue();
        assertThat(tokenizer.next()).isEqualTo("");
        assertThat(tokenizer.hasNext()).isFalse();

        //assertThat("a".substring(0)).isEqualTo("a");
        assertThat("a".substring(1)).isEqualTo("");
    }

    @Test
    public void testTail() {
        TextTokenizer tokenizer = new TextTokenizer("abc:123::test", ":");
        assertThat(tokenizer.tail()).isEqualTo("abc:123::test");

        assertThat(tokenizer.next()).isEqualTo("abc");
        assertThat(tokenizer.tail()).isEqualTo("123::test");

        assertThat(tokenizer.next()).isEqualTo("123");
        assertThat(tokenizer.tail()).isEqualTo(":test");

        assertThat(tokenizer.next()).isEqualTo("");
        assertThat(tokenizer.tail()).isEqualTo("test");

        assertThat(tokenizer.next()).isEqualTo("test");
        assertThat(tokenizer.hasNext()).isFalse();

        // -----------------------------------------------------

        tokenizer = new TextTokenizer("abc:-123:-:-test", ":-");
        assertThat(tokenizer.tail()).isEqualTo("abc:-123:-:-test");

        assertThat(tokenizer.next()).isEqualTo("abc");
        assertThat(tokenizer.tail()).isEqualTo("123:-:-test");

        assertThat(tokenizer.next()).isEqualTo("123");
        assertThat(tokenizer.tail()).isEqualTo(":-test");

        assertThat(tokenizer.next()).isEqualTo("");
        assertThat(tokenizer.tail()).isEqualTo("test");

        assertThat(tokenizer.next()).isEqualTo("test");
        assertThat(tokenizer.hasNext()).isFalse();
    }

}
