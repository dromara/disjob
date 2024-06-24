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

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.tuple.Tuple4;
import org.apache.commons.exec.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Maven dependency test
 *
 * @author Ponfee
 */
public class MavenDependencyConflictTest {

    @Disabled
    @Test
    public void testConflictedVersion() {
        StopWatch stopWatch = StopWatch.createStarted();
        String dependencyTree = dependencyTree();
        stopWatch.stop();

        String line = "───────────────────────────────────────────────────────────────────────────────";

        System.out.println("\n\n");
        System.out.println(dependencyTree);
        System.out.println("\n\n");
        System.out.println(line);
        System.out.println("Total time: " + stopWatch);
        System.out.println(line);
        System.out.println("Conflict jar version");
        System.out.println(line);
        String result = parseConflictedVersionJar(dependencyTree);
        if (StringUtils.isBlank(result)) {
            System.out.println("Not conflicted version jar");
        } else {
            System.out.println(result);
        }
        System.out.println(line);
    }

    private static String dependencyTree() {
        String path = new File(MavenProjects.getProjectBaseDir()).getParentFile().getAbsolutePath() + "/";
        String installCmd = "bash " + path + "mvnw clean install -DskipTests -U -f " + path + "pom.xml";

        // String treeCmd = "mvn dependency:tree -f " + path + "pom.xml";
        // -B: Run in non-interactive (batch) mode (disables output color)
        // -q: 安静模式，只输出ERROR
        String treeCmd = "bash " + path + "mvnw -B dependency:tree -f " + path + "pom.xml";
        try {
            execute(installCmd);
            return execute(treeCmd);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    private static String parseConflictedVersionJar(String text) {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(text.split("\n"))
            .filter(e -> StringUtils.startsWithAny(e, "[INFO] +- ", "[INFO] |  "))
            .map(s -> s.replaceAll("^\\[INFO] ", "").replaceAll("^\\W+", "").trim())
            .map(s -> s.split(":"))
            .filter(e -> e.length >= 4)
            .map(e -> Tuple4.of(e[0], e[1], e[2], e[3]))
            .distinct()
            .collect(Collectors.groupingBy(e -> e.a + ":" + e.b))
            .entrySet()
            .stream()
            .filter(e -> e.getValue().size() > 1)
            .filter(e -> e.getValue().stream().noneMatch(x -> StringUtils.endsWithAny(x.d, "-x86_64", "-aarch_64")))
            .forEach(e -> {
                builder.append(e.getKey()).append("\n");
                e.getValue().forEach(x -> builder.append("    " + x.a + ":" + x.b + ":" + x.c + ":" + x.d + "\n"));
            });
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    private static String execute(String command) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(out));
        executor.setWatchdog(new ExecuteWatchdog(120000L));
        executor.execute(CommandLine.parse(command));
        return out.toString(StandardCharsets.UTF_8.name());
    }

}
