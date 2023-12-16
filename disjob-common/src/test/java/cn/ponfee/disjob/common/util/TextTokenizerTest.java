/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(tokenizer.next()).isEqualTo("test");
        assertThat(tokenizer.hasNext()).isFalse();
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
