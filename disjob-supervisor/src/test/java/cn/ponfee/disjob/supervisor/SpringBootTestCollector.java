/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
        TEST_CLASSES_MAP.computeIfAbsent(applicationContext, k -> ConcurrentHashMap.newKeySet()).add(testClasses);
    }

}
