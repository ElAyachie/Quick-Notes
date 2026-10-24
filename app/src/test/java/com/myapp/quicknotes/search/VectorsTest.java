package com.myapp.quicknotes.search;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VectorsTest {
    private static final float TOLERANCE = 1e-6f;

    @Test
    public void normalizedVectorHasLengthOne() {
        float[] vector = Vectors.normalized(new float[]{3, 4});

        assertArrayEquals(new float[]{0.6f, 0.8f}, vector, TOLERANCE);
        assertEquals(1f, Vectors.dot(vector, vector), TOLERANCE);
    }

    @Test
    public void zeroVectorStaysZero() {
        assertArrayEquals(new float[]{0, 0}, Vectors.normalized(new float[]{0, 0}), 0);
    }

    @Test
    public void dotOfUnitVectorsIsTheirCosineSimilarity() {
        float[] east = {1, 0};
        float[] north = {0, 1};
        float[] northEast = Vectors.normalized(new float[]{1, 1});

        assertEquals(1f, Vectors.dot(east, east), TOLERANCE);
        assertEquals(0f, Vectors.dot(east, north), TOLERANCE);
        assertEquals((float) Math.cos(Math.PI / 4), Vectors.dot(east, northEast), TOLERANCE);
        assertEquals(-1f, Vectors.dot(east, new float[]{-1, 0}), TOLERANCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dotRejectsVectorsOfDifferentLengths() {
        Vectors.dot(new float[]{1, 0}, new float[]{1, 0, 0});
    }

    @Test
    public void bytesRoundTrip() {
        float[] vector = {0.25f, -1.5f, 3.4028235e38f, Float.MIN_VALUE, 0f};

        assertArrayEquals(vector, Vectors.fromBytes(Vectors.toBytes(vector)), 0);
    }

    @Test
    public void bytesAreLittleEndian() {
        // 1.0f is 0x3F800000.
        assertArrayEquals(new byte[]{0, 0, (byte) 0x80, 0x3F},
                Vectors.toBytes(new float[]{1f}));
    }
}
