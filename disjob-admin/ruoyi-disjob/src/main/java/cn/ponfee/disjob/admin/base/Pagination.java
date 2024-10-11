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

package cn.ponfee.disjob.admin.base;

import cn.ponfee.disjob.common.model.PageResponse;
import com.ruoyi.common.core.page.TableDataInfo;

import java.util.List;

/**
 * Page utilities
 *
 * @author Ponfee
 */
public class Pagination {

    /**
     * 转为table结构
     *
     * @param page the page query result
     * @return TableDataInfo
     * @see com.ruoyi.common.core.controller.BaseController#getDataTable(List)
     */
    public static TableDataInfo toTableDataInfo(PageResponse<?> page) {
        TableDataInfo table = new TableDataInfo();
        table.setCode(0);
        table.setRows(page.getRows());
        table.setTotal(page.getTotal());
        return table;
    }

}
