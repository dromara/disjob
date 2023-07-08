/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
