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

import cn.ponfee.disjob.supervisor.application.ServerInvokeService;
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
    private static final String PERMISSION_CODE = "disjob:supervisor:operate";

    private final ServerInvokeService serverInvokeService;

    public SupervisorInfoController(ServerInvokeService serverInvokeService) {
        this.serverInvokeService = serverInvokeService;
    }

    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping
    public String supervisor(ModelMap mmap) {
        mmap.put("list", serverInvokeService.supervisors());
        return PREFIX + "/supervisor";
    }

}
