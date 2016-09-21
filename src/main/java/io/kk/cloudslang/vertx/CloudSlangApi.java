package io.kk.cloudslang.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author keke
 */
public class CloudSlangApi extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(CloudSlangApi.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    super.start(startFuture);
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    super.stop(stopFuture);
  }
}
