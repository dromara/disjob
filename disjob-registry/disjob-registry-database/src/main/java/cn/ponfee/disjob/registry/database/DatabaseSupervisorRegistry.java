/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.database;

import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.database.configuration.DatabaseRegistryProperties;

/**
 * Registry supervisor based database.
 *
 * @author Ponfee
 */
public class DatabaseSupervisorRegistry extends DatabaseServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    public DatabaseSupervisorRegistry(DatabaseRegistryProperties config, JdbcTemplateWrapper jdbcTemplateWrapper) {
        super(config, jdbcTemplateWrapper);
    }

}
