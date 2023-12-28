/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle.impl;

import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.ProcessUtils;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.exception.JobRuntimeException;
import cn.ponfee.disjob.core.handle.ExecuteResult;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.handle.Savepoint;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import cn.ponfee.disjob.core.util.JobUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.commons.lang3.SystemUtils.OS_NAME;

/**
 * Script job handler.
 * <p>
 *
 * <pre>job_param example: {@code
 *  {
 *    "type": "SHELL",
 *    "script":'
 *      #!/bin/sh
 *      echo "hi\'\\" sh!"
 *    '
 *  }
 * }</pre>
 *
 * @author Ponfee
 */
public class ScriptJobHandler extends JobHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptJobHandler.class);

    private static final String[] DOWNLOAD_PROTOCOL = {"http://", "https://", "ftp://"};
    private static final String WORKER_SCRIPT_DIR = SystemUtils.USER_HOME + "/disjob/worker/scripts/";

    private Charset charset;
    private Long pid;

    @Override
    protected void onStop() {
        if (pid != null) {
            ProcessUtils.killProcess(pid, charset);
        }
    }

    @Override
    public ExecuteResult execute(ExecutingTask executingTask, Savepoint savepoint) throws Exception {
        ScriptParam scriptParam = Jsons.JSON5.readValue(executingTask.getTaskParam(), ScriptParam.class);
        Assert.notNull(scriptParam, () -> "Invalid script param: " + scriptParam);
        Assert.notNull(scriptParam.type, () -> "Script type cannot be null: " + scriptParam);
        scriptParam.type.check();
        this.charset = Files.charset(scriptParam.charset);

        String scriptFileName = scriptParam.type.buildFileName(executingTask.getTaskId());
        String scriptPath = prepareScriptFile(scriptParam.script, scriptFileName, charset);

        Process process = scriptParam.type.exec(scriptPath, scriptParam.envp);
        this.pid = ProcessUtils.getProcessId(process);
        LOG.info("Script process id: {}, {}", executingTask.getTaskId(), pid);
        return JobUtils.completeProcess(process, charset, executingTask, LOG);
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

    @Getter
    @Setter
    public static class ScriptParam implements Serializable {
        private static final long serialVersionUID = 4733248467785540711L;

        private ScriptType type;
        private String charset;
        private String script;
        private String[] envp;
    }

    private static String prepareScriptFile(String script, String scriptFileName, Charset charset) throws IOException {
        Assert.hasText(script, "Script source cannot be empty.");
        String scriptPath = WORKER_SCRIPT_DIR + scriptFileName;

        File scriptFile = new File(scriptPath);
        if (scriptFile.exists()) {
            return scriptPath;
        }

        FileUtils.forceMkdirParent(scriptFile);
        if (!scriptFile.createNewFile()) {
            throw new JobRuntimeException(JobCodeMsg.JOB_EXECUTE_ERROR, "Create script file failed: " + scriptPath);
        }

        // download script from url
        if (StringUtils.startsWithAny(script, DOWNLOAD_PROTOCOL)) {
            // read-timeout: 10 minutes
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
