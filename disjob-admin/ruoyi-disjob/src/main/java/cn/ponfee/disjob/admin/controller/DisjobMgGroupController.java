/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.admin.controller;

import cn.ponfee.disjob.admin.util.PageUtils;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.application.ServerMetricsService;
import cn.ponfee.disjob.supervisor.application.request.AddSchedGroupRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import cn.ponfee.disjob.supervisor.application.value.TokenName;
import com.google.common.collect.ImmutableMap;
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
import java.util.stream.Collectors;

/**
 * Manage group controller
 *
 * @author Ponfee
 */
@Controller
@RequestMapping("/" + DisjobMgGroupController.PREFIX)
public class DisjobMgGroupController extends BaseController {

    static final String PREFIX = "disjob/mggroup";
    private static final String PERMISSION_OPERATE = "disjob:mggroup:operate";

    private final SchedGroupService schedGroupService;
    private final ServerMetricsService serverMetricsService;

    public DisjobMgGroupController(SchedGroupService schedGroupService,
                                   ServerMetricsService serverMetricsService) {
        this.schedGroupService = schedGroupService;
        this.serverMetricsService = serverMetricsService;
    }

    // -------------------------------------------------------查询

    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/match_group")
    @ResponseBody
    public AjaxResult matchGroup(@RequestParam(value = "term", required = false) String term) {
        List<ImmutableMap<String, String>> result = schedGroupService.matchGroup(term)
            .stream()
            .map(e -> ImmutableMap.of("id", e, "text", e))
            .collect(Collectors.toList());

        return AjaxResult.success(result);
    }

    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping
    public String mggroup() {
        return PREFIX + "/mggroup";
    }

    /**
     * 查询分组列表
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SchedGroupPageRequest request) {
        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        request.truncateUserGroup();
        return PageUtils.toTableDataInfo(schedGroupService.queryForPage(request));
    }

    // -------------------------------------------------------操作

    /**
     * 新增分组
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/add")
    public String add() {
        return PREFIX + "/add";
    }

    /**
     * 新增分组
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "新增分组", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(AddSchedGroupRequest req) {
        req.setCreatedBy(getLoginName());
        schedGroupService.add(req);
        return success();
    }

    /**
     * 删除分组
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "删除分组", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam("group") String group) {
        if (schedGroupService.delete(group, getLoginName())) {
            return success();
        } else {
            return AjaxResult.error("删除失败");
        }
    }

    /**
     * 令牌管理
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/token")
    public String token(@RequestParam("group") String group, ModelMap mmap) {
        mmap.put("data", schedGroupService.get(group));
        return PREFIX + "/token";
    }

    /**
     * 令牌管理
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "令牌管理", businessType = BusinessType.UPDATE)
    @PostMapping("/token")
    @ResponseBody
    public AjaxResult token(@RequestParam("group") String group,
                            @RequestParam("name") TokenName name,
                            @RequestParam("operation") TokenOperation operation,
                            @RequestParam("currentValue") String currentValue) {
        String newToken = TokenOperation.clear == operation ? "" : UuidUtils.uuid32();
        String oldToken = TokenOperation.set == operation ? "" : currentValue;
        if (schedGroupService.updateToken(group, name, newToken, getLoginName(), oldToken)) {
            return AjaxResult.success("操作成功", newToken);
        } else {
            return AjaxResult.error("操作失败");
        }
    }

    /**
     * 更新own_user
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "更新Own User", businessType = BusinessType.UPDATE)
    @PostMapping("/update_own_user")
    @ResponseBody
    public AjaxResult updateOwnUser(@RequestParam("group") String group,
                                    @RequestParam("ownUser") String ownUser) {
        if (schedGroupService.updateOwnUser(group, ownUser, getLoginName())) {
            return AjaxResult.success("更新成功", ownUser);
        } else {
            return AjaxResult.error("更新失败");
        }
    }

    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/worker")
    public String worker(@RequestParam("group") String group, ModelMap mmap) {
        mmap.put("list", serverMetricsService.workers(group));
        return PREFIX + "/worker";
    }

    private enum TokenOperation {
        /**
         * Set token if current token is empty value
         */
        set,
        /**
         * Change token
         */
        change,
        /**
         * Clear token value to empty
         */
        clear
    }

}
