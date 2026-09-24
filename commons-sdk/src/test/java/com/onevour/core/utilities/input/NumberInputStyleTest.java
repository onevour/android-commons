package com.onevour.core.utilities.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NumberInputStyleTest {

    @Test
    public void testDefaultBuilder() {
        NumberInputStyle style = new NumberInputStyle.Builder().build();
        assertNotNull(style);
        assertNull(style.getTypeface());
        assertNull(style.getDialogBackgroundColor());
        assertNull(style.getTitleTextColor());
        assertNull(style.getTitleTextSizePx());
        assertNull(style.getResultTextColor());
        assertNull(style.getResultTextSizePx());
        assertNull(style.getKeyTextColor());
        assertNull(style.getKeyTextSizePx());
        assertNull(style.getKeyBackgroundDrawable());
        assertNull(style.getKeyBackgroundColor());
    }

    @Test
    public void testBuilderWithProperties() {
        int bgColor = 0xFF123456;
        int titleColor = 0xFF654321;
        float titleSize = 20.0f;
        int resultColor = 0xFF00FF00;
        float resultSize = 32.0f;
        int keyColor = 0xFFFF0000;
        float keySize = 24.0f;
        int keyBgColor = 0xFFEEEEEE;

        NumberInputStyle style = new NumberInputStyle.Builder()
                .setDialogBackgroundColor(bgColor)
                .setTitleTextColor(titleColor)
                .setTitleTextSizePx(titleSize)
                .setResultTextColor(resultColor)
                .setResultTextSizePx(resultSize)
                .setKeyTextColor(keyColor)
                .setKeyTextSizePx(keySize)
                .setKeyBackgroundColor(keyBgColor)
                .build();

        assertEquals(Integer.valueOf(bgColor), style.getDialogBackgroundColor());
        assertEquals(Integer.valueOf(titleColor), style.getTitleTextColor());
        assertEquals(Float.valueOf(titleSize), style.getTitleTextSizePx());
        assertEquals(Integer.valueOf(resultColor), style.getResultTextColor());
        assertEquals(Float.valueOf(resultSize), style.getResultTextSizePx());
        assertEquals(Integer.valueOf(keyColor), style.getKeyTextColor());
        assertEquals(Float.valueOf(keySize), style.getKeyTextSizePx());
        assertEquals(Integer.valueOf(keyBgColor), style.getKeyBackgroundColor());
    }
}
