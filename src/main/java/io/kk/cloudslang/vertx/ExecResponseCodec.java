package io.kk.cloudslang.vertx;

import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

/**
 * @author keke
 */
public class ExecResponseCodec implements MessageCodec<ExecResponse, ExecResponse> {
  @Override
  public void encodeToWire(Buffer buffer, ExecResponse execResponse) {
    String strJson = execResponse.toJson().encode();
    byte[] encoded = strJson.getBytes(CharsetUtil.UTF_8);
    buffer.appendInt(encoded.length);
    Buffer buff = Buffer.buffer(encoded);
    buffer.appendBuffer(buff);
  }

  @Override
  public ExecResponse decodeFromWire(int pos, Buffer buffer) {
    int length = buffer.getInt(pos);
    pos += 4;
    byte[] encoded = buffer.getBytes(pos, pos + length);
    String str = new String(encoded, CharsetUtil.UTF_8);
    return new ExecResponse(new JsonObject(str));
  }

  @Override
  public ExecResponse transform(ExecResponse execResponse) {
    return new ExecResponse(execResponse.toJson().copy());
  }

  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
