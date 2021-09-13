package io.jaegertracing.dependencies;

import java.nio.ByteBuffer;
import java.util.Base64;

public class Base64Util {

    public static String toBase64(long value, boolean is128bits) {
        ByteBuffer buffer = ByteBuffer.allocate(is128bits ? Long.BYTES * 2 : Long.BYTES);
        if(is128bits)
            buffer.putLong(0);
        buffer.putLong(value);
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public static String toBase64(long value) {
        return toBase64(value, false);
    }

}
