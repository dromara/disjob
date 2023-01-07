/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.spring;

import cn.ponfee.scheduler.common.base.TypedMap;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Yaml properties
 *
 * @author Ponfee
 */
public class YamlProperties extends Properties implements TypedMap<Object, Object> {

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

    private void loadYaml(InputStream inputStream) {
        Resource resource = new InputStreamResource(inputStream);
        super.putAll(YamlPropertySourceFactory.loadYml(resource));
    }

}
