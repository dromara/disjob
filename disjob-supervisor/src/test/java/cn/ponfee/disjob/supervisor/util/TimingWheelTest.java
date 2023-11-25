/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.util.Fields;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <a href="https://bugs.openjdk.org/browse/JDK-8161372">ConcurrentHashMap#computeIfAbsent阻塞的bug(要先get一次解决)</a>
 *
 * @author Ponfee
 */
public class TimingWheelTest {

    private final Map<Integer, Integer> concurrentMap = new ConcurrentHashMap<>();

    /*
    @Test // 该方法阻塞(死锁)了
    public void testComputeIfAbsent1() {
        System.out.println("Test concurrent hash map in fibonacci recurse: " + fibonacci(20));
    }

    @Test // 该方法阻塞(死锁)了：https://bugs.openjdk.org/secure/attachment/23985/Main.java
    public void testComputeIfAbsent2() {
        Map<String, Integer> map = new ConcurrentHashMap<>(16);
        map.computeIfAbsent(
            "AaAa",
            key -> map.computeIfAbsent("BBBB", k -> 42)
        );
    }
    */

    @Test
    public void testConcurrentHashMapCapt() {
        // The ConcurrentHashMap actual capacity is 128
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>(64, 1f);
        Assertions.assertEquals(0, map.size());
        Assertions.assertNull(Fields.get(map, "table"));
        for (int i = 0; i < 100; i++) {
            map.put(i, i);
            System.out.println("size: " + map.size() + ", mapping-count: " + map.mappingCount() + ", table-size: " + Array.getLength(Fields.get(map, "table")));
        }
        Assertions.assertEquals(256, Array.getLength(Fields.get(map, "table")));
    }

    @Test
    public void testHashMapCapt() {
        // The HashMap actual capacity is 64
        HashMap<Integer, Integer> map = new HashMap<>(64, 1f);
        Assertions.assertEquals(0, map.size());
        Assertions.assertNull(Fields.get(map, "table"));
        for (int i = 0; i < 60; i++) {
            map.put(i, i);
            System.out.println("size: " + map.size() + ", table-size: " + Array.getLength(Fields.get(map, "table")));
        }
        Assertions.assertEquals(64, Array.getLength(Fields.get(map, "table")));
    }

