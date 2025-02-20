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
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
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
        Map<String, Object> map = ImmutableMap.of(
            "basePackageClasses", Symbol.class,
            "mapperLocations", new String[]{"a", "b"},
            "basePackages", "x",
            "defaultFetchSize", 99
        );
        Ann ann = AnnotationProxy.create(Ann.class, map);
        System.out.println(Jsons.toJson(ann.basePackageClasses()));
        System.out.println(Jsons.toJson(ann.mapperLocations()));
        System.out.println(Jsons.toJson(ann.basePackages()));
        System.out.println(Jsons.toJson(ann.defaultFetchSize()));
        System.out.println(Jsons.toJson(ann.defaultStatementTimeout()));

        System.out.println("\n-------------------\n");

        map = ImmutableMap.of(
            "basePackageClasses", new Class[]{Symbol.class, UuidUtils.class},
            "mapperLocations", new String[]{"a", "b"},
            "basePackages", "x"
        );
        ann = AnnotationProxy.create(Ann.class, map);
        System.out.println(Jsons.toJson(ann.basePackageClasses()));
        System.out.println(Jsons.toJson(ann.mapperLocations()));
        System.out.println(Jsons.toJson(ann.basePackages()));
    }

}
