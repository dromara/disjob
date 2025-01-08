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

package cn.ponfee.disjob.common.tree;

import cn.ponfee.disjob.common.util.Comparators;
import cn.ponfee.disjob.common.util.Jsons;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * NodePath test
 *
 * @author Ponfee
 */
public class NodePathTest {

    static TypeReference<NodePath<JsonId>> LIST_STRING = new TypeReference<NodePath<JsonId>>() { };

    @Test
    public void testJson() {
        JsonId root = new JsonId(null, "root", 0);
        JsonId parent = new JsonId(root, "parent", 1);
        JsonId child = new JsonId(parent, "child", 1);
        NodePath<JsonId> npRoot = new NodePath<>(root);
        NodePath<JsonId> npParent = new NodePath<>(root, parent);
        NodePath<JsonId> npChild = new NodePath<>(npParent, child);

        Assertions.assertEquals("[root, parent, child]", child.toNodePath(JsonId::getName).toString());

        String jsonRoot = Jsons.toJson(npRoot);
        Assertions.assertEquals("[{\"name\":\"root\",\"orders\":0}]", jsonRoot);

        NodePath rootPath1 = Jsons.fromJson(jsonRoot, NodePath.class);
        Assertions.assertEquals(LinkedHashMap.class, rootPath1.get(0).getClass());
        Assertions.assertEquals("root", ((LinkedHashMap) rootPath1.get(0)).get("name"));

        NodePath<JsonId> rootPath2 = Jsons.fromJson(jsonRoot, LIST_STRING);
        Assertions.assertEquals(JsonId.class, rootPath2.get(0).getClass());
        Assertions.assertEquals("root", rootPath2.get(0).getName());

        Wrapper wrapper1 = new Wrapper(npChild);
        String jsonWrapper = Jsons.toJson(wrapper1);
        Assertions.assertEquals("{\"value\":[{\"name\":\"root\",\"orders\":0},{\"parent\":{\"name\":\"root\",\"orders\":0},\"name\":\"parent\",\"orders\":1},{\"parent\":{\"parent\":{\"name\":\"root\",\"orders\":0},\"name\":\"parent\",\"orders\":1},\"name\":\"child\",\"orders\":1}]}", jsonWrapper);
        Wrapper wrapper2 = Jsons.fromJson(jsonWrapper, Wrapper.class);
        Assertions.assertEquals(JsonId.class, wrapper2.getValue().get(0).getClass());
        Assertions.assertEquals("root", wrapper2.getValue().get(0).getName());
        Assertions.assertEquals("parent", wrapper2.getValue().get(1).getName());
        Assertions.assertEquals("child", wrapper2.getValue().get(2).getName());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Wrapper {
        private NodePath<JsonId> value;
    }

    @Getter
    @Setter
    public static class JsonId extends NodeId<JsonId> {
        private static final long serialVersionUID = -6344204521700761391L;

        private String name;
        private int orders;

        public JsonId() {
            super(null);
        }

        public JsonId(JsonId parent, @Nonnull String name, int orders) {
            super(parent);
            this.name = Objects.requireNonNull(name);
            this.orders = orders;
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + Objects.hashCode(name);
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj)
                && Objects.equals(this.name, ((JsonId) obj).name);
        }

        @Override
        public int compareTo(@Nonnull JsonId that) {
            int compared = super.compareTo(that);
            if (compared != 0) {
                return compared;
            }
            compared = Integer.compare(this.orders, that.orders);
            if (compared != 0) {
                return compared;
            }
            return Comparators.compareNullsFirst(this.name, that.name);
        }
    }

}
