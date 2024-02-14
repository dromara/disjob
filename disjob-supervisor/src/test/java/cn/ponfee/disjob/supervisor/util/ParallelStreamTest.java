/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor.util;

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
