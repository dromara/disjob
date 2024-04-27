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
