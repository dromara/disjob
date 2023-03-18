/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.spring;

import cn.ponfee.scheduler.common.base.Symbol.Char;
import cn.ponfee.scheduler.common.base.TypedMap;
import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Fields;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.common.util.Strings;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Yaml properties
 *
 * @author Ponfee
 */
public class YamlProperties extends Properties implements TypedMap<Object, Object> {
    private static final long serialVersionUID = -1599483902442715272L;

    public YamlProperties(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            loadYaml(inputStream);
        }
    }

    public YamlProperties(InputStream inputStream) {
        loadYaml(inputStream);
    }

    public YamlProperties(String content) {
        loadYaml(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    public <T> T extract(Class<T> beanType, String prefix) {
        T bean = ClassUtils.newInstance(beanType);
        char[] separators = {Char.HYPHEN, Char.DOT};
        for (Field field : ClassUtils.listFields(beanType)) {
            for (char separator : separators) {
                String name = prefix + Strings.toSeparatedName(field.getName(), separator);
                if (super.containsKey(name)) {
                    Fields.put(bean, field, ObjectUtils.cast(get(name), field.getType()));
                    break;
                }
            }
        }
        return bean;
    }

    private void loadYaml(InputStream inputStream) {
        Resource resource = new InputStreamResource(inputStream);
        super.putAll(YamlPropertySourceFactory.loadYml(resource));
    }

}
