/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.worker.redis;

import lombok.experimental.SuperBuilder;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;

/**
 * Creates standalone spring redis template
 *
 * @author Ponfee
 */
@SuperBuilder
public class StandaloneRedisTemplateCreator extends AbstractRedisTemplateCreator {

    private String host;
    private int port;

    @Override
    protected RedisConfiguration createRedisConfiguration() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setDatabase(database);
        configuration.setUsername(username);
        configuration.setPassword(password);
        configuration.setHostName(host);
        configuration.setPort(port);
        return configuration;
    }

}
