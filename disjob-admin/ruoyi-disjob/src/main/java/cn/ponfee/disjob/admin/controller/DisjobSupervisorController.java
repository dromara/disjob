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
import cn.ponfee.disjob.supervisor.base.OperationEventType;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * Disjob supervisor controller
 *
 * @author Ponfee
 */
@Controller
@RequestMapping("/" + DisjobSupervisorController.PREFIX)
public class DisjobSupervisorController extends BaseController {

    static final String PREFIX = "disjob/supervisor";
    private static final String PERMISSION_CODE = "disjob:supervisor:operate";

    private final ServerInvokeService serverInvokeService;

    public DisjobSupervisorController(ServerInvokeService serverInvokeService) {
        this.serverInvokeService = serverInvokeService;
    }

    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping
    public String supervisor(ModelMap mmap) {
        mmap.put("list", serverInvokeService.supervisors());
        return PREFIX + "/supervisor";
    }

    /**
     * 发布操作事件
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "发布操作事件", businessType = BusinessType.UPDATE)
    @PostMapping("/publish_operation_event")
    @ResponseBody
    public AjaxResult publishOperationEvent(@RequestParam("eventType") OperationEventType eventType) {
        serverInvokeService.publishOperationEvent(eventType, null, true);
        return AjaxResult.success("发布成功");
    }

}
