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

package cn.ponfee.disjob.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * System utility.
 *
 * 1）VM options(-D即为define)：java -Dkey=value -jar app.jar
 * 2）Program arguments，即main方法的args[]：java -jar app.jar --server.port=8080
 * </pre>
 *
 * @author Ponfee
 */
public final class SystemUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SystemUtils.class);

    public static String getConfig(String name) {
        String value = null;
        try {
            // JVM options
            value = System.getProperty(name);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        } catch (Exception e) {
            LOG.error("Get system property occur error: " + name, e);
        }

        try {
            // 获取操作系统环境变量
            value = System.getenv(name);
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        } catch (Exception e) {
            LOG.error("Get system env occur error: " + name, e);
        }

        return value;
    }

    public static String getConfig(String name, String defaultValue) {
        String value = getConfig(name);
        return value != null ? value : defaultValue;
    }

}
