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
import cn.ponfee.disjob.common.spring.LocalizedMethodArgumentUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam;
import cn.ponfee.disjob.core.param.worker.ExistsTaskParam;
import cn.ponfee.disjob.core.param.worker.GetMetricsParam;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.samples.worker.util.JobHandlerParser;
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

import static cn.ponfee.disjob.core.base.WorkerRpcService.PREFIX_PATH;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * Web server based vertx-web.
 *
 * @author Ponfee
 */
public class VertxWebServer extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(VertxWebServer.class);

    private final int port;
    private final TaskReceiver httpTaskReceiver;
    private final WorkerRpcService workerRpcService;

    public VertxWebServer(int port, TaskReceiver httpTaskReceiver, WorkerRpcService workerRpcService) {
        this.port = port;
        this.httpTaskReceiver = httpTaskReceiver;
        this.workerRpcService = workerRpcService;
    }

    public final void deploy() {
        VertxOptions options = new VertxOptions();
        Vertx vertx0 = Vertx.vertx(options);

        // here super.vertx is null

        vertx0.deployVerticle(this);
        assert super.vertx == vertx0;
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
            countDownLatch.await(60, TimeUnit.SECONDS);
            LOG.info("Close vertx success.");
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
        router.post(PREFIX_PATH + "/job/verify").handler(ctx -> handle(() -> {
            JobHandlerParam param = parseBodyArg(ctx, JobHandlerParam.class);
            workerRpcService.verify(param);
        }, ctx, BAD_REQUEST));

        router.post(PREFIX_PATH + "/job/split").handler(ctx -> handle(() -> {
            JobHandlerParam param = parseBodyArg(ctx, JobHandlerParam.class);
            JobHandlerParser.parse(param, "jobHandler");
            return workerRpcService.split(param);
        }, ctx, INTERNAL_SERVER_ERROR));

        router.get(PREFIX_PATH + "/task/exists").handler(ctx -> handle(() -> {
            ExistsTaskParam param = parseParamArg(ctx, ExistsTaskParam.class);
            return workerRpcService.existsTask(param);
        }, ctx, INTERNAL_SERVER_ERROR));

        router.get(PREFIX_PATH + "/metrics").handler(ctx -> handle(() -> {
            GetMetricsParam param = parseParamArg(ctx, GetMetricsParam.class);
            return workerRpcService.metrics(param);
        }, ctx, INTERNAL_SERVER_ERROR));

        router.post(PREFIX_PATH + "/worker/configure").handler(ctx -> handle(() -> {
            ConfigureWorkerParam param = parseBodyArg(ctx, ConfigureWorkerParam.class);
            workerRpcService.configureWorker(param);
        }, ctx, INTERNAL_SERVER_ERROR));

        if (httpTaskReceiver != null) {
            router.post(PREFIX_PATH + "/task/receive").handler(ctx -> handle(() -> {
                ExecuteTaskParam param = parseBodyArg(ctx, ExecuteTaskParam.class);
                JobHandlerParser.parse(param, "jobHandler");
                return httpTaskReceiver.receive(param);
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
        return args == null ? null : (T) args[0];
    }

    private static <T> T parseParamArg(RoutingContext ctx, Class<T> type) {
        String arg0ParamName = LocalizedMethodArgumentUtils.getQueryParamName(0);
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
