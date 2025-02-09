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

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.collect.TypedMap;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.Properties;

/**
 * Yaml properties
 *
 * @author Ponfee
 */
public class YamlProperties extends Properties implements TypedMap<Object, Object> {
    private static final long serialVersionUID = -1599483902442715272L;

    private Binder binder;

    public YamlProperties(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            loadYaml(inputStream);
        }
    }

    public YamlProperties(InputStream inputStream) {
        loadYaml(inputStream);
    }

    public YamlProperties(byte[] content) {
        loadYaml(new ByteArrayInputStream(content));
    }

    public <T> T bind(String name, Class<T> beanType) {
        if (binder == null) {
            binder = new Binder(new MapConfigurationPropertySource(this));
        }
        return binder.bind(name, beanType).get();
    }

    private void loadYaml(InputStream inputStream) {
        Resource resource = new InputStreamResource(inputStream);
        super.putAll(YamlPropertySourceFactory.loadYml(resource));
    }

}
