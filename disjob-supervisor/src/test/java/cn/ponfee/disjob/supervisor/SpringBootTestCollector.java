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

package cn.ponfee.disjob.supervisor;

import org.springframework.context.ApplicationContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Collect spring boot test info
 *
 * @author Ponfee
 */
public class SpringBootTestCollector {

    private static final ConcurrentMap<ApplicationContext, Set<Class<?>>> TEST_CLASSES_MAP = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            StringBuilder printer = new StringBuilder();
            printer.append("\n\n");
            printer.append("/*=================================Spring container & Test case=================================*\\");
            TEST_CLASSES_MAP.forEach((spring, classes) -> {
                printer.append("\n");
                printer.append(spring + ": \n");
                printer.append(classes.stream().map(e -> "---- " + e.getName()).collect(Collectors.joining("\n")));
                printer.append("\n");
            });
            printer.append("\\*=================================Spring container & Test case=================================*/");
            printer.append("\n\n");
            System.out.println(printer);
        }));
    }

    public static void collect(ApplicationContext applicationContext, Class<?> testClasses) {
        TEST_CLASSES_MAP.computeIfAbsent(applicationContext, k -> ConcurrentHashMap.newKeySet())
                        .add(testClasses);
    }

}
