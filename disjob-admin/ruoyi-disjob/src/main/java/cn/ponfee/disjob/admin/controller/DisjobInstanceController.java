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

import cn.ponfee.disjob.admin.base.Pagination;
import cn.ponfee.disjob.common.base.TextTokenizer;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.Numbers;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.supervisor.application.AuthorizeGroupService;
import cn.ponfee.disjob.supervisor.application.OpenapiService;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.application.request.SchedInstancePageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobSearchRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedTaskResponse;
import cn.ponfee.disjob.supervisor.component.JobQuerier;
import cn.ponfee.disjob.supervisor.exception.KeyNotExistsException;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 任务实例Controller
 *
 * @author Ponfee
 */
@Controller
@RequestMapping("/" + DisjobInstanceController.PREFIX)
public class DisjobInstanceController extends BaseController {

    static final String PREFIX = "disjob/instance";
    private static final String PERMISSION_CODE = "disjob:instance:operate";

    private static final int WAIT_SLEEP_ROUND = 20;
    private static final long[] WAIT_SLEEP_MILLIS = {2500, 1000};
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[1-9]\\d*$");

    private final OpenapiService openapiService;
    private final JobQuerier jobQuerier;
    private final AuthorizeGroupService authorizeGroupService;

    public DisjobInstanceController(OpenapiService openapiService,
                                    JobQuerier jobQuerier,
                                    AuthorizeGroupService authorizeGroupService) {
        this.openapiService = openapiService;
        this.jobQuerier = jobQuerier;
        this.authorizeGroupService = authorizeGroupService;
    }

    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping("/search_job")
    @ResponseBody
    public AjaxResult searchJob(@RequestParam(value = "term") String term) {
        SchedJobSearchRequest req = parseTerm(term);
        return AjaxResult.success(req == null ? Collections.emptyList() : jobQuerier.searchJob(req));
    }

    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping
    public String instance() {
        return PREFIX + "/instance";
    }

    /**
     * 查询任务实例列表-tree
     */
    @RequiresPermissions(PERMISSION_CODE)
    @PostMapping("/tree")
    @ResponseBody
    public Object tree(SchedInstancePageRequest request,
                       @RequestParam(value = "resetSearch", required = false) String resetSearch) {
        return queryForPage(request, true, resetSearch);
    }

    /**
     * 查询任务实例列表-flat
     */
    @RequiresPermissions(PERMISSION_CODE)
    @PostMapping("/flat")
    @ResponseBody
    public Object flat(SchedInstancePageRequest request,
                       @RequestParam(value = "resetSearch", required = false) String resetSearch) {
        return queryForPage(request, false, resetSearch);
    }

    @RequiresPermissions(PERMISSION_CODE)
    @PostMapping("/children")
    @ResponseBody
    public List<SchedInstanceResponse> children(@RequestParam("pnstanceId") Long pnstanceId) {
        authorizeGroupService.authorizeInstance(getLoginName(), pnstanceId);

        return openapiService.listInstanceChildren(pnstanceId);
    }

    /**
     * Date等类型序列化会使用toString():
     * <pre>{@code
     *  mmap.put("tasks", tasks);
     *  data: [[${tasks}]]
     * }</pre>
     *
     * <p>
     * 使用Json方式序列化:
     * <pre>{@code
     *  mmap.put("tasks", Jsons.toJson(tasks));
     *  data: [(${tasks})]
     * }</pre>
     *
     * @param instanceId the instance id
     * @param mmap       the mmap
     * @return html page path
     */
    @RequiresPermissions(PERMISSION_CODE)
    @GetMapping("/tasks/{instanceId}")
    public String tasks(@PathVariable("instanceId") Long instanceId, ModelMap mmap) {
        authorizeGroupService.authorizeInstance(getLoginName(), instanceId);

        List<SchedTaskResponse> tasks = openapiService.getInstanceTasks(instanceId);
        mmap.put("tasks", Jsons.toJson(tasks));
        return PREFIX + "/tasks";
    }

    // -----------------------------------------------------------操作

