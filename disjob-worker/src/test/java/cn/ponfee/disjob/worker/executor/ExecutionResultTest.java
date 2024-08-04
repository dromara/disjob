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

package cn.ponfee.disjob.worker.executor;

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
public class ExecutionResultTest {

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
        assertThatThrownBy(() -> Jsons.fromJson(json, ExecutionResult.class)).hasMessageStartingWith("Cannot construct instance of");
    }

}
