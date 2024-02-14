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

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Spring utils
 *
 * @author Ponfee
 */
public final class SpringUtils {

    public static Resource getResource(String resourceLocation) throws IOException {
        // return new DefaultResourceLoader().getResource(resourceLocation);
        URL url = ResourceUtils.getURL(resourceLocation);
        byte[] bytes = IOUtils.toByteArray(url);
        return new InputStreamResource(new ByteArrayInputStream(bytes));
    }

}
