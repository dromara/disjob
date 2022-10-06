package cn.ponfee.scheduler.supervisor.test.common.spring;

import cn.ponfee.scheduler.common.lock.RedisLock;
import cn.ponfee.scheduler.common.util.MavenProjects;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.supervisor.SpringBootTestBase;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Redis lock test
 *
 * @author Ponfee
 */
public class RedisLockTest extends SpringBootTestBase<StringRedisTemplate> {

    private static final String NAME = RandomStringUtils.randomAlphabetic(3);
    private static final int RATIO = 7;

    @Test
    public void test0() throws InterruptedException {
        RedisLock redisLock = new RedisLock(bean(), "test:lock:" + ObjectUtils.uuid32(), 1000);
        Assertions.assertTrue(redisLock.tryLock());
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
        Assertions.assertFalse(redisLock.isLocked());
        Assertions.assertFalse(redisLock.isHeldByCurrentThread());
        Assertions.assertTrue(redisLock.tryLock());
    }

    @Test
    public void test1() throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file())));
        final Printer printer = new Printer(new RedisLock(bean(), "test:lock:1", 30000));
        final AtomicInteger num = new AtomicInteger(0);
        String line;
        List<Thread> threads = new ArrayList<>();
        System.out.println("\n=========================START========================");
        while ((line = reader.readLine()) != null) {
            if (ThreadLocalRandom.current().nextInt(RATIO) == 0) {
                final String line0 = line;
                threads.add(new Thread(() -> printer.output(NAME + "-" + num.getAndIncrement() + "\t" + line0 + "\n")));
            }
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
        final Lock lock = new RedisLock(bean(), "test:lock:2", 30000);
        final AtomicInteger num = new AtomicInteger(0);
        String line;
        List<Thread> threads = new ArrayList<>();
        System.out.println("\n=========================START========================");
        while ((line = reader.readLine()) != null) {
            if (ThreadLocalRandom.current().nextInt(RATIO) == 0) {
                final String _line = line;
                threads.add(new Thread(
                    () -> new Printer(lock).output(NAME + "-" + num.getAndIncrement() + "\t" + _line + "\n")
                ));
            }
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
        while ((line = reader.readLine()) != null) {
            final String line0 = line;
            if (ThreadLocalRandom.current().nextInt(RATIO) == 0) {
                threads.add(new Thread(
                    () -> new Printer(new RedisLock(bean(), "test:lock:3", 30000)).output(NAME + "-" + num.getAndIncrement() + "\t" + line0 + "\n")
                ));
            }
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
        Printer printer = new Printer(new RedisLock(bean(), "test:lock:4", 30000));
        AtomicInteger num = new AtomicInteger(RATIO);
        System.out.println("\n=========================START========================");
        List<Map<Integer, String>> lines = Files.readLines(file(), StandardCharsets.UTF_8)
                .stream()
                .filter(e -> ThreadLocalRandom.current().nextInt(3) == 0)
                .map(line -> ImmutableMap.of(num.getAndIncrement(), line))
                .collect(Collectors.toList());

        execute(lines, map -> {
            Entry<Integer, String> line = map.entrySet().iterator().next();
            printer.output(NAME + "-" + line.getKey() + "\t" + line.getValue() + "\n");
        }, ForkJoinPool.commonPool());
        System.out.println("=========================END========================\n");
    }

    @Test
    public void testTryLockWithTimeout() throws InterruptedException {
        Assertions.assertFalse(Arrays.equals("value".getBytes(), (byte[]) bean().execute((RedisCallback<Object>) conn -> conn.get(ObjectUtils.uuid()), true)));

        String key = "test:lock:" + ObjectUtils.uuid32();
        RedisLock redisLock = new RedisLock(bean(), key, 2000);
        redisLock.tryLock();

        String actualKey = "lock:" + key;
        Assertions.assertTrue(bean().hasKey(actualKey));
        Assertions.assertEquals(2000L, bean().execute((RedisCallback<Object>) conn -> conn.ttl(actualKey.getBytes(StandardCharsets.UTF_8), TimeUnit.MILLISECONDS)));

        boolean acquired = false;
        long start = System.currentTimeMillis();
        if (redisLock.tryLock(3000, TimeUnit.MILLISECONDS)) {
            acquired = true;
        }
        long cost = System.currentTimeMillis() - start;
        System.out.println("cost: " + cost);
        Assertions.assertTrue(acquired);
        Assertions.assertTrue(cost <= 2500);
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
