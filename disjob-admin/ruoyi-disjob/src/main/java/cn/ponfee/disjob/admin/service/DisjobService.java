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

package cn.ponfee.disjob.admin.service;

import cn.ponfee.disjob.common.base.IntValueDesc;
import cn.ponfee.disjob.common.base.IntValueEnum;
import cn.ponfee.disjob.common.spring.ResourceScanner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Disjob Service
 *
 * @author Ponfee
 */
@Service
public class DisjobService {

    /**
     * 加载枚举定义
     */
    private static final Map<String, Class<IntValueEnum<?>>> ENUM_MAP = new ResourceScanner("cn/ponfee/disjob/core/enums/**/*.class")
        .scan4class(new Class<?>[]{IntValueEnum.class}, null)
        .stream()
        // cn.ponfee.disjob.core.enums.TriggerType$1：Class#getSimpleName为空串，e.isAnonymousClass()为true
        .filter(e -> e.isEnum() && !e.isAnonymousClass())
        .collect(Collectors.toMap(Class::getSimpleName, e -> (Class<IntValueEnum<?>>) e));

    public List<IntValueDesc> enums(String enumName) {
        return values(enumName);
    }

    public String desc(String enumName, int value) {
        return values(enumName).stream()
            .filter(e -> e.getValue() == value)
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException("Enum value not found: " + enumName + "#" + value))
            .getDesc();
    }

    private static List<IntValueDesc> values(String enumName) {
        Class<IntValueEnum<?>> clazz = Objects.requireNonNull(ENUM_MAP.get(enumName), () -> "Enum name not found: " + enumName);
        return IntValueEnum.values(clazz);
    }

}
