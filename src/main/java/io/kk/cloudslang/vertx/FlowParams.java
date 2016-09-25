package io.kk.cloudslang.vertx;

import io.vertx.core.json.JsonObject;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author keke
 */
public class FlowParams implements Serializable {
  private JsonObject json;

  public FlowParams(JsonObject jsonObject) {
    this.json = Objects.requireNonNull(jsonObject);
  }

  public FlowParams(String value) {
    this(new JsonObject(Objects.requireNonNull(value)));
  }

  public FlowParams setFlowName(String name) {
    json.put("name", name);
    return this;
  }

  public String getFlowName() {
    return json.getString("name");
  }

  public JsonObject getArgs() {
    return json.getJsonObject("args");
  }

  public String getExecId() {
    return json.getString("id");
  }

  public FlowParams setExecId(String id) {
    json.put("id", id);
    return this;
  }

  public JsonObject toJson() {
    return json;
  }
}
