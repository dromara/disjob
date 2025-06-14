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

import cn.ponfee.disjob.common.collect.TypedMap;
import cn.ponfee.disjob.common.model.PageRequest;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.HashMap;
import java.util.Map;

/**
 * TypedMap Test
 *
 * @author Ponfee
 */
class TypedMapTest {

    @Test
    void testTypedHashMap() {
        TypedHashMap<String, Object> map = new TypedHashMap<>();
        map.put("a", 123);
        map.put("b", "111");
        map.put("c", true);
        map.put("d", null);

        Assertions.assertEquals(123, map.get("a"));
        Assertions.assertEquals("111", map.get("b"));
        Assertions.assertEquals(true, map.get("c"));
        Assertions.assertEquals(111, map.getInt("b"));
        Assertions.assertEquals("111", map.getOrDefault("b", "111"));
        Assertions.assertEquals("xxx", map.getOrDefault("x", "xxx"));
        Assertions.assertNull(map.get("d"));
        Assertions.assertNull(map.get("e"));
        Assertions.assertTrue(map.containsKey("d"));
        Assertions.assertFalse(map.containsKey("e"));

        Assertions.assertTrue(map.containsValue(123));
        Assertions.assertTrue(map.removeBoolean("c"));
    }

    @Test
    void testPageRequest() throws JSONException {
        PageRequest request = new PageRequest();
        request.put("a", "123");
        request.setPaged(true);
        request.setPageNumber(1);
        request.setPageSize(20);
        JSONAssert.assertEquals("{\"paged\":true,\"pageSize\":20,\"pageNumber\":1,\"params\":{\"a\":\"123\"}}", Jsons.toJson(request), JSONCompareMode.NON_EXTENSIBLE);
    }

    private static class TypedHashMap<K, V> extends HashMap<K, V> implements TypedMap<K, V> {
        private static final long serialVersionUID = 8327519862600266982L;

        public TypedHashMap() {
        }

        public TypedHashMap(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        public TypedHashMap(Map<? extends K, ? extends V> m) {
            super(m);
        }
    }

}
