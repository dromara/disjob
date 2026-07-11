package com.ruoyi.common.config.serializer;

import com.ruoyi.common.annotation.Sensitive;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.enums.DesensitizedType;
import com.ruoyi.common.utils.ShiroUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Objects;

/**
 * 数据脱敏序列化过滤
 *
 * @author ruoyi
 */
public class SensitiveJsonSerializer extends ValueSerializer<String>
{
    private DesensitizedType desensitizedType;

    @Override
    public void serialize(String value, JsonGenerator generator, SerializationContext provider)
    {
        if (desensitization())
        {
            generator.writeString(desensitizedType.desensitizer().apply(value));
        }
        else
        {
            generator.writeString(value);
        }
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property)
    {
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (Objects.nonNull(annotation) && Objects.equals(String.class, property.getType().getRawClass()))
        {
            this.desensitizedType = annotation.desensitizedType();
            return this;
        }
        return prov.findContentValueSerializer(property.getType(), property);
    }

    /**
     * 是否需要脱敏处理
     */
    private boolean desensitization()
    {
        SysUser securityUser = ShiroUtils.getSysUser();
        if (securityUser == null)
        {
            return true;
        }
        // 管理员不脱敏
        return !securityUser.isAdmin();
    }
}
