package com.myapp.quicknotes.search;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// Arithmetic on embedding vectors, and their storage format in the database.
public final class Vectors {
    private Vectors() {
    }

    // The vector scaled to length 1. Stored vectors are kept at length 1 so that the similarity
    // of two of them (the cosine of the angle between them) is a plain dot product.
    public static float[] normalized(float[] vector) {
        double sumOfSquares = 0;
        for (float value : vector) {
            sumOfSquares += value * value;
        }
        float[] result = new float[vector.length];
        if (sumOfSquares == 0) {
            return result;
        }
        float scale = (float) (1 / Math.sqrt(sumOfSquares));
        for (int i = 0; i < vector.length; i++) {
            result[i] = vector[i] * scale;
        }
        return result;
    }

    public static float dot(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Vectors of different lengths: " + a.length + " and " + b.length);
        }
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    // Four bytes per value, little-endian, so a stored vector reads back the same on any device.
    public static byte[] toBytes(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.asFloatBuffer().put(vector);
        return buffer.array();
    }

    public static float[] fromBytes(byte[] bytes) {
        float[] vector = new float[bytes.length / Float.BYTES];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(vector);
        return vector;
    }
}
