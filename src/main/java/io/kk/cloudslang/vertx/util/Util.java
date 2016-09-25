package io.kk.cloudslang.vertx.util;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

/**
 * @author keke
 */
public class Util {
  public static String getConfig(JsonObject config, String env, String prop) {
    String value = System.getenv(env);
    if(StringUtils.isNotBlank(value)){
      return value;
    }
    return config.getValue(prop).toString();
  }
}
