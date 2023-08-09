/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.admin.util;

import cn.ponfee.disjob.common.model.PageResponse;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.utils.ServletUtils;

import java.util.List;

/**
 * Disjob Utils
 *
 * @author Ponfee
 */
public class DisjobUtils {

    /**
     * 转为table结构
     *
     * @param page the page query result
     * @return TableDataInfo
     * @see com.ruoyi.common.core.controller.BaseController#getDataTable(List)
     */
    public static TableDataInfo toTableDataInfo(PageResponse<?> page) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(0);
        rspData.setRows(page.getRows());
        rspData.setTotal(page.getTotal());
        return rspData;
    }

    public static int getPageNumberParameter() {
        return Convert.toInt(ServletUtils.getParameter(TableSupport.PAGE_NUM), 1);
    }

    public static int getPageSizeParameter() {
        return Convert.toInt(ServletUtils.getParameter(TableSupport.PAGE_SIZE), 20);
    }
}
