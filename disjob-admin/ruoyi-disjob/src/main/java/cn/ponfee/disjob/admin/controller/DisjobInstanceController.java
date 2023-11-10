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
import cn.ponfee.disjob.common.util.WaitForProcess;
import cn.ponfee.disjob.core.api.supervisor.SupervisorOpenRpcService;
import cn.ponfee.disjob.core.api.supervisor.request.SchedInstancePageRequest;
import cn.ponfee.disjob.core.api.supervisor.response.SchedInstanceResponse;
import cn.ponfee.disjob.core.api.supervisor.response.SchedTaskResponse;
import cn.ponfee.disjob.core.enums.RunState;
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
 * 调度实例Controller
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

    private final SupervisorOpenRpcService supervisorOpenRpcService;

    public DisjobInstanceController(SupervisorOpenRpcService supervisorOpenRpcService) {
        this.supervisorOpenRpcService = supervisorOpenRpcService;
    }

    @RequiresPermissions(PERMISSION_VIEW)
    @GetMapping
    public String instance() {
        return PREFIX + "/instance";
    }

    /**
     * 查询调度实例列表-tree
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @PostMapping("/tree")
    @ResponseBody
    public TableDataInfo tree(SchedInstancePageRequest request) {
        request.setParent(true);
        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        return PageUtils.toTableDataInfo(supervisorOpenRpcService.queryInstanceForPage(request));
    }

    /**
     * 查询调度实例列表-flat
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @PostMapping("/flat")
    @ResponseBody
    public TableDataInfo flat(SchedInstancePageRequest request) {
        request.setParent(false);
        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        return PageUtils.toTableDataInfo(supervisorOpenRpcService.queryInstanceForPage(request));
    }

    @RequiresPermissions(PERMISSION_QUERY)
    @PostMapping("/children")
    @ResponseBody
    public List<SchedInstanceResponse> children(@RequestParam("pnstanceId") Long pnstanceId) {
        return supervisorOpenRpcService.listInstanceChildren(pnstanceId);
    }

    @RequiresPermissions(PERMISSION_QUERY)
    @GetMapping("/tasks/{instanceId}")
    public String tasks(@PathVariable("instanceId") Long instanceId, ModelMap mmap) {
        List<SchedTaskResponse> tasks = supervisorOpenRpcService.getInstanceTasks(instanceId);
        mmap.put("tasks", Jsons.toJson(tasks));
        return PREFIX + "/tasks";
    }

    // -----------------------------------------------------------操作

    /**
     * 删除调度实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "删除调度实例", businessType = BusinessType.DELETE)
    @PostMapping("/remove/{instanceId}")
    @ResponseBody
    public AjaxResult remove(@PathVariable("instanceId") Long instanceId) {
        supervisorOpenRpcService.deleteInstance(instanceId);
        return success();
    }

    /**
     * 暂停调度实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "暂停调度实例", businessType = BusinessType.UPDATE)
    @PostMapping("/pause/{instanceId}")
    @ResponseBody
    public AjaxResult pause(@PathVariable("instanceId") Long instanceId) {
        supervisorOpenRpcService.pauseInstance(instanceId);
        WaitForProcess.process(WAIT_SLEEP_ROUND, WAIT_SLEEP_MILLIS, () -> {
            SchedInstanceResponse instance = supervisorOpenRpcService.getInstance(instanceId, false);
            return !RunState.PAUSABLE_LIST.contains(RunState.of(instance.getRunState()));
        });
        return success();
    }

    /**
     * 恢复调度实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "恢复调度实例", businessType = BusinessType.UPDATE)
    @PostMapping("/resume/{instanceId}")
    @ResponseBody
    public AjaxResult resume(@PathVariable("instanceId") Long instanceId) {
        supervisorOpenRpcService.resumeInstance(instanceId);
        WaitForProcess.process(WAIT_SLEEP_ROUND, new long[]{500, 200}, () -> {
            SchedInstanceResponse instance = supervisorOpenRpcService.getInstance(instanceId, false);
            return !RunState.PAUSED.equals(instance.getRunState());
        });
        return success();
    }

    /**
     * 取消调度实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "取消调度实例", businessType = BusinessType.UPDATE)
    @PostMapping("/cancel/{instanceId}")
    @ResponseBody
    public AjaxResult cancel(@PathVariable("instanceId") Long instanceId) {
        supervisorOpenRpcService.cancelInstance(instanceId);
        WaitForProcess.process(WAIT_SLEEP_ROUND, WAIT_SLEEP_MILLIS, () -> {
            SchedInstanceResponse instance = supervisorOpenRpcService.getInstance(instanceId, false);
            return RunState.of(instance.getRunState()).isTerminal();
        });
        return success();
    }

}
