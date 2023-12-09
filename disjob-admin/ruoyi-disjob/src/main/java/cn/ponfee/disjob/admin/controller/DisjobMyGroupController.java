/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.admin.controller;

import cn.ponfee.disjob.admin.util.PageUtils;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.application.ServerMetricsService;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

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

    public DisjobMyGroupController(SchedGroupService schedGroupService,
                                   ServerMetricsService serverMetricsService) {
        this.schedGroupService = schedGroupService;
        this.serverMetricsService = serverMetricsService;
    }

    // -------------------------------------------------------查询

    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping
    public String mygroup() {
        return PREFIX + "/mygroup";
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
        return PageUtils.toTableDataInfo(schedGroupService.queryForPage(request));
    }

    @RequiresPermissions(PERMISSION_OPERATE)
    @GetMapping("/worker")
    public String worker(@RequestParam("group") String group, ModelMap mmap) throws Exception {
        mmap.put("list", serverMetricsService.workers(group));
        return PREFIX + "/worker";
    }

}
