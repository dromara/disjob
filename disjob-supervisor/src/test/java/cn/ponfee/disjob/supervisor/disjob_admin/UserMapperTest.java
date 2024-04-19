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

package cn.ponfee.disjob.supervisor.disjob_admin;

import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author Ponfee
 */
public class UserMapperTest extends SpringBootTestBase<UserMapper> {

    @Test
    public void testQuerySql() {
        Map<String, Object> disjob = bean.getByLoginName("disjob");
        Map<String, Object> admin = bean.getByLoginName("admin");

        System.out.println("disjob: " + disjob);
        System.out.println("admin: " + admin);

        Assertions.assertEquals("disjob", disjob.get("login_name"));
        Assertions.assertEquals("admin", admin.get("login_name"));
    }

}
