/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GroovyUtils test
 *
 * @author Ponfee
 */
public class GroovyUtilsTest {

    @Test
    public void test() throws Exception {
        String scriptText =
            "package cn.ponfee.disjob;                    \n" +
            "import cn.ponfee.disjob.common.util.Jsons;   \n" +
            "return Jsons.toJson(list)+(a+b)+str.length(); \n";

        Map<String, Object> params = ImmutableMap.of(
            "list", Arrays.asList("x", "y"),
            "a", 1,
            "b", 2,
            "str", "string"
        );

        String result = "[\"x\",\"y\"]36";

        assertThat(GroovyUtils.Evaluator.SHELL.eval(scriptText, params).toString()).isEqualTo(result);
        assertThat(GroovyUtils.Evaluator.SCRIPT.eval(scriptText, params).toString()).isEqualTo(result);
        assertThat(GroovyUtils.Evaluator.CLASS.eval(scriptText, params).toString()).isEqualTo(result);
    }

}