    @Test
    public void testTimeSecond() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals((int) ((System.currentTimeMillis() % 60000) / 1000), Calendar.getInstance().get(Calendar.SECOND));
            Thread.sleep(ThreadLocalRandom.current().nextLong(200));
        }
    }

    @Test
    public void testTimeHour() {
        System.out.println((System.currentTimeMillis() % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)); // 要"+8"时区
        System.out.println(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        System.out.println(LocalDateTime.now().getHour());
        Assertions.assertEquals("1970-01-01 08:00:00", Dates.format(Dates.ofTimeMillis(0L)));
        Assertions.assertEquals(LocalDateTime.now().getHour(), Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        Assertions.assertEquals(2, String.format("%02d", Calendar.getInstance().get(Calendar.HOUR_OF_DAY)).length());
        Assertions.assertEquals("00", String.format("%02d", 0));
        Assertions.assertEquals("06", String.format("%02d", 6));
        Assertions.assertEquals("10", String.format("%02d", 10));
    }

    static int round = 100000;

    @Test
    public void testRoundCurrentTimeMillis() {
        for (int second, i = 0; i < round; i++) {
            second = (int) ((System.currentTimeMillis() % 60000) / 1000);
        }
    }

    @Test
    public void testRoundCalendar() {
        for (int second, i = 0; i < round; i++) {
            second = Calendar.getInstance().get(Calendar.SECOND);
        }
    }

    @Test
    public void testAtomicBoolean() {
        // 24天
        System.out.println(TimeUnit.MILLISECONDS.toDays(Integer.MAX_VALUE));
        Assertions.assertFalse(Calendar.getInstance() == Calendar.getInstance());

        Date date = new Date();
        int second = (int) ((date.getTime() % 60000) / 1000);
        System.out.println("second: " + second);
        Assertions.assertEquals((int) ((date.getTime() % 60000) / 1000), Dates.toLocalDateTime(date).getSecond());

        long currentTimeMillis = System.currentTimeMillis();
        long maxTiming = (System.currentTimeMillis() / 1000) * 1000 + 999;
        System.out.println("currentTimeMillis: " + currentTimeMillis);
        System.out.println("maxTiming: " + maxTiming);

        AtomicBoolean exclusive = new AtomicBoolean(false);
        Assertions.assertTrue(exclusive.compareAndSet(false, true));
        Assertions.assertFalse(exclusive.compareAndSet(false, true));
    }

    @Test
    public void testTreeMap() {
        TreeMap<Integer, String> treeMap = new TreeMap<>(/*Comparator.reverseOrder()*/);
        //System.out.println(treeMap.firstKey()); // NPE
        System.out.println(treeMap.firstEntry());
        treeMap.put(5, UuidUtils.uuid32());
        treeMap.put(5, UuidUtils.uuid32());
        treeMap.put(5, UuidUtils.uuid32());
        for (int i = 0; i < 10; i++) {
            treeMap.put(ThreadLocalRandom.current().nextInt(20), UuidUtils.uuid32());
        }
        treeMap.forEach((k, v) -> System.out.println(k + " -> " + v));
        System.out.println("-------------\n");
        int size = treeMap.size(), count = 0;
        while (treeMap.firstKey() < 8) {
            count++;
            Map.Entry<Integer, String> entry = treeMap.pollFirstEntry();
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        Assertions.assertEquals(size, count + treeMap.size());
        System.out.println("-------------\n");
        treeMap.forEach((k, v) -> System.out.println(k + " -> " + v));
    }

    @Test
    public void testTimingQueue() {
        TimingQueue<ExecuteTaskParam> timingQueue = new TimingQueue<>();
        for (int i = 0; i < 100; i++) {
            long triggerTime = ThreadLocalRandom.current().nextLong(100);
            timingQueue.offer(CommonTest.createExecuteTaskParam(Operations.TRIGGER, 0, 0, 1L, 0, triggerTime, JobType.GENERAL, RouteStrategy.ROUND_ROBIN, 1, "jobHandler", new Worker("default", "workerId", "host", 1)));
        }

        System.out.println("-------------\n");
        Assertions.assertEquals(100, timingQueue.size());
        ExecuteTaskParam first = timingQueue.peek();
        for (ExecuteTaskParam e; (e = timingQueue.poll()) != null; ) {
            System.out.print(e.timing() + ", ");
            Assertions.assertEquals(first, e);
            first = timingQueue.peek();
        }
        Assertions.assertEquals(0, timingQueue.size());
        Assertions.assertTrue(timingQueue.isEmpty());
    }

    @Test
    public void testTimingWheel() {
        long tickMs = 100;
        int ringSize = 60;
        long msPerRound = tickMs * ringSize;
        TimingWheel<ExecuteTaskParam> timingWheel = new TimingWheel<ExecuteTaskParam>(tickMs, ringSize) {};
        long hour = TimeUnit.HOURS.toMillis(10);
        System.out.println("hour=" + hour);

        for (int i = 0; i < 100000; i++) {
            long triggerTime = System.currentTimeMillis() + 5000 + ThreadLocalRandom.current().nextLong(hour);
            timingWheel.offer(CommonTest.createExecuteTaskParam(Operations.TRIGGER, 0, 0, 1L, 0, triggerTime, JobType.GENERAL, RouteStrategy.ROUND_ROBIN, 1, "jobHandler", new Worker("default", "workerId", "host", 1)));
        }
    }

    private int fibonacci(int i) {
        if (i == 0) {
            return i;
        }
        if (i == 1) {
            return 1;
        }
        return concurrentMap.computeIfAbsent(i, (key) -> {
            System.out.println("Value: " + key);
            return fibonacci(i - 2) + fibonacci(i - 1);
        });
    }

    private static final class TimingQueue<T extends TimingWheel.Timing<T>> extends PriorityQueue<T> {
        @Override
        public synchronized T poll() {
            return super.poll();
        }

        @Override
        public synchronized boolean offer(T timing) {
            return super.offer(timing);
        }
    }
}
