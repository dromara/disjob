/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.util.Jsons;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Execute result test
 *
 * @author Ponfee
 */
public class ExecuteResultTest {

    @Test
    public void test() {
        Result<Void> success = Result.success();
        String json = success.toString();
        assertThat(json).isEqualTo("{\"code\":0,\"msg\":\"OK\"}");
        Result<?> result = Jsons.fromJson(json, Result.class);
        assertThat(result.getClass()).isEqualTo(Result.class);
        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getMsg()).isEqualTo("OK");
        assertThat(result.getData()).isNull();

        assertThatThrownBy(() -> Jsons.fromJson(json, Result.ImmutableResult.class)).hasMessageStartingWith("Cannot construct instance of");
        assertThatThrownBy(() -> Jsons.fromJson(json, ExecuteResult.class)).hasMessageStartingWith("Cannot construct instance of");
    }

}
