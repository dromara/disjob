/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
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
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 添加copyright
 *
 * @author Ponfee
 */
@Disabled
public class CopyrightTest {

    private static final String OLD_COPYRIGHT_KEYWORD = " Copyright (c) 2017-2023 Ponfee ";
    private static final String NEW_COPYRIGHT_KEYWORD = " Copyright (c) 2017-2024 Ponfee ";

    private static final String BASE_DIR = MavenProjects.getProjectBaseDir();
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
                if (text.contains("** \\______   \\____   _____/ ____\\____   ____   " + OLD_COPYRIGHT_KEYWORD + " **")) {
                    Writer writer = new FileWriter(file.getAbsolutePath());
                    IOUtils.write(COPYRIGHT, writer);
                    IOUtils.write(text.substring(83 * 7 + 1), writer);
                    writer.flush();
                    writer.close();
                    return;
                }

                if (!text.contains("** \\______   \\____   _____/ ____\\____   ____   " + NEW_COPYRIGHT_KEYWORD)) {
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
                System.out.println(file.getName());
            } else if (isOwnerCode(text)) {
                // 自己编写的代码，添加Copyright
                if (!text.contains(NEW_COPYRIGHT_KEYWORD)) {
                    System.out.println(file.getName());
                }
            } else {
                // 引用他人的代码，不加Copyright
                if (text.contains(NEW_COPYRIGHT_KEYWORD)) {
                    System.out.println(file.getName());
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

    private static void handleFile(Consumer<File> consumer) {
        FileUtils
            .listFiles(new File(BASE_DIR).getParentFile(), new String[]{"java"}, true)
            .forEach(consumer);
    }

    private boolean isOwnerCode(String sourceCode) {
        if (sourceCode.contains("public class " + getClass().getSimpleName() + " {\n")) {
            // is current file: CopyrightTest.java
            return true;
        }
        return sourceCode.contains(" * @author Ponfee\n") && StringUtils.countMatches(sourceCode, " @author ") == 1;
    }

}
