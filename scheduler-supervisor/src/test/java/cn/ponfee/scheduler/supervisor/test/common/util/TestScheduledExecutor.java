package cn.ponfee.scheduler.supervisor.test.common.util;

import cn.ponfee.scheduler.common.concurrent.ThreadPoolExecutors;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestScheduledExecutor {

    @Test
    public void testSubmit() throws InterruptedException {
        ScheduledThreadPoolExecutor registryScheduledExecutor = new ScheduledThreadPoolExecutor(1, ThreadPoolExecutors.DISCARD);
        registryScheduledExecutor.scheduleAtFixedRate(() -> {
            System.out.println("scheduled");
        }, 2, 1, TimeUnit.SECONDS);


        System.out.println(new Date());
        registryScheduledExecutor.submit(() -> System.out.println("submit"));
        registryScheduledExecutor.execute(() -> System.out.println("execute"));
        System.out.println(new Date());

        Thread.sleep(5000);
    }
}
