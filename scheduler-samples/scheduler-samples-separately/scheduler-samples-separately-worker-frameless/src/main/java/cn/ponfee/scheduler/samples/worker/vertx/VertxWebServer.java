/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.worker.vertx;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.WorkerService;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.handle.SplitTask;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.worker.rpc.WorkerServiceProvider;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * Web server based vertx-web.
 *
 * @author Ponfee
 */
public class VertxWebServer extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(VertxWebServer.class);

    private static final String RPC_PATH_PREFIX = "/" + WorkerService.PREFIX_PATH;
    private static final WorkerServiceProvider WORKER_SERVICE_PROVIDER = new WorkerServiceProvider();

    private final int port;
    private final TaskReceiver httpTaskReceiver;

    public VertxWebServer(int port, TaskReceiver httpTaskReceiver) {
        this.port = port;
        this.httpTaskReceiver = httpTaskReceiver;
    }

    public final void deploy() {
        Vertx vertx0 = Vertx.vertx();

        // here super.vertx is null

        vertx0.deployVerticle(this);
        assert super.vertx == vertx0;
    }

    public final void close() {
        if (super.vertx == null) {
            LOG.error("Cannot close un-deployed vertx.");
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        super.vertx.close(ctx -> countDownLatch.countDown());
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

        router.post(RPC_PATH_PREFIX + "job/verify").handler(ctx -> {
            String[] params = ctx.body().asPojo(String[].class);
            boolean result = WORKER_SERVICE_PROVIDER.verify(params[0], params[1]);
            response(ctx, OK.code(), result);
        });

        router.post(RPC_PATH_PREFIX + "job/split").handler(ctx -> {
            String[] params = ctx.body().asPojo(String[].class);
            try {
                List<SplitTask> result = WORKER_SERVICE_PROVIDER.split(params[0], params[1]);
                response(ctx, OK.code(), result);
            } catch (JobException e) {
                LOG.error("Split job failed: " + Arrays.toString(params), e);
                response(ctx, INTERNAL_SERVER_ERROR.code(), Throwables.getRootCauseMessage(e));
            }
        });

        if (httpTaskReceiver != null) {
            router.post(RPC_PATH_PREFIX + "task/receive").handler(ctx -> {
                String body = ctx.body().asString();
                // remove start char “[” and end char “]”
                String data = body.substring(1, body.length() - 1);
                ExecuteParam param = Jsons.fromJson(data, ExecuteParam.class);
                boolean result = httpTaskReceiver.receive(param);
                response(ctx, OK.code(), result);
            });
        }

        HttpServerOptions options = new HttpServerOptions();
        HttpServer server = super.vertx.createHttpServer(options);
        server.requestHandler(router);
        server.listen(port);
    }

    private static void response(RoutingContext ctx, int statusCode, Object result) {
        ctx.response()
           .putHeader("Content-Type", "application/json; charset=utf-8")
           .setStatusCode(statusCode)
           .end(Jsons.toJson(result));
    }

}
