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

package cn.ponfee.disjob.admin.controller;

import cn.ponfee.disjob.admin.base.Pagination;
import cn.ponfee.disjob.admin.base.SchedJobExport;
import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.MultithreadExecutors;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.supervisor.application.AuthorizeGroupService;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.application.SchedJobService;
import cn.ponfee.disjob.supervisor.application.request.SchedJobAddRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobPageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobUpdateRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.dag.DAGUtils;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.file.ImageUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 作业配置Controller
 *
 * @author Ponfee
 */
@Controller
@RequestMapping("/" + DisjobJobController.PREFIX)
public class DisjobJobController extends BaseController {

    static final String PREFIX = "disjob/job";
    private static final int MAX_TEXT_LENGTH = 900;
    private static final String PERMISSION_CODE = "disjob:job:operate";

    private final SchedJobService schedJobService;
    private final AuthorizeGroupService authorizeGroupService;

    public DisjobJobController(SchedJobService schedJobService,
                               AuthorizeGroupService authorizeGroupService) {
        this.schedJobService = schedJobService;
        this.authorizeGroupService = authorizeGroupService;
    }

    // -------------------------------------------------------查询

    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping
    public String job(ModelMap mmap) {
        mmap.put("groups", SchedGroupService.myGroups(getLoginName()));
        return PREFIX + "/job";
    }

    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping("/dag")
    public void dag(@RequestParam("expr") String expr, @RequestParam("thumb") boolean thumb, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        try {
            DAGUtils.drawImage(expr, thumb, 2000, response.getOutputStream());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = "[Error]: " + Throwables.getRootCauseMessage(e);
            if (errorMessage.length() > MAX_TEXT_LENGTH) {
                errorMessage = errorMessage.substring(0, MAX_TEXT_LENGTH - 3) + "...";
            }
            ImageUtils.createImage(errorMessage, response.getOutputStream(), 1000, 580);
        }
    }

    /**
     * 查询作业配置列表
     */
    @RequiresPermissions(PERMISSION_CODE)
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SchedJobPageRequest request) {
        request.authorizeAndTruncateGroup(getLoginName());

        if (CollectionUtils.isEmpty(request.getGroups())) {
            return TableDataInfo.empty();
        }
        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        PageResponse<SchedJobResponse> response = schedJobService.queryJobForPage(request);
        return Pagination.toTableDataInfo(response);
    }

    /**
     * 查看作业配置详情
     */
    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping("/detail/{jobId}")
    public String detail(@PathVariable("jobId") long jobId, ModelMap mmap) {
        SchedJobResponse job = schedJobService.getJob(jobId);
        AuthorizeGroupService.authorizeGroup(getLoginName(), job.getGroup());

        mmap.put("job", job);
        return PREFIX + "/detail";
    }

    /**
     * 导出作业配置列表
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "作业配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(SchedJobPageRequest request) {
        request.authorizeAndTruncateGroup(getLoginName());

        List<SchedJobExport> list = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(request.getGroups())) {
            // 不分页查询，导出全部数据
            request.setPaged(false);
            List<SchedJobResponse> rows = schedJobService.queryJobForPage(request).getRows();
            list = Collects.convert(rows, SchedJobExport::of);
        }
        ExcelUtil<SchedJobExport> excel = new ExcelUtil<>(SchedJobExport.class);
        return excel.exportExcel(list, "作业配置数据");
    }

    // -------------------------------------------------------操作

    /**
     * 新增作业配置
     */
    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        return toAdd(new SchedJobResponse(), mmap);
    }

    /**
     * 复制作业配置
     */
    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping("/copy/{jobId}")
    public String copy(@PathVariable("jobId") long jobId, ModelMap mmap) {
        SchedJobResponse job = schedJobService.getJob(jobId);
        AuthorizeGroupService.authorizeGroup(getLoginName(), job.getGroup());

        return toAdd(job, mmap);
    }

    /**
     * 新增作业配置
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "作业配置", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult doAdd(SchedJobAddRequest req) throws JobException {
        String user = getLoginName();
        AuthorizeGroupService.authorizeGroup(user, req.getGroup());

        return success(schedJobService.addJob(user, req));
    }

    /**
     * 修改作业配置
     */
    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping("/edit/{jobId}")
    public String edit(@PathVariable("jobId") long jobId, ModelMap mmap) {
        SchedJobResponse job = schedJobService.getJob(jobId);
        AuthorizeGroupService.authorizeGroup(getLoginName(), job.getGroup());

        mmap.put("job", job);
        return PREFIX + "/edit";
    }

    /**
     * 修改作业配置
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "作业配置", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult doEdit(SchedJobUpdateRequest req) throws JobException {
        String user = getLoginName();
        AuthorizeGroupService.authorizeGroup(user, req.getGroup());

        schedJobService.updateJob(user, req);
        return success();
    }

    /**
     * 删除作业配置
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "作业配置", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam("ids") String ids) {
        List<Long> jobIds = Collects.split(ids, Str.COMMA, Long::parseLong);
        if (jobIds.isEmpty()) {
            return error("Job id不能为空");
        }
        final String user = getLoginName();
        MultithreadExecutors.run(jobIds, jobId -> doDeleteJob(user, jobId), ThreadPoolExecutors.commonThreadPool());

        return success();
    }

    /**
     * 修改作业配置状态
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "修改作业配置状态", businessType = BusinessType.UPDATE)
    @PostMapping("/state/change")
    @ResponseBody
    public AjaxResult changeState(@RequestParam("jobId") long jobId,
                                  @RequestParam("toState") Integer toState) {
        String user = getLoginName();
        authorizeGroupService.authorizeJob(user, jobId);

        schedJobService.changeJobState(user, jobId, toState);
        return success();
    }

    /**
     * 触发执行
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "触发执行", businessType = BusinessType.OTHER)
    @PostMapping("/trigger")
    @ResponseBody
    public AjaxResult manualTriggerJob(@RequestParam("jobId") long jobId) throws JobException {
        String user = getLoginName();
        authorizeGroupService.authorizeJob(user, jobId);

        schedJobService.manualTriggerJob(user, jobId);
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
        schedJobService.deleteJob(user, jobId);
    }

}
