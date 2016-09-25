package io.kk.cloudslang.vertx.util;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author keke
 */
public class RunMain {

  public static final String BOOT_DEPLOYED = "vertx.deployed";
  public static final String CLASSPATH = "classpath:";
  private static final Logger LOG = LoggerFactory.getLogger(RunMain.class);
  private static final String UTF_8 = "UTF-8";

  public static void main(String... args) throws IOException {

    JsonObject jsonConf = getConfig(args);
    DeploymentOptions deploymentOptions = new DeploymentOptions(jsonConf);
    VertxOptions options = new VertxOptions(jsonConf);

    Vertx vertx = Vertx.vertx(options);
    vertx.deployVerticle(Boot.class.getName(), deploymentOptions, r -> {
      if (r.succeeded()) {
        LOG.info("Successfully deployed Boot Verticle");
        vertx.eventBus().publish(BOOT_DEPLOYED, "");
      } else {
        LOG.error("Unable to deploy Boot", r.cause());
        vertx.close();
      }
    });
  }

  private static JsonObject getConfig(String... args) throws IOException {
    JsonObject json;
    if (args.length == 1) {
      LOG.debug("To read vertx configuration from {}", args[0]);
      if (args[0].startsWith(CLASSPATH)) {
        json = new JsonObject(IOUtils.toString(RunMain.class.getResourceAsStream(args[0].substring(CLASSPATH.length())), UTF_8));
      } else {
        json = new JsonObject(FileUtils.readFileToString(new File(args[0]), UTF_8));
      }
    } else {
      json = new JsonObject(IOUtils.toString(RunMain.class.getResourceAsStream("/vertx.json"), UTF_8));
    }
    return json;
  }


  public static class Boot extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(Boot.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
      List<Future> fs = new ArrayList<>();
      config().getJsonObject("boot").forEach(e -> {
        Future<String> future = Future.future();
        LOG.debug("To deploy verticle {}", e.getValue());
        vertx.deployVerticle(e.getValue().toString(),
            new DeploymentOptions(config().getJsonObject(e.getKey())), future.completer());
        fs.add(future);
      });
      CompositeFuture.all(fs).setHandler(r -> {
        if (r.succeeded()) {
          startFuture.complete();
          LOG.info("All verticles were deployed");
        } else {
          startFuture.fail(r.cause());
        }
      });
    }
  }
}
