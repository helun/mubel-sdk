package io.mubel.sdk.codec;

public interface EventDataCodec {

    byte[] encode(Object data);

    <T> T decode(byte[] bytes, Class<T> klass);

}
