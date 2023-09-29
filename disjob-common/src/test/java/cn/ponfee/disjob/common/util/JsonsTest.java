/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Jsons Test
 *
 * @author Ponfee
 */
public class JsonsTest {

    @Test
    public void test() {
        Assertions.assertThat(Jsons.toJson(null)).isEqualTo("null");
        Assertions.assertThat(Jsons.toJson("null")).isEqualTo("\"null\"");
        Assertions.assertThat(Jsons.toJson(new StringBuilder("null"))).isEqualTo("\"null\"");
        Assertions.assertThat(Jsons.toJson(new StringBuilder(""))).isEqualTo("\"\"");
        Assertions.assertThat(Jsons.toJson(new StringBuilder())).isEqualTo("\"\"");

        Assertions.assertThat(Jsons.toJson(new StringBuffer("null"))).isEqualTo("\"null\"");
        Assertions.assertThat(Jsons.toJson(new StringBuffer(""))).isEqualTo("\"\"");
        Assertions.assertThat(Jsons.toJson(new StringBuffer())).isEqualTo("\"\"");
    }

}
