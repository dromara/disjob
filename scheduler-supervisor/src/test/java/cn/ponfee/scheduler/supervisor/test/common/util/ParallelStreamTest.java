package cn.ponfee.scheduler.supervisor.test.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * @author Ponfee
 */
public class ParallelStreamTest {

    @Test
    public void test() {
        String mainThread = Thread.currentThread().getName();
        System.out.println(mainThread);
        System.out.println();
        System.out.println("=================================");
        System.out.println("Using Sequential Stream");
        System.out.println("=================================");
        int[] array = {1};
        int length = array.length;
        IntStream intArrStream = Arrays.stream(array);
        intArrStream.forEach(s -> System.out.println(s + " " + Thread.currentThread().getName()));

        System.out.println("\n");

        System.out.println("=================================");
        System.out.println("Using Parallel Stream");
        System.out.println("=================================");
        IntStream intParallelStream = Arrays.stream(array).parallel();
        intParallelStream.forEach(s ->
            {
                System.out.println(s + " " + Thread.currentThread().getName());
                if (length == 1) {
                    Assertions.assertEquals(Thread.currentThread().getName(), mainThread);
                }
            }
        );
    }
}
