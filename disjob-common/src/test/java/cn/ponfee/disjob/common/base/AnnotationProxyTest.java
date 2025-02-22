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

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.UuidUtils;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.Map;

/**
 * AnnotationProxy Test
 *
 * @author Ponfee
 */
public class AnnotationProxyTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    public @interface Ann {

        String dataSourceName() default "";

        String[] mapperLocations() default {};

        String[] basePackages() default {};

        Class<?>[] basePackageClasses() default {};

        boolean mapUnderscoreToCamelCase() default true;

        String typeAliasesPackage() default "";

        int defaultFetchSize() default 100;

        int defaultStatementTimeout() default 25;

        boolean primary() default false;
    }

    @Test
    void test1() {
        Map<String, Object> map1 = ImmutableMap.of(
            "basePackageClasses", Symbol.class,
            "mapperLocations", new String[]{"a", "b"},
            "basePackages", "x",
            "defaultFetchSize", 99
        );
        Ann ann1 = AnnotationProxy.create(Ann.class, map1);
        Assertions.assertThat(Jsons.toJson(ann1.basePackageClasses())).isEqualTo("[\"cn.ponfee.disjob.common.base.Symbol\"]");
        Assertions.assertThat(Jsons.toJson(ann1.mapperLocations())).isEqualTo("[\"a\",\"b\"]");
        Assertions.assertThat(Jsons.toJson(ann1.basePackages())).isEqualTo("[\"x\"]");
        Assertions.assertThat(Jsons.toJson(ann1.defaultFetchSize())).isEqualTo("99");
        Assertions.assertThat(Jsons.toJson(ann1.defaultStatementTimeout())).isEqualTo("25");

        System.out.println("\n-------------------\n");

        Map<String, Object> map2 = ImmutableMap.of(
            "basePackageClasses", new Class[]{Symbol.class, UuidUtils.class},
            "mapperLocations", new String[]{"a", "b"},
            "basePackages", "x"
        );
        Ann ann2 = AnnotationProxy.create(Ann.class, map2);
        Assertions.assertThat(Jsons.toJson(ann2.basePackageClasses())).isEqualTo("[\"cn.ponfee.disjob.common.base.Symbol\",\"cn.ponfee.disjob.common.util.UuidUtils\"]");
        Assertions.assertThat(Jsons.toJson(ann2.mapperLocations())).isEqualTo("[\"a\",\"b\"]");
        Assertions.assertThat(Jsons.toJson(ann2.basePackages())).isEqualTo("[\"x\"]");

        Assertions.assertThat(Object[].class.isAssignableFrom(Object[].class)).isTrue();
        Assertions.assertThat(String[].class.isAssignableFrom(String[].class)).isTrue();
        Assertions.assertThat(Object[].class.isAssignableFrom(String[].class)).isTrue();
        Assertions.assertThat(String[].class.isAssignableFrom(Object[].class)).isFalse();

        Object[] a = {new Object(), new Object()};
        String[] b = {"a", "b"};
        Assertions.assertThat(Object[].class.isInstance(b)).isTrue();
        Assertions.assertThat(String[].class.isInstance(b)).isTrue();
        Assertions.assertThat(String[].class.isInstance(a)).isFalse();

        a = b;
        Assertions.assertThat(Arrays.toString(a)).isEqualTo("[a, b]");
    }

}
