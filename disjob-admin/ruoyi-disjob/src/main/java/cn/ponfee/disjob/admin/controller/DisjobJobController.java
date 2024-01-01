/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.admin.controller;

import cn.ponfee.disjob.admin.export.SchedJobExport;
import cn.ponfee.disjob.admin.util.PageUtils;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.supervisor.application.AuthorizeGroupService;
import cn.ponfee.disjob.supervisor.application.OpenapiService;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.application.request.AddSchedJobRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobPageRequest;
import cn.ponfee.disjob.supervisor.application.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedJobResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 作业配置Controller
 *
 * @author Ponfee
 */
@Controller
@RequestMapping("/" + DisjobJobController.PREFIX)
public class DisjobJobController extends BaseController {

    static final String PREFIX = "disjob/job";
    private static final String PERMISSION_JOB = "disjob:job:operate";

    private final OpenapiService openapiService;
    private final AuthorizeGroupService authorizeGroupService;

    public DisjobJobController(OpenapiService openapiService,
                               AuthorizeGroupService authorizeGroupService) {
        this.openapiService = openapiService;
        this.authorizeGroupService = authorizeGroupService;
    }

    // -------------------------------------------------------查询

    @RequiresPermissions(PERMISSION_JOB)
    @GetMapping
    public String job(ModelMap mmap) {
        mmap.put("groups", SchedGroupService.myGroups(getLoginName()));
        return PREFIX + "/job";
    }

    /**
     * 查询作业配置列表
     */
    @RequiresPermissions(PERMISSION_JOB)
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SchedJobPageRequest request) {
        request.authorizeAndTruncateGroup(getLoginName());
        if (CollectionUtils.isEmpty(request.getGroups())) {
            return PageUtils.empty();
        }

        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        PageResponse<SchedJobResponse> response = openapiService.queryJobForPage(request);
        return PageUtils.toTableDataInfo(response);
    }

    /**
     * 查看作业配置详情
     */
    @RequiresPermissions(PERMISSION_JOB)
    @GetMapping("/detail/{jobId}")
    public String detail(@PathVariable("jobId") long jobId, ModelMap mmap) {
        SchedJobResponse job = openapiService.getJob(jobId);
        AuthorizeGroupService.authorizeGroup(getLoginName(), job.getGroup());

        Assert.notNull(job, () -> "Job id not found: " + jobId);
        mmap.put("job", job);
        return PREFIX + "/detail";
    }

    /**
     * 导出作业配置列表
     */
    @RequiresPermissions(PERMISSION_JOB)
    @Log(title = "作业配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(SchedJobPageRequest request) {
        request.authorizeAndTruncateGroup(getLoginName());

        List<SchedJobExport> list;
        if (CollectionUtils.isEmpty(request.getGroups())) {
            list = Collections.emptyList();
        } else {
            request.setPaged(false);
            List<SchedJobResponse> rows = openapiService.queryJobForPage(request).getRows();
            list = Collects.convert(rows, SchedJobExport::ofSchedJobResponse);
        }

        ExcelUtil<SchedJobExport> excel = new ExcelUtil<>(SchedJobExport.class);
        return excel.exportExcel(list, "作业配置数据");
    }

    // -------------------------------------------------------操作

    /**
     * 新增作业配置
     */
    @RequiresPermissions(PERMISSION_JOB)
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        return toAdd(new SchedJobResponse(), mmap);
    }

    /**
     * 复制作业配置
     */
    @RequiresPermissions(PERMISSION_JOB)
    @GetMapping("/copy/{jobId}")
    public String copy(@PathVariable("jobId") long jobId, ModelMap mmap) {
        SchedJobResponse job = openapiService.getJob(jobId);
        AuthorizeGroupService.authorizeGroup(getLoginName(), job.getGroup());

        return toAdd(job, mmap);
    }

    /**
     * 新增作业配置
     */
    @RequiresPermissions(PERMISSION_JOB)
    @Log(title = "作业配置", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult doAdd(AddSchedJobRequest req) throws JobException {
        String user = getLoginName();
        AuthorizeGroupService.authorizeGroup(user, req.getGroup());

        req.setCreatedBy(user);
        openapiService.addJob(req);
        return success();
    }

    /**
     * 修改作业配置
     */
    @RequiresPermissions(PERMISSION_JOB)
    @GetMapping("/edit/{jobId}")
    public String edit(@PathVariable("jobId") long jobId, ModelMap mmap) {
        SchedJobResponse job = openapiService.getJob(jobId);
        AuthorizeGroupService.authorizeGroup(getLoginName(), job.getGroup());

        Assert.notNull(job, () -> "Job id not found: " + jobId);
        mmap.put("job", job);
        return PREFIX + "/edit";
    }

    /**
     * 修改作业配置
     */
    @RequiresPermissions(PERMISSION_JOB)
    @Log(title = "作业配置", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult doEdit(UpdateSchedJobRequest req) throws JobException {
        String user = getLoginName();
        authorizeGroupService.authorizeJob(user, req.getJobId());

        req.setUpdatedBy(user);
        openapiService.updateJob(req);
        return success();
    }

    /**
     * 删除作业配置
     */
    @RequiresPermissions(PERMISSION_JOB)
    @Log(title = "作业配置", businessType = BusinessType.DELETE)
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
        String user = getLoginName();

        final Executor executor = ThreadPoolExecutors.commonPool();
        jobIds.stream()
            .map(e -> CompletableFuture.runAsync(() -> doDeleteJob(user, e), executor))
            .collect(Collectors.toList())
            .forEach(CompletableFuture::join);

        return success();
    }

    /**
     * 修改作业配置状态
     */
    @RequiresPermissions(PERMISSION_JOB)
    @Log(title = "修改作业配置状态", businessType = BusinessType.UPDATE)
    @PostMapping("/changeState")
    @ResponseBody
    public AjaxResult changeState(@RequestParam("jobId") long jobId,
                                  @RequestParam("toState") Integer toState) {
        authorizeGroupService.authorizeJob(getLoginName(), jobId);

        boolean result = openapiService.changeJobState(jobId, toState);
        return toAjax(result);
    }

    /**
     * 触发执行
     */
    @RequiresPermissions(PERMISSION_JOB)
    @Log(title = "触发执行", businessType = BusinessType.OTHER)
    @PostMapping("/trigger")
    @ResponseBody
    public AjaxResult trigger(@RequestParam("jobId") long jobId) throws JobException {
        authorizeGroupService.authorizeJob(getLoginName(), jobId);

        openapiService.triggerJob(jobId);
        return success();
    }

    // -------------------------------------------------------private methods

    private String toAdd(SchedJobResponse job, ModelMap mmap) {
        mmap.put("job", job);
        mmap.put("groups", SchedGroupService.myGroups(getLoginName()));
        return PREFIX + "/add";
    }

    private void doDeleteJob(String user, long jobId) {
        authorizeGroupService.authorizeJob(user, jobId);
        openapiService.deleteJob(jobId);
    }

}
