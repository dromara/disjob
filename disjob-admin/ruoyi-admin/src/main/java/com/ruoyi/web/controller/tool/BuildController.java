package com.ruoyi.web.controller.tool;

import com.ruoyi.common.core.controller.BaseController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * build 表单构建
 *
 * @author ruoyi
 */
@Controller
public class BuildController extends BaseController
{
    private final String prefix = "tool/build";

    @RequiresPermissions("tool:build:view")
    @GetMapping("/tool/build")
    public String build()
    {
        return prefix + "/build";
    }
}
