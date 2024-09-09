package io.wdsj.asw.common.datatype.io;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public class LimitedByteArrayDataOutput {
    private static final int doubleSize = Double.BYTES;
    private static final int floatSize = Float.BYTES;
    private static final int intSize = Integer.BYTES;
    private static final int longSize = Long.BYTES;
    private static final int charSize = Character.BYTES;
    private static final int shortSize = Short.BYTES;
    private static final int booleanSize = 1;
    private final ByteArrayDataOutput output;
    private final int maxSize;
    private int currentSize;

    LimitedByteArrayDataOutput(int maxSize) {
        this.output = ByteStreams.newDataOutput();
        this.maxSize = maxSize;
        this.currentSize = 0;
    }

    public static LimitedByteArrayDataOutput newDataOutput(int maxSize) {
        return new LimitedByteArrayDataOutput(maxSize);
    }

    public byte[] toByteArray() {
        return output.toByteArray();
    }

    public void write(byte[] bytes) throws IOException {
        if (currentSize + bytes.length > maxSize) {
            throw new IOException("Data exceeds maximum size of " + maxSize + " bytes");
        }
        output.write(bytes);
        currentSize += bytes.length;
    }

    public void writeInt(int value) throws IOException {
        if (currentSize + intSize > maxSize) {
            throw new IOException("Data exceeds maximum size of " + maxSize + " bytes");
        }
        output.writeInt(value);
        currentSize += intSize;
    }

    public void writeLong(long value) throws IOException {
        if (currentSize + longSize > maxSize) {
            throw new IOException("Data exceeds maximum size of " + maxSize + " bytes");
        }
        output.writeLong(value);
        currentSize += longSize;
    }

    public void writeBoolean(boolean value) throws IOException {
        if (currentSize + booleanSize > maxSize) {
            throw new IOException("Data exceeds maximum size of " + maxSize + " bytes");
        }
        output.writeBoolean(value);
        currentSize += booleanSize;
    }

    public void writeUTF(String value) throws IOException {
        byte[] utfBytes = value.getBytes(StandardCharsets.UTF_8);
        if (currentSize + utfBytes.length + 2 > maxSize) {  // 2 bytes for UTF string length
            throw new IOException("Data exceeds maximum size of " + maxSize + " bytes");
        }
        output.writeUTF(value);
        currentSize += utfBytes.length + 2;
    }

    public void writeDouble(double value) throws IOException {
        if (currentSize + doubleSize > maxSize) {
            throw new IOException("Data exceeds maximum size of " + maxSize + " bytes");
        }
        output.writeDouble(value);
        currentSize += doubleSize;
    }

    public void writeFloat(float value) throws IOException {
        if (currentSize + floatSize > maxSize) {
            throw new IOException("Data exceeds maximum size of " + maxSize + " bytes");
        }
        output.writeFloat(value);
        currentSize += floatSize;
    }

    public void writeShort(short value) throws IOException {
        if (currentSize + shortSize > maxSize) {
            throw new IOException("Data exceeds maximum size of " + maxSize + " bytes");
        }
        output.writeShort(value);
        currentSize += shortSize;
    }

    public void writeChar(char value) throws IOException {
        if (currentSize + charSize > maxSize) {
            throw new IOException("Data exceeds maximum size of " + maxSize + " bytes");
        }
        output.writeChar(value);
        currentSize += charSize;
    }

    public int getCurrentSize() {
        return currentSize;
    }
}