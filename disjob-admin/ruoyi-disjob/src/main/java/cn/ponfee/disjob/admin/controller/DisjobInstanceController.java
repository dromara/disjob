/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.admin.controller;

import cn.ponfee.disjob.admin.util.PageUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.SleepWaitUtils;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.supervisor.provider.openapi.request.SchedInstancePageRequest;
import cn.ponfee.disjob.supervisor.provider.openapi.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.provider.openapi.response.SchedTaskResponse;
import cn.ponfee.disjob.supervisor.service.SupervisorOpenapiService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务实例Controller
 *
 * @author Ponfee
 */
@Controller
@RequestMapping("/" + DisjobInstanceController.PREFIX)
public class DisjobInstanceController extends BaseController {

    static final String PREFIX = "disjob/instance";
    private static final String PERMISSION_VIEW = "disjob:instance:view";
    private static final String PERMISSION_QUERY = "disjob:instance:query";
    private static final String PERMISSION_OPERATE = "disjob:instance:operate";
    private static final int WAIT_SLEEP_ROUND = 9;
    private static final long[] WAIT_SLEEP_MILLIS = {2500, 500};

    private final SupervisorOpenapiService supervisorOpenapiService;

    public DisjobInstanceController(SupervisorOpenapiService supervisorOpenapiService) {
        this.supervisorOpenapiService = supervisorOpenapiService;
    }

    @RequiresPermissions(PERMISSION_VIEW)
    @GetMapping
    public String instance() {
        return PREFIX + "/instance";
    }

    /**
     * 查询任务实例列表-tree
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @PostMapping("/tree")
    @ResponseBody
    public TableDataInfo tree(SchedInstancePageRequest request) {
        request.setParent(true);
        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        return PageUtils.toTableDataInfo(supervisorOpenapiService.queryInstanceForPage(request));
    }

    /**
     * 查询任务实例列表-flat
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @PostMapping("/flat")
    @ResponseBody
    public TableDataInfo flat(SchedInstancePageRequest request) {
        request.setParent(false);
        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        return PageUtils.toTableDataInfo(supervisorOpenapiService.queryInstanceForPage(request));
    }

    @RequiresPermissions(PERMISSION_QUERY)
    @PostMapping("/children")
    @ResponseBody
    public List<SchedInstanceResponse> children(@RequestParam("pnstanceId") Long pnstanceId) {
        return supervisorOpenapiService.listInstanceChildren(pnstanceId);
    }

    /**
     * Date等类型序列化会使用toString():
     * <pre>{@code
     *  mmap.put("tasks", tasks);
     *  data: [[${tasks}]]
     * }</pre>
     *
     * 使用Json方式序列化:
     * <pre>{@code
     *  mmap.put("tasks", Jsons.toJson(tasks));
     *  data: [[${list}]]
     * }</pre>
     *
     * @param instanceId the instance id
     * @param mmap       the mmap
     * @return html page path
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @GetMapping("/tasks/{instanceId}")
    public String tasks(@PathVariable("instanceId") Long instanceId, ModelMap mmap) {
        List<SchedTaskResponse> tasks = supervisorOpenapiService.getInstanceTasks(instanceId);
        mmap.put("tasks", Jsons.toJson(tasks));
        return PREFIX + "/tasks";
    }

    // -----------------------------------------------------------操作

    /**
     * 删除任务实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "删除任务实例", businessType = BusinessType.DELETE)
    @PostMapping("/remove/{instanceId}")
    @ResponseBody
    public AjaxResult remove(@PathVariable("instanceId") Long instanceId) {
        supervisorOpenapiService.deleteInstance(instanceId);
        return success();
    }

    /**
     * 暂停任务实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "暂停任务实例", businessType = BusinessType.UPDATE)
    @PostMapping("/pause/{instanceId}")
    @ResponseBody
    public AjaxResult pause(@PathVariable("instanceId") Long instanceId) {
        supervisorOpenapiService.pauseInstance(instanceId);
        SleepWaitUtils.waitUntil(WAIT_SLEEP_ROUND, WAIT_SLEEP_MILLIS, () -> {
            SchedInstanceResponse instance = supervisorOpenapiService.getInstance(instanceId, false);
            return !RunState.PAUSABLE_LIST.contains(RunState.of(instance.getRunState()));
        });
        return success();
    }

    /**
     * 恢复任务实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "恢复任务实例", businessType = BusinessType.UPDATE)
    @PostMapping("/resume/{instanceId}")
    @ResponseBody
    public AjaxResult resume(@PathVariable("instanceId") Long instanceId) {
        supervisorOpenapiService.resumeInstance(instanceId);
        SleepWaitUtils.waitUntil(WAIT_SLEEP_ROUND, new long[]{500, 200}, () -> {
            SchedInstanceResponse instance = supervisorOpenapiService.getInstance(instanceId, false);
            return !RunState.PAUSED.equals(instance.getRunState());
        });
        return success();
    }

    /**
     * 取消任务实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "取消任务实例", businessType = BusinessType.UPDATE)
    @PostMapping("/cancel/{instanceId}")
    @ResponseBody
    public AjaxResult cancel(@PathVariable("instanceId") Long instanceId) {
        supervisorOpenapiService.cancelInstance(instanceId);
        SleepWaitUtils.waitUntil(WAIT_SLEEP_ROUND, WAIT_SLEEP_MILLIS, () -> {
            SchedInstanceResponse instance = supervisorOpenapiService.getInstance(instanceId, false);
            return RunState.of(instance.getRunState()).isTerminal();
        });
        return success();
    }

}
