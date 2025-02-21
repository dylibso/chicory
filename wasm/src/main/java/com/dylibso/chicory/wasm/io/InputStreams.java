package com.dylibso.chicory.wasm.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class InputStreams {
    private InputStreams() {}

    public static byte[] readAllBytes(InputStream is) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        int bufLen = 1024;
        byte[] buf = new byte[bufLen];

        // Create an output stream to store all bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int bytesRead;
        while ((bytesRead = is.read(buf, 0, bufLen)) != -1) {
            outputStream.write(buf, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }
}
