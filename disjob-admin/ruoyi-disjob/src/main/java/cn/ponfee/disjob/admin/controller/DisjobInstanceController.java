package cn.ponfee.disjob.admin.controller;

import cn.ponfee.disjob.admin.util.DisjobUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.openapi.supervisor.SupervisorOpenapi;
import cn.ponfee.disjob.core.openapi.supervisor.request.SchedInstancePageRequest;
import cn.ponfee.disjob.core.openapi.supervisor.response.SchedInstanceResponse;
import cn.ponfee.disjob.core.openapi.supervisor.response.SchedTaskResponse;
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
 * @author ponfee
 */
@Controller
@RequestMapping("/" + DisjobInstanceController.PREFIX)
public class DisjobInstanceController extends BaseController {

    static final String PREFIX = "disjob/instance";
    private static final String PERMISSION_VIEW = "disjob:instance:view";
    private static final String PERMISSION_QUERY = "disjob:instance:query";
    private static final String PERMISSION_OPERATE = "disjob:instance:operate";

    private final SupervisorOpenapi supervisorOpenapi;

    public DisjobInstanceController(SupervisorOpenapi supervisorOpenapi) {
        this.supervisorOpenapi = supervisorOpenapi;
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
        request.setPageNumber(DisjobUtils.getPageNumberParameter());
        request.setPageSize(DisjobUtils.getPageSizeParameter());
        return DisjobUtils.toTableDataInfo(supervisorOpenapi.queryInstanceForPage(request));
    }

    /**
     * 查询调度实例列表-flat
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @PostMapping("/flat")
    @ResponseBody
    public TableDataInfo flat(SchedInstancePageRequest request) {
        request.setParent(false);
        request.setPageNumber(DisjobUtils.getPageNumberParameter());
        request.setPageSize(DisjobUtils.getPageSizeParameter());
        return DisjobUtils.toTableDataInfo(supervisorOpenapi.queryInstanceForPage(request));
    }

    @RequiresPermissions(PERMISSION_QUERY)
    @PostMapping("/children")
    @ResponseBody
    public List<SchedInstanceResponse> children(@RequestParam("pnstanceId") Long pnstanceId) {
        return supervisorOpenapi.listInstanceChildren(pnstanceId);
    }

    @RequiresPermissions(PERMISSION_QUERY)
    @GetMapping("/tasks/{instanceId}")
    public String tasks(@PathVariable("instanceId") Long instanceId, ModelMap mmap) {
        List<SchedTaskResponse> tasks = supervisorOpenapi.getInstanceTasks(instanceId);
        mmap.put("tasks", Jsons.toJson(tasks));
        return PREFIX + "/tasks";
    }

    // -----------------------------------------------------------操作

    /**
     * 删除调度实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "删除调度实例", businessType = BusinessType.DELETE)
    @RequestMapping(value = "/remove/{instanceId}", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public AjaxResult remove(@PathVariable("instanceId") Long instanceId) {
        supervisorOpenapi.deleteInstance(instanceId);
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
        return toAjax(supervisorOpenapi.pauseInstance(instanceId));
    }

    /**
     * 恢复调度实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "恢复调度实例", businessType = BusinessType.UPDATE)
    @PostMapping("/resume/{instanceId}")
    @ResponseBody
    public AjaxResult resume(@PathVariable("instanceId") Long instanceId) {
        return toAjax(supervisorOpenapi.resumeInstance(instanceId));
    }

    /**
     * 取消调度实例
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "取消调度实例", businessType = BusinessType.UPDATE)
    @PostMapping("/cancel/{instanceId}")
    @ResponseBody
    public AjaxResult cancel(@PathVariable("instanceId") Long instanceId) {
        return toAjax(supervisorOpenapi.cancelInstance(instanceId));
    }

}
