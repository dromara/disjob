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

import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 添加copyright
 *
 * @author Ponfee
 */
@Disabled
public class CopyrightTest {

    public static final String CURRENT_FILE_CLASS = "\npublic class " + CopyrightTest.class.getSimpleName() + " {\n";
    private static final String OLD_COPYRIGHT_KEYWORD = "\n * Copyright 2022-2023 Ponfee (http://www.ponfee.cn/)\n";
    private static final String NEW_COPYRIGHT_KEYWORD = "\n * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)\n";

    private static final String BASE_DIR = MavenProjects.getProjectBaseDir(); // + "/src/test/java/cn/ponfee/disjob/common/spring/a";
    private static final String COPYRIGHT = ThrowingSupplier.doChecked(
        () -> IOUtils.resourceToString("copy-right.txt", UTF_8, CopyrightTest.class.getClassLoader())
    );

    @Test
    public void upsertCopyright() {
        handleFile(file -> {
            String text = ThrowingSupplier.doChecked(() -> IOUtils.toString(file.toURI(), UTF_8));
            if (!isOwnerCode(text)) {
                return;
            }
            try {
                if (text.contains(OLD_COPYRIGHT_KEYWORD)) {
                    Writer writer = new FileWriter(file.getAbsolutePath());
                    IOUtils.write(COPYRIGHT, writer);
                    IOUtils.write(text.substring(621), writer);
                    writer.flush();
                    writer.close();
                    return;
                }

                if (!text.contains(NEW_COPYRIGHT_KEYWORD)) {
                    Writer writer = new FileWriter(file.getAbsolutePath());
                    IOUtils.write(COPYRIGHT, writer);
                    IOUtils.write(text, writer);
                    writer.flush();
                    writer.close();
                }
            } catch (IOException e) {
                ExceptionUtils.rethrow(e);
            }
        });
    }

    @Test
    public void checkCopyright() {
        handleFile(file -> {
            String text = ThrowingSupplier.doChecked(() -> IOUtils.toString(file.toURI(), UTF_8));
            if (StringUtils.countMatches(text, " @author ") == 0) {
                System.out.println("No author: " + file.getName());
            } else if (isOwnerCode(text)) {
                // 自己编写的代码，添加Copyright
                if (!text.contains(NEW_COPYRIGHT_KEYWORD)) {
                    System.out.println("No copyright: " + file.getName());
                }
            } else {
                // 引用他人的代码，不加Copyright
                if (text.contains(NEW_COPYRIGHT_KEYWORD)) {
                    System.out.println("Mistake copyright: " + file.getName());
                }
            }
        });
    }

    @Test
    public void testNoCopyright() {
        handleFile(file -> {
            String text = ThrowingSupplier.doChecked(() -> IOUtils.toString(file.toURI(), UTF_8));
            if (!text.contains(NEW_COPYRIGHT_KEYWORD)) {
                System.out.println(file.getName());
            }
        });
    }

    // ---------------------------------------------------------------------------------

    private static void handleFile(Consumer<File> consumer) {
        FileUtils
            .listFiles(new File(BASE_DIR).getParentFile(), new String[]{"java"}, true)
            .stream()
            .filter(e -> !IGNORED_FILES.contains(e.getName()))
            .forEach(consumer);
    }

    private boolean isOwnerCode(String sourceCode) {
        if (sourceCode.contains(CURRENT_FILE_CLASS)) {
            // is current file: CopyrightTest.java
            return true;
        }
        return sourceCode.contains("\n * @author Ponfee\n") && StringUtils.countMatches(sourceCode, " @author ") == 1;
    }

    private static final Set<String> IGNORED_FILES = ImmutableSet.of(
        "SchedJobConverterImpl.java",
        "ServerMetricsConverterImpl.java",
        "SchedGroupConverterImpl.java"
    );

}
