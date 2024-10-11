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
import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.supervisor.application.AuthorizeGroupService;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.application.ServerInvokeService;
import cn.ponfee.disjob.supervisor.application.request.ConfigureAllWorkerRequest;
import cn.ponfee.disjob.supervisor.application.request.ConfigureOneWorkerRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupUpdateRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedGroupResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.service.ISysUserService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * My group controller
 *
 * @author Ponfee
 */
@Controller
@RequestMapping("/" + DisjobMyGroupController.PREFIX)
public class DisjobMyGroupController extends BaseController {

    static final String PREFIX = "disjob/mygroup";
    private static final String PERMISSION_CODE = "disjob:mygroup:operate";

    private final SchedGroupService schedGroupService;
    private final ServerInvokeService serverInvokeService;
    private final ISysUserService sysUserService;

    public DisjobMyGroupController(SchedGroupService schedGroupService,
                                   ServerInvokeService serverInvokeService,
                                   ISysUserService sysUserService) {
        this.schedGroupService = schedGroupService;
        this.serverInvokeService = serverInvokeService;
        this.sysUserService = sysUserService;
    }

    // -------------------------------------------------------查询

    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping
    public String mygroup(ModelMap mmap) {
        mmap.put("groups", SchedGroupService.myGroups(getLoginName()));
        return PREFIX + "/mygroup";
    }

    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping("/search_user")
    @ResponseBody
    public List<String> searchUser(@RequestParam(value = "term") String term) {
        return sysUserService.searchUser(term);
    }

    /**
     * 查询分组列表
     */
    @RequiresPermissions(PERMISSION_CODE)
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SchedGroupPageRequest request) {
        request.authorizeAndTruncateGroup(getLoginName());
        if (CollectionUtils.isEmpty(request.getGroups())) {
            return TableDataInfo.empty();
        }

        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        return Pagination.toTableDataInfo(schedGroupService.queryForPage(request));
    }

    /**
     * 修改分组
     */
    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping("/edit/{group}")
    public String edit(@PathVariable("group") String group, ModelMap mmap) {
        String user = getLoginName();
        AuthorizeGroupService.authorizeGroup(user, group);

        SchedGroupResponse data = schedGroupService.get(group);
        Assert.notNull(data, () -> "Group not found: " + group);
        mmap.put("data", data);
        mmap.put("isOwnUser", user.equals(data.getOwnUser()));
        return PREFIX + "/edit";
    }

    /**
     * 修改分组
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "修改分组", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult doEdit(SchedGroupUpdateRequest req) {
        String user = getLoginName();
        AuthorizeGroupService.authorizeGroup(user, req.getGroup());

        SchedGroupResponse data = schedGroupService.get(req.getGroup());
        Assert.isTrue(req.getVersion() == data.getVersion(), "Edit data conflicted.");
        if (!user.equals(data.getOwnUser())) {
            // 非Own User不可更换own_user数据(即只有Own User本人才能更换该group的own_user为其它人)
            Assert.isTrue(req.getOwnUser().equals(data.getOwnUser()), "Cannot modify own user.");
        }
        req.setUpdatedBy(user);
        boolean result = schedGroupService.edit(req);
        return result ? success() : error("修改冲突，请刷新页面");
    }

    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping("/worker")
    public String worker(@RequestParam("group") String group,
                         @RequestParam(value = "worker", required = false) String worker,
                         ModelMap mmap) {
        AuthorizeGroupService.authorizeGroup(getLoginName(), group);

        if (StringUtils.isBlank(worker) || !worker.contains(Str.COLON)) {
            worker = null;
        }

        mmap.put("group", group);
        mmap.put("worker", worker);
        mmap.put("workers", serverInvokeService.workers(group, worker));
        return PREFIX + "/worker";
    }

    /**
     * 修改指定Worker的参数配置
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "修改指定Worker的参数配置", businessType = BusinessType.UPDATE)
    @PostMapping("/configure_one_worker")
    @ResponseBody
    public AjaxResult configureOneWorker(ConfigureOneWorkerRequest request) {
        AuthorizeGroupService.authorizeGroup(getLoginName(), request.getGroup());

        serverInvokeService.configureOneWorker(request);
        return AjaxResult.success("修改成功");
    }

    /**
     * 修改该分组下的全部Worker的参数配置
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "修改该分组下的全部Worker的参数配置", businessType = BusinessType.UPDATE)
    @PostMapping("/configure_all_worker")
    @ResponseBody
    public AjaxResult configureAllWorker(ConfigureAllWorkerRequest request) {
        AuthorizeGroupService.authorizeGroup(getLoginName(), request.getGroup());

        serverInvokeService.configureAllWorker(request);
        return AjaxResult.success("修改成功");
    }

}
