package cn.ponfee.scheduler.supervisor.test.common.util;

import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * @author Ponfee
 */
public class TreeMapTest {

    @Test
    @Ignore
    public void testTreeMap() {
        Map<String, String> executing = new TreeMap<>(); // TreeMap HashMap
        IntStream.range(0, 256)
            .mapToObj(i -> String.format("%03d", i))
            .forEach(e -> executing.put(e, "v-" + e));
        Map<String, String> finished = new TreeMap<>();

        System.out.println("-----------------before");
        int beforeSize = executing.size();
        String beforeFull = JSON.toJSONString(executing);
        String beforeEmpty = JSON.toJSONString(finished);
        System.out.println(beforeFull);
        System.out.println(beforeEmpty);
        for (int i = 0; !executing.isEmpty(); i++) {
            for (Iterator<Map.Entry<String, String>> iter = executing.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<String, String> shard = iter.next();
                String value = shard.getValue();
                if (ThreadLocalRandom.current().nextInt(9) < 3) {
                    String s1 = shard.getKey();
                    iter.remove();
                    finished.put(shard.getKey(), value);
                    //finished.put(s1, value);
                    String s2 = shard.getKey();
                    System.out.println("remove_before_and_after: " + s1 + " -> " + s2);
                }
            }
        }

        System.out.println("\n\n-----------------after");
        String afterFull = JSON.toJSONString(finished);
        String afterEmpty = JSON.toJSONString(executing);
        System.out.println(afterFull);
        System.out.println(afterEmpty);

        int afterSize = finished.size();
        Assert.assertEquals(beforeEmpty, afterEmpty);
        Assert.assertEquals(beforeSize, afterSize);
        Assert.assertEquals(beforeFull, afterFull);
    }


}
