package cn.ponfee.disjob.admin.service.impl;

import cn.ponfee.disjob.admin.service.DisjobService;
import cn.ponfee.disjob.common.base.IntValueDesc;
import cn.ponfee.disjob.common.base.IntValueEnum;
import cn.ponfee.disjob.common.spring.ResourceScanner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 配置Service业务层处理
 *
 * @author ponfee
 */
@Service("disjobService")
public class DisjobServiceImpl implements DisjobService {

    /**
     * 枚举定义
     */
    private static final Map<String, Class<IntValueEnum<?>>> ENUM_MAP = new ResourceScanner("cn/ponfee/disjob/**/*.class")
        .scan4class(new Class<?>[]{IntValueEnum.class}, null)
        .stream()
        // cn.ponfee.disjob.core.enums.TriggerType$1：Class#getSimpleName为空串，e.isAnonymousClass()为true
        .filter(e -> e.isEnum() && !e.isAnonymousClass())
        .collect(Collectors.toMap(Class::getSimpleName, e -> (Class<IntValueEnum<?>>) e));

    @Override
    public List<IntValueDesc> enums(String enumName) {
        Class<IntValueEnum<?>> enumClass = Objects.requireNonNull(ENUM_MAP.get(enumName), () -> "Enum name not found: " + enumName);
        return IntValueEnum.values(enumClass);
    }

    @Override
    public String desc(String enumName, int value) {
        Class<IntValueEnum<?>> enumClass = Objects.requireNonNull(ENUM_MAP.get(enumName), () -> "Enum name not found: " + enumName);
        return IntValueEnum.values(enumClass).stream().filter(e -> e.getValue() == value).findAny().get().getDesc();
    }

}
