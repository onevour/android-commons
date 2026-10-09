package com.onevour.core.location;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CoordinatesTest {

    @Test
    public void nearZeroZero_orMissing_isNoLocation() {
        assertFalse(Coordinates.isValid(0.0, 0.0));
        assertFalse(Coordinates.isValid(1.0, 1.0));
        assertFalse(Coordinates.isValid(null, 106.8));
    }

    @Test
    public void aRealPlace_isALocation() {
        assertTrue(Coordinates.isValid(-6.2, 106.8));
        assertTrue(Coordinates.isValid(-7.98, 112.63));
    }
}
