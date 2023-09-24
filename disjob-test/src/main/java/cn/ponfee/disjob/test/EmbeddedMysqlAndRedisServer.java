/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.test;

import ch.vorburger.mariadb4j.DB;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.test.db.EmbeddedMysqlServerMariaDB;
import cn.ponfee.disjob.test.redis.EmbeddedRedisServerKstyrc;
import redis.embedded.RedisServer;

/**
 * Embedded mysql & redis server
 *
 * @author Ponfee
 */
public final class EmbeddedMysqlAndRedisServer {

    public static void main(String[] args) {
        EmbeddedMysqlAndRedisServer.starter()
            .mysqlPort(3306)
            .redisMasterPort(6379)
            .redisSlavePort(6380)
            .start();
    }

    private volatile DB mariaDBServer;
    private volatile RedisServer redisServer;

    private EmbeddedMysqlAndRedisServer(int mysqlPort, int redisMasterPort, int redisSlavePort) {
        System.out.println("/*============================================================*\\");
        this.mariaDBServer = ThrowingSupplier.execute(() -> EmbeddedMysqlServerMariaDB.start(mysqlPort));
        System.out.println("\\*============================================================*/");

        System.out.println("\n\n\n\n\n\n");

        System.out.println("/*============================================================*\\");
        this.redisServer = EmbeddedRedisServerKstyrc.start(redisMasterPort, redisSlavePort);
        System.out.println("\\*============================================================*/");

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public synchronized void stop() {
        ThrowingRunnable.execute(() -> Thread.sleep(10000));
        if (mariaDBServer != null) {
            ThrowingRunnable.execute(mariaDBServer::stop);
            mariaDBServer = null;
        }
        if (redisServer != null) {
            ThrowingRunnable.execute(redisServer::stop);
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