    /**
     * 删除任务实例
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "删除任务实例", businessType = BusinessType.DELETE)
    @PostMapping("/remove/{instanceId}")
    @ResponseBody
    public AjaxResult remove(@PathVariable("instanceId") Long instanceId) {
        authorizeGroupService.authorizeInstance(getLoginName(), instanceId);

        openapiService.deleteInstance(instanceId);
        return success();
    }

    /**
     * 暂停任务实例
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "暂停任务实例", businessType = BusinessType.UPDATE)
    @PostMapping("/pause/{instanceId}")
    @ResponseBody
    public AjaxResult pause(@PathVariable("instanceId") Long instanceId) {
        authorizeGroupService.authorizeInstance(getLoginName(), instanceId);

        openapiService.pauseInstance(instanceId);
        Threads.waitUntil(WAIT_SLEEP_ROUND, WAIT_SLEEP_MILLIS, () -> {
            SchedInstanceResponse instance = openapiService.getInstance(instanceId, false);
            return !RunState.of(instance.getRunState()).isPausable();
        });
        return success();
    }

    /**
     * 恢复任务实例
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "恢复任务实例", businessType = BusinessType.UPDATE)
    @PostMapping("/resume/{instanceId}")
    @ResponseBody
    public AjaxResult resume(@PathVariable("instanceId") Long instanceId) {
        authorizeGroupService.authorizeInstance(getLoginName(), instanceId);

        openapiService.resumeInstance(instanceId);
        Threads.waitUntil(WAIT_SLEEP_ROUND, new long[]{500, 200}, () -> {
            SchedInstanceResponse instance = openapiService.getInstance(instanceId, false);
            return !RunState.PAUSED.equalsValue(instance.getRunState());
        });
        return success();
    }

    /**
     * 取消任务实例
     */
    @RequiresPermissions(PERMISSION_CODE)
    @Log(title = "取消任务实例", businessType = BusinessType.UPDATE)
    @PostMapping("/cancel/{instanceId}")
    @ResponseBody
    public AjaxResult cancel(@PathVariable("instanceId") Long instanceId) {
        authorizeGroupService.authorizeInstance(getLoginName(), instanceId);

        openapiService.cancelInstance(instanceId);
        Threads.waitUntil(WAIT_SLEEP_ROUND, WAIT_SLEEP_MILLIS, () -> {
            SchedInstanceResponse instance = openapiService.getInstance(instanceId, false);
            return RunState.of(instance.getRunState()).isTerminal();
        });
        return success();
    }

    // ------------------------------------------------------------private methods

    private SchedJobSearchRequest parseTerm(String term) {
        if (StringUtils.isBlank(term)) {
            return null;
        }

        SchedJobSearchRequest request = new SchedJobSearchRequest();
        Set<String> groups = null;
        String user = getLoginName();
        if ("*".equals(term)) {
            request.setGroups(AuthorizeGroupService.truncateGroup(SchedGroupService.myGroups(user)));
            return request;
        }

        TextTokenizer tokenizer = new TextTokenizer(term, ": ");
        if (tokenizer.hasNext()) {
            String str = tokenizer.next().trim();
            if (SchedGroupService.myGroups(user).contains(str)) {
                groups = Collections.singleton(str);
                term = tokenizer.tail();
            }
        }
        if (groups == null) {
            groups = AuthorizeGroupService.truncateGroup(SchedGroupService.myGroups(user));
        }

        term = term.trim();
        request.setGroups(groups);
        request.setJobName(Strings.concatSqlLike(term));
        if (NUMBER_PATTERN.matcher(term).matches()) {
            request.setJobId(Numbers.toWrapLong(term));
        }

        return request;
    }

    private Object queryForPage(SchedInstancePageRequest request, boolean root, String resetSearch) {
        if (StringUtils.isBlank(resetSearch)) {
            return TableDataInfo.empty();
        }
        try {
            request.authorize(getLoginName(), authorizeGroupService);
        } catch (KeyNotExistsException e) {
            return TableDataInfo.empty();
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }

        request.setRoot(root);
        request.setPageNumber(super.getPageNumber());
        request.setPageSize(super.getPageSize());
        return Pagination.toTableDataInfo(openapiService.queryInstanceForPage(request));
    }

}
