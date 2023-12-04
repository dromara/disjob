/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.admin.controller;

import cn.ponfee.disjob.admin.util.PageUtils;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.application.request.AddSchedGroupRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import cn.ponfee.disjob.supervisor.application.request.UpdateSchedGroupRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedGroupResponse;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

/**
 * 分组配置
 *
 * @author Ponfee
 */
@Controller
@RequestMapping("/" + DisjobGroupController.PREFIX)
public class DisjobGroupController extends BaseController {
    static final String PREFIX = "disjob/group";

    private static final String PERMISSION_VIEW = "disjob:group:view";
    private static final String PERMISSION_QUERY = "disjob:group:query";
    private static final String PERMISSION_OPERATE = "disjob:group:operate";

    private final SchedGroupService schedGroupService;

    public DisjobGroupController(SchedGroupService schedGroupService) {
        this.schedGroupService = schedGroupService;
    }

    // -------------------------------------------------------查询

    @RequiresPermissions(PERMISSION_VIEW)
    @GetMapping()
    public String group() {
        return PREFIX + "/group";
    }

    /**
     * 查询调度配置列表
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SchedGroupPageRequest request) {
        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        return PageUtils.toTableDataInfo(schedGroupService.queryForPage(request));
    }

    // -------------------------------------------------------操作

    /**
     * 新增调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        return toAdd(new SchedGroupResponse(), mmap);
    }

    /**
     * 复制调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/copy/{group}")
    public String copy(@PathVariable("group") String group, ModelMap mmap) {
        return toAdd(schedGroupService.getGroup(group), mmap);
    }

    private String toAdd(SchedGroupResponse group, ModelMap mmap) {
        mmap.put("group", group);
        return PREFIX + "/add";
    }

    /**
     * 新增调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "调度配置", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult doAdd(AddSchedGroupRequest req) throws JobException {
        req.setCreatedBy(getLoginName());
        schedGroupService.add(req);
        return success();
    }

    /**
     * 修改调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/edit/{group}")
    public String edit(@PathVariable("group") String group, ModelMap mmap) {
        SchedGroupResponse entity = schedGroupService.getGroup(group);
        Assert.notNull(entity, () -> "Group not found: " + group);
        mmap.put("group", entity);
        return PREFIX + "/edit";
    }

    /**
     * 修改调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "调度配置", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult doEdit(UpdateSchedGroupRequest request) {
        request.setUpdatedBy(getLoginName());
        schedGroupService.update(request);
        return success();
    }

    /**
     * 删除调度配置
     */
    @RequiresPermissions(PERMISSION_OPERATE)
    @Log(title = "调度配置", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam("group") String group) {
        if (schedGroupService.delete(group)) {
            return success();
        } else {
            return AjaxResult.error("删除失败");
        }
    }

}
