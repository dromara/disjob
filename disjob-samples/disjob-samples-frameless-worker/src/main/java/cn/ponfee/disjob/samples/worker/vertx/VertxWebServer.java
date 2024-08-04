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

package cn.ponfee.disjob.samples.worker.vertx;

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.spring.RpcControllerUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.dto.worker.*;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.samples.worker.util.JobExecutorParser;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * Web server based vertx-web.
 *
 * @author Ponfee
 */
public class VertxWebServer extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(VertxWebServer.class);

    private final int port;
    private final String prefixPath;
    private final TaskReceiver taskReceiver;
    private final WorkerRpcService workerRpcService;

    public VertxWebServer(int port, String contextPath, TaskReceiver taskReceiver, WorkerRpcService workerRpcService) {
        this.port = port;
        this.prefixPath = Strings.concatPath(Strings.trimPath(contextPath), WorkerRpcService.PREFIX_PATH);
        this.taskReceiver = taskReceiver;
        this.workerRpcService = workerRpcService;
    }

    public final void deploy() {
        VertxOptions options = new VertxOptions();
        Vertx vertx0 = Vertx.vertx(options);

        // here super.vertx is null

        vertx0.deployVerticle(this);
        if (super.vertx != vertx0) {
            LOG.warn("Not a same vertx: super.vertx({}) != vertx0({})", super.vertx, vertx0);
        }
    }

    public final void close() {
        if (super.vertx == null) {
            LOG.error("Cannot close un-deployed vertx.");
            return;
        }

        // HttpServer.close()
        CountDownLatch countDownLatch = new CountDownLatch(1);
        super.vertx.close(ctx -> {
            ctx.result();
            countDownLatch.countDown();
        });
        try {
            boolean res = countDownLatch.await(60, TimeUnit.SECONDS);
            LOG.info("Close vertx success {}.", res);
        } catch (InterruptedException e) {
            LOG.error("Close vertx interrupted.", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void start() {
        Router router = Router.router(super.vertx);
        router.route().handler(BodyHandler.create());

        //String[] args = ctx.body().asPojo(String[].class);
        router.post(prefixPath + "/job/verify").handler(ctx -> handle(() -> {
            VerifyJobParam param = parseBodyArg(ctx, VerifyJobParam.class);
            workerRpcService.verify(param);
        }, ctx, BAD_REQUEST));

        router.post(prefixPath + "/job/split").handler(ctx -> handle(() -> {
            SplitJobParam param = parseBodyArg(ctx, SplitJobParam.class);
            JobExecutorParser.parse(param, "jobExecutor");
            return workerRpcService.split(param);
        }, ctx, INTERNAL_SERVER_ERROR));

        router.get(prefixPath + "/task/exists").handler(ctx -> handle(() -> {
            ExistsTaskParam param = parseParamArg(ctx, ExistsTaskParam.class);
            return workerRpcService.existsTask(param);
        }, ctx, INTERNAL_SERVER_ERROR));

        router.get(prefixPath + "/metrics").handler(ctx -> handle(() -> {
            GetMetricsParam param = parseParamArg(ctx, GetMetricsParam.class);
            return workerRpcService.metrics(param);
        }, ctx, INTERNAL_SERVER_ERROR));

        router.post(prefixPath + "/worker/configure").handler(ctx -> handle(() -> {
            ConfigureWorkerParam param = parseBodyArg(ctx, ConfigureWorkerParam.class);
            workerRpcService.configureWorker(param);
        }, ctx, INTERNAL_SERVER_ERROR));

        if (taskReceiver != null) {
            router.post(prefixPath + "/task/receive").handler(ctx -> handle(() -> {
                ExecuteTaskParam param = parseBodyArg(ctx, ExecuteTaskParam.class);
                JobExecutorParser.parse(param, "jobExecutor");
                return taskReceiver.receive(param);
            }, ctx, INTERNAL_SERVER_ERROR));
        }

        HttpServerOptions options = new HttpServerOptions()
            .setIdleTimeout(120)
            .setIdleTimeoutUnit(TimeUnit.SECONDS);

        super.vertx
            .createHttpServer(options)
            .requestHandler(router)
            .listen(port);
    }

    private static void handle(ThrowingRunnable<?> action, RoutingContext ctx, HttpResponseStatus failStatus) {
        handle(action.toSupplier(null), ctx, failStatus);
    }

    private static void handle(ThrowingSupplier<?, ?> action, RoutingContext ctx, HttpResponseStatus failStatus) {
        HttpServerResponse resp = ctx.response().putHeader("Content-Type", "application/json; charset=utf-8");
        try {
            Object result = action.get();
            resp.setStatusCode(OK.code());
            if (result == null) {
                resp.end();
            } else {
                resp.end(toJson(result));
            }
        } catch (Throwable e) {
            resp.setStatusCode(failStatus.code())
                .end(Throwables.getRootCauseMessage(e));
        }
    }

    private static <T> T parseBodyArg(RoutingContext ctx, Class<T> type) {
        Object[] args = Jsons.parseArray(ctx.body().asString(), type);
        return (T) Collects.get(args, 0);
    }

    private static <T> T parseParamArg(RoutingContext ctx, Class<T> type) {
        String arg0ParamName = RpcControllerUtils.getQueryParameterName(0);
        String json = Collects.get(ctx.queryParam(arg0ParamName), 0);
        return Jsons.fromJson(json, type);
    }

    private static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).toString();
        }
        return Jsons.toJson(obj);
    }

}
