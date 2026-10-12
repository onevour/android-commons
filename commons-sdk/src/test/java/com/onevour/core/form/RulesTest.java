package com.onevour.core.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

public class RulesTest {

    @Test
    public void emptyMeansNullBlankTextOrEmptyList() {
        assertTrue(Rules.isEmpty(null));
        assertTrue(Rules.isEmpty("  "));
        assertTrue(Rules.isEmpty(new ArrayList<>()));
        assertFalse(Rules.isEmpty("a"));
        assertFalse(Rules.isEmpty(0));
        assertFalse(Rules.isEmpty(Collections.singletonList(1L)));
    }

    @Test
    public void numbersAreReadFromTextWithCommaOrDot() {
        assertEquals(12.5, Rules.toNumber("12,5"), 0.0);
        assertEquals(12.5, Rules.toNumber(" 12.5 "), 0.0);
        assertEquals(3.0, Rules.toNumber(3), 0.0);
        assertNull(Rules.toNumber("abc"));
        assertNull(Rules.toNumber(null));
    }
}
