package io.kk.cloudslang.vertx;

import io.cloudslang.lang.entities.SystemProperty;
import io.vertx.core.json.JsonObject;

import java.util.Set;

/**
 * @author keke
 */
public interface SystemPropertiesLoader {
  Set<SystemProperty> load(JsonObject config);
}
