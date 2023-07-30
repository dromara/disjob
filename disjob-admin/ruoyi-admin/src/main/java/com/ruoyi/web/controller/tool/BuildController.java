package com.ruoyi.web.controller.tool;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.ruoyi.common.core.controller.BaseController;

/**
 * build 表单构建
 * 
 * @author ruoyi
 */
@Controller
public class BuildController extends BaseController
{
    private String prefix = "tool/build";

    @RequiresPermissions("tool:build:view")
    @GetMapping("/tool/build")
    public String build()
    {
        return prefix + "/build";
    }
}
