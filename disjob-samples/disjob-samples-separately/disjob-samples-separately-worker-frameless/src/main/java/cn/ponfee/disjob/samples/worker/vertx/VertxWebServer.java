/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.worker.vertx;

import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.util.Fields;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.WorkerService;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import cn.ponfee.disjob.core.param.JobHandlerParam;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.samples.worker.util.JobHandlerHolder;
import cn.ponfee.disjob.worker.base.WorkerServiceProvider;
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

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * Web server based vertx-web.
 *
 * @author Ponfee
 */
public class VertxWebServer extends AbstractVerticle {

    static {
        // 启动时预先加载
        JobHandlerHolder.getJobHandler(null);
    }

    private static final Logger LOG = LoggerFactory.getLogger(VertxWebServer.class);

    private static final String PATH_PREFIX = "/" + WorkerService.PREFIX_PATH;
    private static final WorkerServiceProvider WORKER_SERVICE_PROVIDER = new WorkerServiceProvider();

    private final int port;
    private final TaskReceiver httpTaskReceiver;

    public VertxWebServer(int port, TaskReceiver httpTaskReceiver) {
        this.port = port;
        this.httpTaskReceiver = httpTaskReceiver;
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
        router.post(PATH_PREFIX + "job/verify").handler(ctx -> invoke(() -> {
            JobHandlerParam param = parseArg(ctx, JobHandlerParam.class);
            WORKER_SERVICE_PROVIDER.verify(param);
            return null;
        }, ctx, BAD_REQUEST));

        router.post(PATH_PREFIX + "job/split").handler(ctx -> invoke(() -> {
            JobHandlerParam param = parseArg(ctx, JobHandlerParam.class);
            modifyJobHandler(param, "jobHandler");
            return WORKER_SERVICE_PROVIDER.split(param);
        }, ctx, INTERNAL_SERVER_ERROR));

        if (httpTaskReceiver != null) {
            router.post(PATH_PREFIX + "task/receive").handler(ctx -> invoke(() -> {
                ExecuteTaskParam param = parseArg(ctx, ExecuteTaskParam.class);
                modifyJobHandler(param, "jobHandler");
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

    private static void invoke(ThrowingSupplier<?, ?> action, RoutingContext ctx, HttpResponseStatus failStatus) {
        HttpServerResponse resp = ctx.response().putHeader("Content-Type", "application/json; charset=utf-8");
        try {
            Object result = action.get();
            resp.setStatusCode(OK.code())
                .end(result == null ? null : Jsons.toJson(result));
        } catch (Throwable e) {
            resp.setStatusCode(failStatus.code())
                .end(Throwables.getRootCauseMessage(e));
        }
    }

    private static <T> T parseArg(RoutingContext ctx, Class<T> type) {
        Object[] args = Jsons.parseArray(ctx.body().asString(), type);
        return args == null ? null : (T) args[0];
    }

    /**
     * 仅限测试环境使用
     *
     * @param param     object
     * @param fieldName field name
     */
    public static void modifyJobHandler(Object param, String fieldName) {
        String jobHandler = (String) Fields.get(param, fieldName);
        try {
            JobHandlerUtils.load(jobHandler);
        } catch (Exception ex) {
            Optional.ofNullable(JobHandlerHolder.getJobHandler(jobHandler))
                .map(Class::getName)
                .ifPresent(e -> Fields.put(param, fieldName, e));
        }
    }

}
