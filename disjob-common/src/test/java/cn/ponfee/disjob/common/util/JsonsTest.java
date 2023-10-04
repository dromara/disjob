/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jsons Test
 *
 * @author Ponfee
 */
public class JsonsTest {

    @Test
    public void test() {
        assertThat(Jsons.toJson(null)).isEqualTo("null");
        assertThat(Jsons.toJson("null")).isEqualTo("\"null\"");
        assertThat(Jsons.toJson(new StringBuilder("null"))).isEqualTo("\"null\"");
        assertThat(Jsons.toJson(new StringBuilder())).isEqualTo("\"\"");
        assertThat(new StringBuilder().toString().equals("")).isTrue();

        assertThat(Jsons.toJson(new StringBuffer("null"))).isEqualTo("\"null\"");
        assertThat(Jsons.toJson(new StringBuffer())).isEqualTo("\"\"");
        assertThat(new StringBuffer().toString().equals("")).isTrue();
    }


    @Test
    public void testJson2() throws Exception {
        String json =
            "{\n" +
                "    \"type\": 'SHELL',\n" +
                "    \"script\": '\n" +
                "      #!/bin/sh \n" +
                "      echo \"hello shell!\"\n" +
                "    ',\n" +
                "}\n";

        System.out.println(json);
        Map<String, Object> map = Jsons.JSON5.readValue(json, Map.class);
        System.out.println(map);

        assertThat(map.get("script")).isEqualTo("\n" +
            "      #!/bin/sh \n" +
            "      echo \"hello shell!\"\n" +
            "    ");
    }

}
