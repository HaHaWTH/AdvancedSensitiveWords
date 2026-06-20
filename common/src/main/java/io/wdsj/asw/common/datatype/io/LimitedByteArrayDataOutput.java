package io.wdsj.asw.common.datatype.io;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class LimitedByteArrayDataOutput {
    private static final int DOUBLE_SIZE = Double.BYTES;
    private static final int FLOAT_SIZE = Float.BYTES;
    private static final int INT_SIZE = Integer.BYTES;
    private static final int LONG_SIZE = Long.BYTES;
    private static final int CHAR_SIZE = Character.BYTES;
    private static final int SHORT_SIZE = Short.BYTES;
    private static final int BOOLEAN_SIZE = 1;
    private final ByteArrayDataOutput output;
    private final int maxSize;
    private int currentSize;

    private LimitedByteArrayDataOutput(int maxSize) {
        this.output = ByteStreams.newDataOutput();
        this.maxSize = maxSize;
        this.currentSize = 0;
    }

    public static LimitedByteArrayDataOutput newDataOutput(int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("Max size must be greater than 0");
        }
        return new LimitedByteArrayDataOutput(maxSize);
    }

    public byte[] toByteArray() {
        return output.toByteArray();
    }

    public void write(byte[] bytes) throws IOException {
        ensureWritable(bytes.length);
        output.write(bytes);
        currentSize += bytes.length;
    }

    public void writeInt(int value) throws IOException {
        ensureWritable(INT_SIZE);
        output.writeInt(value);
        currentSize += INT_SIZE;
    }

    public void writeLong(long value) throws IOException {
        ensureWritable(LONG_SIZE);
        output.writeLong(value);
        currentSize += LONG_SIZE;
    }

    public void writeBoolean(boolean value) throws IOException {
        ensureWritable(BOOLEAN_SIZE);
        output.writeBoolean(value);
        currentSize += BOOLEAN_SIZE;
    }

    public void writeUTF(String value) throws IOException {
        byte[] utfBytes = value.getBytes(StandardCharsets.UTF_8);
        ensureWritable(utfBytes.length + 2);
        output.writeUTF(value);
        currentSize += utfBytes.length + 2;
    }

    public void writeDouble(double value) throws IOException {
        ensureWritable(DOUBLE_SIZE);
        output.writeDouble(value);
        currentSize += DOUBLE_SIZE;
    }

    public void writeFloat(float value) throws IOException {
        ensureWritable(FLOAT_SIZE);
        output.writeFloat(value);
        currentSize += FLOAT_SIZE;
    }

    public void writeShort(short value) throws IOException {
        ensureWritable(SHORT_SIZE);
        output.writeShort(value);
        currentSize += SHORT_SIZE;
    }

    public void writeChar(char value) throws IOException {
        ensureWritable(CHAR_SIZE);
        output.writeChar(value);
        currentSize += CHAR_SIZE;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    private void ensureWritable(int bytes) throws IOException {
        if (currentSize + bytes > maxSize) {
            throw new IOException("Data exceeds maximum size of " + maxSize + " bytes");
        }
    }
}
