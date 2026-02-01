/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.supervisor;

import cn.ponfee.disjob.common.base.TablePrinter;
import cn.ponfee.disjob.common.concurrent.ShutdownHookManager;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.tree.PlainNode;
import cn.ponfee.disjob.common.tree.TreeNode;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collect spring boot test container info
 *
 * @author Ponfee
 */
class SpringBootTestCollector {

    private static final Map<ApplicationContext, Set<Object>> CONTAINER_BEAN_MAP = new ConcurrentHashMap<>();

    static {
        ShutdownHookManager.addShutdownHook(Integer.MAX_VALUE, SpringBootTestCollector::print);
    }

    static void collect(ApplicationContext applicationContext, Object bean) {
        CONTAINER_BEAN_MAP.computeIfAbsent(applicationContext, k -> ConcurrentHashMap.newKeySet()).add(bean);
    }

    // ------------------------------------------------------------------------private methods

    private static void print() throws IOException {
        List<TreeNode<Integer, String>> nodes = new ArrayList<>();
        CONTAINER_BEAN_MAP.forEach((ctx, beans) -> {
            Integer ctxId = System.identityHashCode(ctx);
            String ctxStr = "[" + Dates.DATETIME_MILLI_FORMAT.format(ctx.getStartupDate()) + "] " + ctx.getDisplayName();
            if (ctx.getParent() != null) {
                ctxStr += " --> " + ctx.getParent().getDisplayName();
            }
            nodes.add(new TreeNode<>(ctxId, null, ctxStr));
            beans.forEach(e -> nodes.add(new TreeNode<>(System.identityHashCode(e), ctxId, ObjectUtils.identityToString(e))));
        });

        StringBuilder output = new StringBuilder(4096);
        TreeNode<Integer, String> root = new TreeNode<>(null, null, "Spring boot test containers");
        root.mount(nodes);
        root.print(output, PlainNode::getAttach);
        List<String> lines = Arrays.asList(output.toString().split("\n"));

        System.out.println("\n\n" + TablePrinter.HALF.print(null, lines) + "\n\n");
    }

}
