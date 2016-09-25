package io.kk.cloudslang.vertx;

import io.cloudslang.lang.compiler.SlangSource;
import io.vertx.core.json.JsonObject;

import java.util.Set;

/**
 * @author keke
 */
public interface DependenciesLoader {
  Set<SlangSource> load(JsonObject config);
}
