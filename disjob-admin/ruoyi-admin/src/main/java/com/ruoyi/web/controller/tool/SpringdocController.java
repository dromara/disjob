package com.ruoyi.web.controller.tool;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.ruoyi.common.core.controller.BaseController;

/**
 * Springdoc接口
 * 
 * @author ruoyi
 */
@Controller
public class SpringdocController extends BaseController
{
    @RequiresPermissions("tool:apidocs:view")
    @GetMapping("/tool/apidocs")
    public String index()
    {
        return redirect("/apidocs.html");
    }
}
