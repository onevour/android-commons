package com.onevour.core.components.numpad;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.Px;

public class NumPadStyle {

    private Typeface typeface;
    private Integer dialogBackgroundColor;
    private Integer titleTextColor;
    private Float titleTextSizePx;
    private Integer resultTextColor;
    private Float resultTextSizePx;
    private Integer keyTextColor;
    private Float keyTextSizePx;
    private Drawable keyBackgroundDrawable;
    private Integer keyBackgroundColor;
    private Integer accentColor;
    private Integer resultBackgroundColor;
    private Integer afterPointColor;
    private Integer dividerColor;
    private Integer handleColor;
    private Integer rippleColor;
    /** Cancel label in capitals; null keeps the layout's (capitals). */
    private Boolean cancelAllCaps;

    public NumPadStyle() {
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public Integer getDialogBackgroundColor() {
        return dialogBackgroundColor;
    }

    public Integer getTitleTextColor() {
        return titleTextColor;
    }

    public Float getTitleTextSizePx() {
        return titleTextSizePx;
    }

    public Integer getResultTextColor() {
        return resultTextColor;
    }

    public Float getResultTextSizePx() {
        return resultTextSizePx;
    }

    public Integer getKeyTextColor() {
        return keyTextColor;
    }

    public Float getKeyTextSizePx() {
        return keyTextSizePx;
    }

    public Drawable getKeyBackgroundDrawable() {
        return keyBackgroundDrawable;
    }

    public Integer getKeyBackgroundColor() {
        return keyBackgroundColor;
    }

    /**
     * Brand accent color, independent of {@link #getKeyBackgroundColor()}: applied only to the
     * submit key's pill shape and the Cancel label, leaving the digit keys untouched.
     */
    public Integer getAccentColor() {
        return accentColor;
    }

    /**
     * Background tint for the typed-value "screen" ({@code key_result}), independent of
     * {@link #getResultTextColor()} which only affects its text.
     */
    public Integer getResultBackgroundColor() {
        return resultBackgroundColor;
    }

    /**
     * Tint for the decimal-point key's ring and text while it's showing the "after point"
     * state, in place of the library's default red. The ring's inner "hole" reuses
     * {@link #getResultBackgroundColor()} when set, else the library's default screen color.
     */
    public Integer getAfterPointColor() {
        return afterPointColor;
    }

    /**
     * Tint for the horizontal divider above the Cancel key.
     */
    public Integer getDividerColor() {
        return dividerColor;
    }

    /**
     * Tint for the drag-handle bar at the top of the dialog.
     */
    public Integer getHandleColor() {
        return handleColor;
    }

    /**
     * Touch-ripple highlight color for every key (digits, submit, Cancel, backspace),
     * in place of the theme's {@code ?attr/colorControlHighlight} / the library's default gray.
     */
    public Integer getRippleColor() {
        return rippleColor;
    }

    public Boolean getCancelAllCaps() {
        return cancelAllCaps;
    }

    public static class Builder {

        private final NumPadStyle style = new NumPadStyle();

        public Builder setTypeface(Typeface typeface) {
            style.typeface = typeface;
            return this;
        }

        public Builder setDialogBackgroundColor(@ColorInt int color) {
            style.dialogBackgroundColor = color;
            return this;
        }

        public Builder setTitleTextColor(@ColorInt int color) {
            style.titleTextColor = color;
            return this;
        }

        public Builder setTitleTextSizePx(@Px float sizePx) {
            style.titleTextSizePx = sizePx;
            return this;
        }

        public Builder setResultTextColor(@ColorInt int color) {
            style.resultTextColor = color;
            return this;
        }

        public Builder setResultTextSizePx(@Px float sizePx) {
            style.resultTextSizePx = sizePx;
            return this;
        }

        public Builder setKeyTextColor(@ColorInt int color) {
            style.keyTextColor = color;
            return this;
        }

        public Builder setKeyTextSizePx(@Px float sizePx) {
            style.keyTextSizePx = sizePx;
            return this;
        }

        public Builder setKeyBackgroundDrawable(Drawable drawable) {
            style.keyBackgroundDrawable = drawable;
            return this;
        }

        public Builder setKeyBackgroundColor(@ColorInt int color) {
            style.keyBackgroundColor = color;
            return this;
        }

        public Builder setAccentColor(@ColorInt int color) {
            style.accentColor = color;
            return this;
        }

        public Builder setResultBackgroundColor(@ColorInt int color) {
            style.resultBackgroundColor = color;
            return this;
        }

        public Builder setAfterPointColor(@ColorInt int color) {
            style.afterPointColor = color;
            return this;
        }

        public Builder setDividerColor(@ColorInt int color) {
            style.dividerColor = color;
            return this;
        }

        public Builder setHandleColor(@ColorInt int color) {
            style.handleColor = color;
            return this;
        }

        public Builder setRippleColor(@ColorInt int color) {
            style.rippleColor = color;
            return this;
        }

        /** false for an app whose buttons are not in capitals (the default is capitals). */
        public Builder setCancelAllCaps(boolean allCaps) {
            style.cancelAllCaps = allCaps;
            return this;
        }

        public NumPadStyle build() {
            return style;
        }
    }
}
