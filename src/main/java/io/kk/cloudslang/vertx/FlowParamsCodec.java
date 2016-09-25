package io.kk.cloudslang.vertx;

import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

/**
 * @author keke
 */
public class FlowParamsCodec implements MessageCodec<FlowParams, FlowParams> {
  @Override
  public void encodeToWire(Buffer buffer, FlowParams flowParams) {
    String strJson = flowParams.toJson().encode();
    byte[] encoded = strJson.getBytes(CharsetUtil.UTF_8);
    buffer.appendInt(encoded.length);
    Buffer buff = Buffer.buffer(encoded);
    buffer.appendBuffer(buff);
  }

  @Override
  public FlowParams decodeFromWire(int pos, Buffer buffer) {
    int length = buffer.getInt(pos);
    pos += 4;
    byte[] encoded = buffer.getBytes(pos, pos + length);
    String str = new String(encoded, CharsetUtil.UTF_8);
    return new FlowParams(new JsonObject(str));
  }

  @Override
  public FlowParams transform(FlowParams flowParams) {
    return new FlowParams(flowParams.toJson().copy());
  }

  @Override
  public String name() {
    return FlowParamsCodec.class.getName();
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
