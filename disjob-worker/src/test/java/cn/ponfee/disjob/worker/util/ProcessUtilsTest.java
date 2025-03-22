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

package cn.ponfee.disjob.worker.util;

import cn.ponfee.disjob.common.tuple.Tuple4;
import cn.ponfee.disjob.common.util.MavenProjects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.exec.*;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ProcessUtils test
 *
 * @author Ponfee
 */
@Disabled
public class ProcessUtilsTest {

    // `$PATH:$JAVA_HOME/bin`：commons-exec可以使用这种方式，而Process则会报错
    private static final Map<String, String> JDK_ENV = ImmutableMap.of(
        //"JAVA_HOME", "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home",
        //"PATH", "$JAVA_HOME/bin"
    );

    @Test
    public void testJavaVersion() throws IOException {
        execute("bash", "-c", "java -version");
    }

    @Test
    public void testMavenPackage() throws IOException {
        //String pomFile = "pom.xml";
        //String pomFile = "disjob-samples/pom.xml";
        String pomFile = "disjob-admin/pom.xml";

        String rootPath = new File(MavenProjects.getProjectBaseDir()).getParentFile().getAbsolutePath() + "/";
        String installCmd = "bash " + rootPath + "mvnw clean package -DskipTests -U -f " + rootPath + pomFile;
        execute(installCmd.split("\\s+"));
    }

    @Test
    public void testConflictedJarVersion() throws IOException {
        String pomFile = "pom.xml";
        //String pomFile = "disjob-samples/pom.xml";
        //String pomFile = "disjob-admin/pom.xml";

        String rootPath = new File(MavenProjects.getProjectBaseDir()).getParentFile().getAbsolutePath() + "/";
        // -B: Run in non-interactive (batch) mode (disables output color)
        // -q: 安静模式，只输出ERROR
        String treeCmd = "bash " + rootPath + "mvnw dependency:tree -B -f " + rootPath + pomFile;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Stopwatch stopwatch = Stopwatch.createStarted();
        executeByCommonsExec(treeCmd, output);
        stopwatch.stop();

        String dependencyTree = output.toString(StandardCharsets.UTF_8.name());
        String line = "───────────────────────────────────────────────────────────────────────────────";
        System.out.println("\n\n");
        System.out.println(dependencyTree);
        System.out.println("\n\n");
        System.out.println(line);
        System.out.println("Total time: " + stopwatch);
        System.out.println(line);
        System.out.println("Conflict jar version");
        System.out.println(line);
        String result = parseConflictedJarVersion(dependencyTree);
        if (StringUtils.isBlank(result)) {
            System.out.println("Not conflicted version jar");
        } else {
            System.out.println(result);
        }
        System.out.println(line);
    }

    // ------------------------------------------------------------------------------private methods

    private static void execute(String... cmdarray) throws IOException {
        System.out.println("\n\n");

        System.out.println("-------------------------------------------------------executeByRuntimeProcess");
        executeByRuntimeProcess(cmdarray);
        System.out.println("\n");

        System.out.println("-------------------------------------------------------executeByProcessBuilder");
        executeByProcessBuilder(cmdarray);
        System.out.println("\n");

        System.out.println("-------------------------------------------------------executeByCommonsExec");
        String command = String.join(" ", cmdarray).replaceFirst("^((bash -c )|(sh -c ))", "");
        executeByCommonsExec(command, System.out);

        System.out.println("\n\n");
    }

    private static void executeByRuntimeProcess(String... cmdarray) throws IOException {
        String[] envp = null;
        if (MapUtils.isNotEmpty(JDK_ENV)) {
            Map<String, String> environment = new HashMap<>(System.getenv());
            ProcessUtils.mergeEnv(environment, JDK_ENV);
            envp = environment.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
        }

        // envp设置为null表示不修改，否则得把全部env设置进去才可以（不然会报错）
        Process process = Runtime.getRuntime().exec(cmdarray, envp);
        Long pid = ProcessUtils.getProcessId(process);
        System.out.println("Process id: " + pid);
        ProcessUtils.progress(process, StandardCharsets.UTF_8);
    }

    private static void executeByProcessBuilder(String... cmdarray) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        ProcessUtils.mergeEnv(processBuilder.environment(), JDK_ENV);
        processBuilder.command(cmdarray);

        Process process = processBuilder.start();
        Long pid = ProcessUtils.getProcessId(process);
        System.out.println("Process id: " + pid);
        ProcessUtils.progress(process, StandardCharsets.UTF_8);
    }

    private static void executeByCommonsExec(String command, OutputStream output) throws IOException {
        Executor executor = DefaultExecutor.builder().get();
        executor.setStreamHandler(new PumpStreamHandler(output));
        executor.setWatchdog(ExecuteWatchdog.builder().setTimeout(Duration.ofSeconds(300)).get());

        Map<String, String> environment = null;
        if (MapUtils.isNotEmpty(JDK_ENV)) {
            environment = EnvironmentUtils.getProcEnvironment();
            ProcessUtils.mergeEnv(environment, JDK_ENV);
        }

        executor.execute(CommandLine.parse(command), environment);
    }

    private static String parseConflictedJarVersion(String text) {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(text.split("\n"))
            .filter(e -> StringUtils.startsWithAny(e, "[INFO] +- ", "[INFO] |  "))
            .map(s -> s.replaceAll("^\\[INFO] ", "").replaceAll("^\\W+", "").trim())
            .map(s -> s.split(":"))
            .filter(e -> /*e.length >= 5 && */!"test".equals(e[4]))
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

}
