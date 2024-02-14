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

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.tuple.Tuple;
import org.apache.commons.lang3.builder.Builder;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Resource scanner test
 *
 * @author Ponfee
 */
public class ResourceScannerTest {

    @Test
    public void test() {
        Set<Class<?>> classes = new ResourceScanner("/cn/ponfee/disjob/**/*.class").scan4class(new Class[]{Tuple.class}, null);
        assertEquals(6, classes.size());

        classes = new ResourceScanner("org/apache/commons/lang3/**/*.class").scan4class(new Class[]{Builder.class}, null);
        assertEquals(11, classes.size());

        String file = "copy-right.txt";
        Map<String, String> map = new ResourceScanner(file).scan4text();
        assertThat(map.get(file))
            .startsWith("/*\n * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)\n")
            .contains("*     https://www.apache.org/licenses/LICENSE-2.0\n");
    }
}
