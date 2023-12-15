/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.admin.controller;

import cn.ponfee.disjob.supervisor.application.ServerMetricsService;
import com.ruoyi.common.core.controller.BaseController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Supervisor info controller
 *
 * @author Ponfee
 */
@Controller
@RequestMapping("/" + SupervisorInfoController.PREFIX)
public class SupervisorInfoController extends BaseController {

    static final String PREFIX = "disjob/supervisor";
    private static final String PERMISSION_SUPERVISOR = "disjob:supervisor:operate";

    private final ServerMetricsService serverMetricsService;

    public SupervisorInfoController(ServerMetricsService serverMetricsService) {
        this.serverMetricsService = serverMetricsService;
    }

    @RequiresPermissions(PERMISSION_SUPERVISOR)
    @GetMapping
    public String supervisor(ModelMap mmap) throws Exception {
        mmap.put("list", serverMetricsService.supervisors());
        return PREFIX + "/supervisor";
    }

}
