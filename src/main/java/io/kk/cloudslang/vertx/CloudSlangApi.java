package io.kk.cloudslang.vertx;

import io.kk.cloudslang.vertx.util.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author keke
 */
public class CloudSlangApi extends AbstractVerticle {
  public static final String CONTENT_TYPE = "application/json";
  public static final String CONTENT_TYPE_UTF_8 = CONTENT_TYPE + ";charset=UTF-8";
  public static final String NAME = "name";
  private static final Logger LOG = LoggerFactory.getLogger(CloudSlangApi.class);
  private HttpServer server;
  private Router restApi;
  private MessageProducer<JsonObject> publisher;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    initWebServer();
    super.start(startFuture);
    LOG.info("CloudSlang API Deployed");
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    server.close();
    super.stop(stopFuture);

  }

  private void initWebServer() {
    HttpServerOptions options = new HttpServerOptions(config().getJsonObject("server"));
    server = vertx.createHttpServer(options);
    Router mainRouter = Router.router(vertx);
    restApi = Router.router(vertx);
    mainRouter.mountSubRouter(getBaseUrl(), restApi);
    restApi.route().handler(BodyHandler.create());
    initRestApi(restApi);
    server.requestHandler(mainRouter::accept).listen();
    publisher = vertx.eventBus().publisher(CloudSlangService.EXEC_FLOW);
    LOG.info("CloudSlang Vertx API listening at {}", options.getPort());
  }

  private void initRestApi(Router restApi) {
    restApi.post("/:" + NAME).consumes(CONTENT_TYPE).produces(CONTENT_TYPE_UTF_8).useNormalisedPath(true).handler(this::runFlow);
  }

  private void runFlow(RoutingContext routingContext) {
    JsonObject data;
    try {
      data = routingContext.getBodyAsJson();
    } catch (DecodeException e) {
      LOG.error("Unable to read post body", e);
      routingContext.response().setStatusCode(400).end();
      return;
    }
    data.put(NAME, routingContext.request().getParam(NAME));
    String id = UUID.randomUUID().toString();
    data.put("execId", id);
    waitForResponse(id, routingContext);
    publisher.send(data);
  }

  private void waitForResponse(String execId, RoutingContext routingContext) {
    final String address = CloudSlangService.EXEC_FLOW_RESPONSE + "_" + execId;
    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(address);
    consumer.handler(h -> {
      try {
        JsonObject response = h.body();
        LOG.debug("Got exec response {}", response.encodePrettily());
        routingContext.response().setStatusCode(response.getInteger("status-code"))
            .putHeader("Content-Type", CONTENT_TYPE_UTF_8).end(response.toString());
      } finally {
        consumer.unregister();
      }
    });
    consumer.exceptionHandler(ex -> {
      LOG.error("Unable to consume message {}", address);
      LOG.error("Error received", ex);
      routingContext.response().setStatusCode(500).end(ex.getMessage());
      consumer.unregister();
    });
  }

  private String getBaseUrl() {
    return Util.getConfig(config(), "BASE_URL", "baseUrl");
  }
}
