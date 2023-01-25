/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.handle.impl;

import cn.ponfee.scheduler.common.base.exception.CheckedThrowing;
import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.util.Files;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.JobHandler;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.commons.lang3.SystemUtils.OS_NAME;

/**
 * Script job handler.
 *
 * @author Ponfee
 */
public class ScriptJobHandler extends JobHandler<String> {

    private static final String[] DOWNLOAD_PROTOCOL = {"http", "https", "ftp"};
    private static final String WORKER_DIR = System.getProperty("user.home") + "/scheduler/worker/scripts/";

    @Override
    public Result<String> execute(Checkpoint checkpoint) throws Exception {
        ScriptParam scriptParam = Jsons.fromJson(task.getTaskParam(), ScriptParam.class);
        Assert.notNull(scriptParam, () -> "Invalid script param: " + scriptParam);
        Assert.notNull(scriptParam.type, () -> "Script type cannot be null: " + scriptParam);
        scriptParam.type.check();
        Charset charset = Files.charset(scriptParam.charset);

        String scriptFileName = scriptParam.type.buildFileName(task().getTaskId());
        String scriptPath = prepareScriptFile(scriptParam.script, scriptFileName, charset);

        Process process = scriptParam.type.exec(scriptPath, scriptParam.envp);

        try (InputStream is = process.getInputStream(); InputStream es = process.getErrorStream()) {
            String verbose = CheckedThrowing.caught(() -> IOUtils.toString(is, charset));
            String error = CheckedThrowing.caught(() -> IOUtils.toString(es, charset));
            int code = process.waitFor();
            // process.exitValue()
            if (code == 0) {
                return Result.success(verbose);
            } else {
                return Result.failure(
                    JobCodeMsg.JOB_EXECUTE_FAILED.getCode(),
                    "Script execute failed: code=" + code + ", verbose=" + verbose + ", error=" + error
                );
            }
        }
    }

    public enum ScriptType {
        /**
         * Window command script.
         */
        CMD {
            @Override
            public void check() {
                Assert.isTrue(IS_OS_WINDOWS, () -> "Command script cannot supported os '" + OS_NAME + "'");
            }

            @Override
            public String buildFileName(long taskId) {
                return String.format("cmd_%d.bat", taskId);
            }

            @Override
            public Process exec(String scriptPath, String[] envp) throws IOException {
                return Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", scriptPath}, envp);
            }
        },
        /**
         * Window powershell script.
         */
        POWERSHELL {
            @Override
            public void check() {
                Assert.isTrue(IS_OS_WINDOWS, () -> "Powershell script cannot supported os '" + OS_NAME + "'");
            }

            @Override
            public String buildFileName(long taskId) {
                return String.format("powershell_%d.ps1", taskId);
            }

            @Override
            public Process exec(String scriptPath, String[] envp) throws IOException {
                return Runtime.getRuntime().exec(new String[]{"powershell.exe", scriptPath}, envp);
            }
        },
        /**
         * Linux/Unix shell script.
         */
        SHELL {
            @Override
            public void check() {
                Assert.isTrue(!IS_OS_WINDOWS, () -> "Shell script cannot supported windows os '" + OS_NAME + "'");
            }

            @Override
            public String buildFileName(long taskId) {
                return String.format("shell_%d.sh", taskId);
            }

            @Override
            public Process exec(String scriptPath, String[] envp) throws Exception {
                chmodFile(scriptPath);
                return Runtime.getRuntime().exec(new String[]{"/bin/sh", scriptPath}, envp);
            }
        },
        /**
         * Python script.
         */
        PYTHON {
            @Override
            public void check() {
                // No-op
            }

            @Override
            public String buildFileName(long taskId) {
                return String.format("python_%d.py", taskId);
            }

            @Override
            public Process exec(String scriptPath, String[] envp) throws Exception {
                if (!IS_OS_WINDOWS) {
                    chmodFile(scriptPath);
                }
                return Runtime.getRuntime().exec(new String[]{"python", scriptPath}, envp);
            }
        },
        ;

        public abstract void check();

        public abstract String buildFileName(long taskId);

        public abstract Process exec(String scriptPath, String[] envp) throws Exception;
    }

    @Data
    public static class ScriptParam implements Serializable {
        private static final long serialVersionUID = 4733248467785540711L;

        private ScriptType type;
        private String charset;
        private String script;
        private String[] envp;
    }

    private static String prepareScriptFile(String script, String scriptFileName, Charset charset) throws IOException {
        Assert.hasText(script, "Script source cannot be empty.");

        String scriptPath = WORKER_DIR + scriptFileName;
        File scriptFile = new File(scriptPath);
        if (scriptFile.exists()) {
            return scriptPath;
        }

        FileUtils.forceMkdirParent(scriptFile);
        if (!scriptFile.createNewFile()) {
            throw new RuntimeException("Create script file failed: " + scriptPath);
        }

        // download script from url
        if (StringUtils.startsWithAny(script, DOWNLOAD_PROTOCOL)) {
            FileUtils.copyURLToFile(new URL(script), scriptFile, 5000, 600000);
            return scriptPath;
        }

        FileUtils.write(scriptFile, script, charset);
        return scriptPath;
    }

    private static void chmodFile(String scriptPath) throws Exception {
        //Runtime.getRuntime().exec(new String[]{"/bin/chmod", "755", scriptPath}).waitFor();
        int code = new ProcessBuilder("/bin/chmod", "755", scriptPath).start().waitFor();
        Assert.state(code == 0, () -> "Chmod script file '" + scriptPath + "' failed, code: " + code);
    }

}
