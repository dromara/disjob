package com.ruoyi.web.controller.tool;

import com.ruoyi.common.core.controller.BaseController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tool controller
 *
 * @author ruoyi
 */
@Controller
public class ToolController extends BaseController {

    private static final String PREFIX = "tool";

    @RequiresPermissions("tool:build:view")
    @GetMapping(PREFIX + "/build")
    public String build() {
        return PREFIX + "/build";
    }

    @RequiresPermissions("tool:apidocs:view")
    @GetMapping("apidocs")
    public String apidocs() {
        return redirect("/apidocs.html");
    }

}
