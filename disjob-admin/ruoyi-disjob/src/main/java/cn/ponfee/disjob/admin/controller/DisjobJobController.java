/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.admin.controller;

import cn.ponfee.disjob.admin.domain.SchedJobExport;
import cn.ponfee.disjob.admin.util.PageUtils;
import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.core.api.supervisor.SupervisorOpenRpcService;
import cn.ponfee.disjob.core.api.supervisor.request.AddSchedJobRequest;
import cn.ponfee.disjob.core.api.supervisor.request.SchedJobPageRequest;
import cn.ponfee.disjob.core.api.supervisor.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.core.api.supervisor.response.SchedJobResponse;
import cn.ponfee.disjob.core.exception.JobCheckedException;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 调度配置Controller
 *
 * @author Ponfee
 */
@Controller
@RequestMapping("/" + DisjobJobController.PREFIX)
public class DisjobJobController extends BaseController {
    static final String PREFIX = "disjob/job";

    private static final String PERMISSION_VIEW = "disjob:job:view";
    private static final String PERMISSION_QUERY = "disjob:job:query";
    private static final String PERMISSION_OPERATE = "disjob:job:operate";

    private final SupervisorOpenRpcService supervisorOpenRpcService;

    public DisjobJobController(SupervisorOpenRpcService supervisorOpenRpcService) {
        this.supervisorOpenRpcService = supervisorOpenRpcService;
    }

    // -------------------------------------------------------查询

    @RequiresPermissions(PERMISSION_VIEW)
    @GetMapping()
    public String job() {
        return PREFIX + "/job";
    }

    /**
     * 查询调度配置列表
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SchedJobPageRequest request) {
        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        PageResponse<SchedJobResponse> response = supervisorOpenRpcService.queryJobForPage(request);
        return PageUtils.toTableDataInfo(response);
    }

    /**
     * 查看调度配置详情
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @GetMapping("/detail/{jobId}")
    public String detail(@PathVariable("jobId") Long jobId, ModelMap mmap) {
        SchedJobResponse job = supervisorOpenRpcService.getJob(jobId);
        Assert.notNull(job, () -> "Job id not found: " + jobId);
        mmap.put("job", job);
        return PREFIX + "/detail";
    }

    /**
     * 导出调度配置列表
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @Log(title = "调度配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(SchedJobPageRequest request) {
        request.setPaged(false);
        List<SchedJobResponse> rows = supervisorOpenRpcService.queryJobForPage(request).getRows();
        List<SchedJobExport> list = Collects.convert(rows, SchedJobExport::ofSchedJobResponse);
        ExcelUtil<SchedJobExport> excel = new ExcelUtil<>(SchedJobExport.class);
        return excel.exportExcel(list, "调度配置数据");
    }

    // -------------------------------------------------------操作

    /**
     * 新增调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/add")
    public String add() {
        return PREFIX + "/add";
    }

    /**
     * 新增调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "调度配置", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult doAdd(AddSchedJobRequest req) throws JobCheckedException {
        req.setCreatedBy(getLoginName());
        supervisorOpenRpcService.addJob(req);
        return success();
    }

    /**
     * 修改调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long jobId, ModelMap mmap) {
        SchedJobResponse job = supervisorOpenRpcService.getJob(jobId);
        Assert.notNull(job, () -> "Job id not found: " + jobId);
        mmap.put("job", job);
        return PREFIX + "/edit";
    }

    /**
     * 修改调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "调度配置", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult doEdit(UpdateSchedJobRequest req) throws JobCheckedException {
        req.setUpdatedBy(getLoginName());
        supervisorOpenRpcService.updateJob(req);
        return success();
    }

    /**
     * 删除调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "调度配置", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam("ids") String ids) {
        Set<Long> jobIds = Arrays.stream(ids.split(","))
            .filter(StringUtils::isNotBlank)
            .map(e -> Long.parseLong(e.trim()))
            .collect(Collectors.toSet());
        if (jobIds.isEmpty()) {
            return error("Job id不能为空");
        }
        jobIds.parallelStream().forEach(supervisorOpenRpcService::deleteJob);
        return success();
    }

    /**
     * 修改调度配置状态
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "修改调度配置状态", businessType = BusinessType.UPDATE)
    @PostMapping("/changeState")
    @ResponseBody
    public AjaxResult changeState(@RequestParam("jobId") Long jobId,
                                  @RequestParam("toState") Integer toState) {
        boolean result = supervisorOpenRpcService.changeJobState(jobId, toState);
        return toAjax(result);
    }

    /**
     * 触发执行
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "触发执行", businessType = BusinessType.OTHER)
    @PostMapping("/trigger")
    @ResponseBody
    public AjaxResult trigger(@RequestParam("jobId") Long jobId) throws JobCheckedException {
        supervisorOpenRpcService.triggerJob(jobId);
        return success();
    }

}
