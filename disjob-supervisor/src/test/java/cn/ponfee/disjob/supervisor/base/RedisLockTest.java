/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.lock.RedisLock;
import cn.ponfee.disjob.common.lock.RedisLockFactory;
import cn.ponfee.disjob.common.util.MavenProjects;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

/**
 * Redis lock test
 *
 * @author Ponfee
 */
public class RedisLockTest extends SpringBootTestBase<StringRedisTemplate> {

    private static final String NAME = RandomStringUtils.randomAlphabetic(3);
    private static final int ROUND = 1;

    private RedisLockFactory factory;

    @Override
    protected void beforeEach() {
        factory = new RedisLockFactory(bean);
    }

    @Test
    public void test0() throws InterruptedException {
        String key = "test:" + UuidUtils.uuid32();
        RedisLock redisLock = factory.create(key, 5000);
        Assertions.assertTrue(redisLock.tryLock());

        Thread.sleep(55);
        Long ttl1 = bean.getExpire("lock:" + key, TimeUnit.MILLISECONDS);
        System.out.println("ttl1: " + ttl1);
        Assertions.assertTrue(ttl1 > 4000 && ttl1 < 5000);

        Thread.sleep(1000);
        Long ttl2 = bean.getExpire("lock:" + key, TimeUnit.MILLISECONDS);
        System.out.println("ttl2: " + ttl2);
        Assertions.assertTrue(ttl2 > 3000 && ttl2 < 4000);

        Assertions.assertTrue(redisLock.tryLock());
        Thread.sleep(50);
        Long ttl3 = bean.getExpire("lock:" + key, TimeUnit.MILLISECONDS);
        System.out.println("ttl3: " + ttl3);
        Assertions.assertTrue(ttl3 > 4000 && ttl3 < 5000);

        Assertions.assertTrue(redisLock.isLocked());
        Assertions.assertTrue(redisLock.isHeldByCurrentThread());

        Thread thread = new Thread(() -> {
            Assertions.assertTrue(redisLock.isLocked());
            Assertions.assertFalse(redisLock.isHeldByCurrentThread());
            System.out.println("child thread done.");
        });
        thread.start();
        thread.join();

        Assertions.assertTrue(redisLock.isLocked());
        redisLock.unlock();

        Assertions.assertTrue(redisLock.isLocked());
        redisLock.unlock();

        Assertions.assertFalse(redisLock.isLocked());
        Assertions.assertFalse(redisLock.isHeldByCurrentThread());
        Assertions.assertTrue(redisLock.tryLock());
    }

    @Test
    public void test1() throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file())));
        final Printer printer = new Printer(factory.create("test:lock:1", 30000));
        final AtomicInteger num = new AtomicInteger(0);
        String line;
        List<Thread> threads = new ArrayList<>();
        System.out.println("\n=========================START========================");
        for (int i = 0; i < ROUND && (line = reader.readLine()) != null; i++) {
            final String line0 = line;
            threads.add(new Thread(() -> printer.output(NAME + "-" + num.getAndIncrement() + "\t" + line0 + "\n")));
        }
        reader.close();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("=========================END========================\n");
    }

    @Test
    public void test2() throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file())));
        final Lock lock = factory.create("test:lock:2", 30000);
        final AtomicInteger num = new AtomicInteger(0);
        String line;
        List<Thread> threads = new ArrayList<>();
        System.out.println("\n=========================START========================");
        for (int i = 0; i < ROUND && (line = reader.readLine()) != null; i++) {
            final String _line = line;
            threads.add(new Thread(
                () -> new Printer(lock).output(NAME + "-" + num.getAndIncrement() + "\t" + _line + "\n")
            ));
        }
        reader.close();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("=========================END========================\n");
    }

    @Test
    public void test3() throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file())));
        final AtomicInteger num = new AtomicInteger(0);
        String line;
        List<Thread> threads = new ArrayList<>();
        System.out.println("\n=========================START========================");
        for (int i = 0; i < ROUND && (line = reader.readLine()) != null; i++) {
            final String line0 = line;
            threads.add(new Thread(
                () -> new Printer(factory.create("test:lock:3", 30000)).output(NAME + "-" + num.getAndIncrement() + "\t" + line0 + "\n")
            ));
        }
        reader.close();
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("=========================END========================\n");
    }

    @Test
    public void test4() throws IOException {
        Printer printer = new Printer(factory.create("test:lock:4", 30000));
        System.out.println("\n=========================START========================");
        List<String> lines = Files.readLines(file(), StandardCharsets.UTF_8)
            .subList(0, ROUND);

        execute(lines, line -> printer.output(NAME + "-" + line + "\n"), ThreadPoolExecutors.commonThreadPool());
        System.out.println("=========================END========================\n");
    }

    /**
     * 当 key 不存在时，返回 -2
     * 当 key 存在但没有设置剩余生存时间时，返回 -1
     * 否则，以秒为单位，返回 key 的剩余生存时间
     *
     * @throws InterruptedException
     */
    @Test
    public void testTryLockWithTimeout() throws InterruptedException {
        int expire = 1000;
        String lockKey = "test:lock:" + UuidUtils.uuid32();
        String actualKey = "lock:" + lockKey;

        RedisLock redisLock = factory.create(lockKey, expire);
        Assertions.assertTrue(redisLock.tryLock());

        Assertions.assertTrue(bean.hasKey(actualKey));

        long ttl1 = bean.getExpire(actualKey, TimeUnit.MILLISECONDS);
        System.out.println("TTL1: " + ttl1);
        Assertions.assertTrue(ttl1 > 0 && ttl1 <= expire);

        Thread.sleep(expire);

        long ttl2 = bean.getExpire(actualKey, TimeUnit.MILLISECONDS);
        System.out.println("TTL2: " + ttl2);
        Assertions.assertTrue(ttl2 <= 0);

        Thread.sleep(expire);

        long ttl3 = bean.getExpire(actualKey, TimeUnit.MILLISECONDS);
        System.out.println("TTL3: " + ttl3);
        Assertions.assertTrue(ttl3 <= 0);

        redisLock.unlock();
        long ttl4 = bean.getExpire(actualKey, TimeUnit.MILLISECONDS);
        System.out.println("TTL4: " + ttl4);
        Assertions.assertEquals(-2, ttl4);
    }

    private static class Printer {
        private final Lock lock;

        Printer(Lock lock) {
            this.lock = lock;
        }

        private void output(final String text) {
            lock.lock();
            try {
                for (int i = 0, n = text.length(); i < n; i++) {
                    System.out.print(text.charAt(i));
                    if ((i & 0x3F) == 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public static <T> void execute(Collection<T> coll, Consumer<T> action, Executor executor) {
        Stopwatch watch = Stopwatch.createStarted();
        coll.stream()
            .map(e -> CompletableFuture.runAsync(() -> action.accept(e), executor))
            .forEach(CompletableFuture::join);
        System.out.println("multi thread run async duration: " + watch.stop());
    }

    private File file() {
        return MavenProjects.getTestJavaFile(getClass());
    }
}
