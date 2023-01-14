/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.test;

import ch.vorburger.mariadb4j.DB;
import cn.ponfee.scheduler.common.base.exception.CheckedThrowing;
import cn.ponfee.scheduler.test.db.EmbeddedMysqlServerMariaDB;
import cn.ponfee.scheduler.test.redis.EmbeddedRedisServerKstyrc;
import redis.embedded.RedisServer;

/**
 * Embedded mysql & redis server
 *
 * @author Ponfee
 */
public final class EmbeddedMysqlAndRedisServer {

    private DB mariaDBServer;
    private RedisServer redisServer;

    private EmbeddedMysqlAndRedisServer(int mysqlPort, int redisMasterPort, int redisSlavePort) {
        System.out.println("/*============================================================*\\");
        this.mariaDBServer = CheckedThrowing.caught(() -> EmbeddedMysqlServerMariaDB.start(mysqlPort));
        System.out.println("\\*============================================================*/");

        System.out.println("\n\n\n\n\n\n");

        System.out.println("/*============================================================*\\");
        this.redisServer = EmbeddedRedisServerKstyrc.start(redisMasterPort, redisSlavePort);
        System.out.println("\\*============================================================*/");

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public synchronized void stop() {
        if (mariaDBServer != null) {
            CheckedThrowing.caught(mariaDBServer::stop);
            mariaDBServer = null;
        }
        if (redisServer != null) {
            CheckedThrowing.caught(redisServer::stop);
            redisServer = null;
        }
    }

    public static Starter starter() {
        return new Starter();
    }

    public static class Starter {
        private int mysqlPort = 3306;
        private int redisMasterPort = 6379;
        private int redisSlavePort = 6380;

        private Starter() {
        }

        public Starter mysqlPort(int mysqlPort) {
            this.mysqlPort = mysqlPort;
            return this;
        }

        public Starter redisMasterPort(int redisMasterPort) {
            this.redisMasterPort = redisMasterPort;
            return this;
        }

        public Starter redisSlavePort(int redisSlavePort) {
            this.redisSlavePort = redisSlavePort;
            return this;
        }

        public EmbeddedMysqlAndRedisServer start() {
            return new EmbeddedMysqlAndRedisServer(mysqlPort, redisMasterPort, redisSlavePort);
        }
    }

}
