package cn.ponfee.disjob.admin.service;

import cn.ponfee.disjob.common.base.IntValueDesc;

import java.util.List;

/**
 * 配置Service接口
 *
 * @author ponfee
 */
public interface DisjobService {

    /**
     * 获取枚举值列表
     *
     * @param enumName 枚举类型名称
     * @return 值列表
     */
    List<IntValueDesc> enums(String enumName);

    /**
     * 获取枚举值描述信息
     *
     * @param enumName 枚举类型名称
     * @param value    枚举值
     * @return 描述信息
     */
    String desc(String enumName, int value);

}
