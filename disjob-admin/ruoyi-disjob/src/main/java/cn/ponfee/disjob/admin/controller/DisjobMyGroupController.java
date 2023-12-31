/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.admin.controller;

import cn.ponfee.disjob.admin.util.PageUtils;
import cn.ponfee.disjob.supervisor.application.AuthorizeGroupService;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.application.ServerMetricsService;
import cn.ponfee.disjob.supervisor.application.request.ModifyMaximumPoolSizeRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import cn.ponfee.disjob.supervisor.application.request.UpdateSchedGroupRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedGroupResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.service.ISysUserService;
import org.apache.commons.collections4.CollectionUtils;
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
    private static final String PERMISSION_OPERATE = "disjob:mygroup:operate";

    private final SchedGroupService schedGroupService;
    private final ServerMetricsService serverMetricsService;
    private final ISysUserService sysUserService;

    public DisjobMyGroupController(SchedGroupService schedGroupService,
                                   ServerMetricsService serverMetricsService,
                                   ISysUserService sysUserService) {
        this.schedGroupService = schedGroupService;
        this.serverMetricsService = serverMetricsService;
        this.sysUserService = sysUserService;
    }

    // -------------------------------------------------------查询

    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping
    public String mygroup(ModelMap mmap) {
        mmap.put("groups", SchedGroupService.myGroups(getLoginName()));
        return PREFIX + "/mygroup";
    }

    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/search_user")
    @ResponseBody
    public List<String> searchUser(@RequestParam(value = "term") String term) {
        return sysUserService.searchUser(term);
    }

    /**
     * 查询分组列表
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SchedGroupPageRequest request) {
        request.authorizeAndTruncateGroup(getLoginName());
        if (CollectionUtils.isEmpty(request.getGroups())) {
            return PageUtils.empty();
        }

        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        return PageUtils.toTableDataInfo(schedGroupService.queryForPage(request));
    }

    /**
     * 修改分组
     */
    @RequiresPermissions(PERMISSION_OPERATE)
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
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "修改分组", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult doEdit(UpdateSchedGroupRequest req) {
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

    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/worker")
    public String worker(@RequestParam("group") String group, ModelMap mmap) {
        AuthorizeGroupService.authorizeGroup(getLoginName(), group);

        mmap.put("group", group);
        mmap.put("list", serverMetricsService.workers(group));
        return PREFIX + "/worker";
    }

    /**
     * 更新Worker线程池最大线程数
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "更新Worker线程池最大线程数", businessType = BusinessType.UPDATE)
    @PostMapping("/modify_maximum_pool_size")
    @ResponseBody
    public AjaxResult modifyMaximumPoolSize(ModifyMaximumPoolSizeRequest request) {
        AuthorizeGroupService.authorizeGroup(getLoginName(), request.getGroup());

        serverMetricsService.modifyWorkerMaximumPoolSize(request);
        return AjaxResult.success("更新成功");
    }

}
