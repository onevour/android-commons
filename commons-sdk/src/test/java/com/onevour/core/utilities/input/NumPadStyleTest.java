package com.onevour.core.utilities.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.onevour.core.components.numpad.NumPadStyle;

import org.junit.Test;

public class NumPadStyleTest {

    @Test
    public void testDefaultBuilder() {
        NumPadStyle style = new NumPadStyle.Builder().build();
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
        assertNull(style.getAccentColor());
        assertNull(style.getResultBackgroundColor());
        assertNull(style.getAfterPointColor());
        assertNull(style.getDividerColor());
        assertNull(style.getHandleColor());
        assertNull(style.getRippleColor());
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
        int accentColor = 0xFF00AAFF;
        int resultBgColor = 0xFF111111;
        int afterPointColor = 0xFFCC0000;
        int dividerColor = 0xFF999999;
        int handleColor = 0xFF888888;
        int rippleColor = 0x33000000;

        NumPadStyle style = new NumPadStyle.Builder()
                .setDialogBackgroundColor(bgColor)
                .setTitleTextColor(titleColor)
                .setTitleTextSizePx(titleSize)
                .setResultTextColor(resultColor)
                .setResultTextSizePx(resultSize)
                .setKeyTextColor(keyColor)
                .setKeyTextSizePx(keySize)
                .setKeyBackgroundColor(keyBgColor)
                .setAccentColor(accentColor)
                .setResultBackgroundColor(resultBgColor)
                .setAfterPointColor(afterPointColor)
                .setDividerColor(dividerColor)
                .setHandleColor(handleColor)
                .setRippleColor(rippleColor)
                .build();

        assertEquals(Integer.valueOf(bgColor), style.getDialogBackgroundColor());
        assertEquals(Integer.valueOf(titleColor), style.getTitleTextColor());
        assertEquals(Float.valueOf(titleSize), style.getTitleTextSizePx());
        assertEquals(Integer.valueOf(resultColor), style.getResultTextColor());
        assertEquals(Float.valueOf(resultSize), style.getResultTextSizePx());
        assertEquals(Integer.valueOf(keyColor), style.getKeyTextColor());
        assertEquals(Float.valueOf(keySize), style.getKeyTextSizePx());
        assertEquals(Integer.valueOf(keyBgColor), style.getKeyBackgroundColor());
        assertEquals(Integer.valueOf(accentColor), style.getAccentColor());
        assertEquals(Integer.valueOf(resultBgColor), style.getResultBackgroundColor());
        assertEquals(Integer.valueOf(afterPointColor), style.getAfterPointColor());
        assertEquals(Integer.valueOf(dividerColor), style.getDividerColor());
        assertEquals(Integer.valueOf(handleColor), style.getHandleColor());
        assertEquals(Integer.valueOf(rippleColor), style.getRippleColor());
    }

    @Test
    public void testAccentColorIndependentOfKeyBackgroundColor() {
        // accentColor must be a separate knob from keyBackgroundColor: setting one must not
        // populate or affect the other, since they style different, non-overlapping elements
        // (submit pill + Cancel text vs. every digit key).
        NumPadStyle accentOnly = new NumPadStyle.Builder().setAccentColor(0xFF00AAFF).build();
        assertNull(accentOnly.getKeyBackgroundColor());

        NumPadStyle keyBgOnly = new NumPadStyle.Builder().setKeyBackgroundColor(0xFFEEEEEE).build();
        assertNull(keyBgOnly.getAccentColor());
    }
}
