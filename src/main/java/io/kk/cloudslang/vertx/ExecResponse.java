package io.kk.cloudslang.vertx;

import io.vertx.core.json.JsonObject;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author keke
 */
public class ExecResponse implements Serializable {
  private JsonObject json;

  public ExecResponse(JsonObject jsonObject) {
    this.json = Objects.requireNonNull(jsonObject);
  }

  public ExecResponse() {
    this(new JsonObject());
  }

  public JsonObject toJson() {
    return json;
  }

  public String getStatus() {
    return json.getString("status");
  }

  public ExecResponse setStatus(String result) {
    json.put("status", result.toLowerCase());
    return this;
  }

  public ExecResponse setResult(JsonObject jsonObject) {
    json.put("result", jsonObject);
    return this;
  }

  public ExecResponse setStatusCode(int code) {
    json.put("status-code", code);
    return this;
  }

  public ExecResponse setErrorReason(String reason) {
    json.put("error-reason", reason);
    return this;
  }

  public int getStatusCode() {
    return json.getInteger("status-code");
  }
}
